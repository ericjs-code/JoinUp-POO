package br.com.joinup.ra3.servico;

import br.com.joinup.ra3.modelo.*;
import br.com.joinup.ra3.persistencia.UnidadeTrabalhoRa3;
import jakarta.persistence.*;
import java.util.List;
import static br.com.joinup.ra3.servico.DadosRa3.*;

public final class ServicoCrudRa3 implements OperacoesRa3 {
    // Obrigatoriedade didática do SQL; NÃO é senha/hash de autenticação e NÃO serve para produção.
    public static final String MARCADOR_SEM_LOGIN = "SEM_LOGIN_RA3_DEMO";
    private final UnidadeTrabalhoRa3 trabalho;
    public ServicoCrudRa3(EntityManagerFactory fabrica) { trabalho = new UnidadeTrabalhoRa3(fabrica); }

    @Override public Registro cadastrar(Cadastro dados) {
        if (dados == null) throw new IllegalArgumentException("Dados obrigatórios.");
        Object criado = trabalho.escrever(em -> {
            Object entidade = switch (dados) {
                case NovoGenero d -> new GeneroMusical(d.nome());
                case NovoOrganizador d -> new Organizador(d.nome(), d.email(), MARCADOR_SEM_LOGIN, d.cnpj());
                case NovoParticipante d -> new Participante(d.nome(), d.cpf(), d.nascimento(), d.email(), MARCADOR_SEM_LOGIN);
                case NovoEvento d -> new Evento(d.nome(), d.data(), d.descricao(), d.localizacao(), d.cidade(), d.uf(), d.cep(),
                        exigir(em, Organizador.class, d.organizadorId()), exigir(em, GeneroMusical.class, d.generoId()));
                case NovoTipo d -> new TipoIngresso(d.nome(), d.valor(), d.quantidade(), exigir(em, Evento.class, d.eventoId()));
                case NovaComunidade d -> new Comunidade(d.nome(), exigir(em, Evento.class, d.eventoId()), d.data());
                case NovaPostagem d -> new Postagem(d.descricao(), d.imagem(), exigir(em, Participante.class, d.participanteId()),
                        exigir(em, Comunidade.class, d.comunidadeId()), d.data());
            };
            em.persist(entidade);
            return entidade;
        });
        return registro(criado);
    }

    @Override public Registro consultar(TipoRegistro tipo, int id) {
        validarTipo(tipo); validarId(id);
        return trabalho.ler(em -> registro(exigir(em, tipo.entidade(), id)));
    }

    @Override public List<Registro> listar(TipoRegistro tipo, int pagina) {
        validarTipo(tipo);
        if (pagina < 1 || pagina > Integer.MAX_VALUE / LIMITE) throw new IllegalArgumentException("Página inválida.");
        return trabalho.ler(em -> em.createQuery("SELECT e FROM " + tipo.nomeJpa() + " e WHERE e.id > :minimo ORDER BY e.id", tipo.entidade())
                .setParameter("minimo", 0).setFirstResult((pagina - 1) * LIMITE).setMaxResults(LIMITE)
                .getResultList().stream().map(ServicoCrudRa3::registro).toList());
    }

    @Override public Registro atualizar(Alteracao dados) {
        if (dados == null) throw new IllegalArgumentException("Dados obrigatórios.");
        Object alterado = trabalho.escrever(em -> {
            return switch (dados) {
                case AlterarGenero d -> { var e = exigir(em, GeneroMusical.class, d.id()); e.renomear(d.nome()); yield e; }
                case AlterarOrganizador d -> { var e = exigir(em, Organizador.class, d.id()); e.renomear(d.nome()); yield e; }
                case AlterarParticipante d -> { var e = exigir(em, Participante.class, d.id()); e.renomear(d.nome()); yield e; }
                case AlterarEvento d -> { var e = exigir(em, Evento.class, d.id()); e.renomear(d.nome()); e.definirImagem(d.imagem()); yield e; }
                case AlterarTipo d -> { var e = exigir(em, TipoIngresso.class, d.id()); e.renomear(d.nome()); e.definirValor(d.valor()); e.definirQuantidades(d.disponivel(), d.vendida()); yield e; }
                case AlterarComunidade d -> { var e = exigir(em, Comunidade.class, d.id()); e.renomear(d.nome()); e.definirDescricao(d.descricao()); e.definirImagem(d.imagem()); yield e; }
                case AlterarPostagem d -> { var e = exigir(em, Postagem.class, d.id()); e.alterarConteudo(d.descricao(), d.imagem()); yield e; }
            };
        }); // Dirty checking + commit da unidade de trabalho, sem merge de objetos fornecidos pelo usuário.
        return registro(alterado);
    }

    @Override public boolean excluir(TipoRegistro tipo, int id, String confirmacao) {
        validarTipo(tipo); validarId(id);
        if (!"SIM".equals(confirmacao)) return false;
        return trabalho.escrever(em -> {
            // InnoDB: o lock exclusivo do pai serializa INSERTs filhos que verificam a FK.
            Object entidade = em.find(tipo.entidade(), id, LockModeType.PESSIMISTIC_WRITE);
            if (entidade == null) throw inexistente();
            boolean vinculado = switch (tipo) {
                case GENERO -> existe(em, "Evento", "generoMusical", id);
                case ORGANIZADOR -> existe(em, "Evento", "organizador", id);
                case PARTICIPANTE -> existe(em, "Postagem", "participante", id);
                case EVENTO -> existe(em, "TipoIngresso", "evento", id) || existe(em, "Comunidade", "evento", id);
                case COMUNIDADE -> existe(em, "Postagem", "comunidade", id);
                case TIPO_INGRESSO, POSTAGEM -> false;
            };
            if (vinculado) throw new FalhaRa3(FalhaRa3.Motivo.VINCULO, "Exclusão recusada: remova primeiro os registros dependentes. Nenhum registro foi excluído.");
            em.remove(entidade);
            return true;
        });
    }

    private static boolean existe(EntityManager em, String entidade, String associacao, int id) {
        // Ambos os identificadores são constantes internas acima. Valores sempre parametrizados.
        return !em.createQuery("SELECT e.id FROM " + entidade + " e WHERE e." + associacao + ".id=:id", Integer.class)
                .setParameter("id", id).setMaxResults(1).getResultList().isEmpty();
    }
    private static <T> T exigir(EntityManager em, Class<T> classe, int id) {
        T entidade = em.find(classe, id);
        if (entidade == null) throw inexistente();
        return entidade;
    }
    private static FalhaRa3 inexistente() { return new FalhaRa3(FalhaRa3.Motivo.NAO_ENCONTRADO, "ID não encontrado. Consulte os registros existentes."); }
    private static void validarTipo(TipoRegistro tipo) { if (tipo == null) throw new IllegalArgumentException("Escolha uma entidade válida."); }

    private static String opcional(String texto) { return texto == null ? "(vazio)" : texto; }
    private static String pai(int id, String nome) { return "#" + id + " " + nome; }
    private static Registro registro(Object entidade) {
        return switch (entidade) {
            case GeneroMusical e -> new Registro(e.getId(), e.getNome());
            case Organizador e -> new Registro(e.getId(), e.getNome() + " | email=" + e.getEmail() + " | CNPJ fictício=" + e.getCnpj() + " | ativo=" + e.isAtivo());
            case Participante e -> new Registro(e.getId(), e.getNome() + " | email=" + e.getEmail() + " | nascimento=" + e.getDataNascimento() + " | ativo=" + e.isAtivo());
            case Evento e -> new Registro(e.getId(), e.getNome() + " | data=" + e.getData() + " UTC | " + e.getDescricao() + " | local=" + e.getLocalizacao()
                    + " | " + e.getCidade() + "/" + e.getEstado() + " | CEP=" + e.getCep() + " | imagem=" + opcional(e.getImagem())
                    + " | organizador=" + pai(e.getOrganizador().getId(), e.getOrganizador().getNome()) + " | gênero=" + pai(e.getGeneroMusical().getId(), e.getGeneroMusical().getNome()));
            case TipoIngresso e -> new Registro(e.getId(), e.getNome() + " | valor=" + e.getValor().toPlainString() + " | disponível=" + e.getQuantidadeDisponivel()
                    + " | vendida=" + e.getQuantidadeVendida() + " | evento=" + pai(e.getEvento().getId(), e.getEvento().getNome()));
            case Comunidade e -> new Registro(e.getId(), e.getNome() + " | descrição=" + opcional(e.getDescricao()) + " | imagem=" + opcional(e.getImagem())
                    + " | criação=" + e.getDataCriacao() + " UTC | evento=" + pai(e.getEvento().getId(), e.getEvento().getNome()));
            case Postagem e -> new Registro(e.getId(), opcional(e.getDescricao()) + " | imagem=" + opcional(e.getImagem()) + " | data=" + e.getDataPostagem() + " UTC"
                    + " | autor=" + pai(e.getParticipante().getId(), e.getParticipante().getNome()) + " | comunidade=" + pai(e.getComunidade().getId(), e.getComunidade().getNome()));
            default -> throw new IllegalArgumentException("Entidade fora do modelo RA3.");
        };
    }
}
