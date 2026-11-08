package br.com.joinup.ra3;

import br.com.joinup.ra3.modelo.*;
import br.com.joinup.ra3.persistencia.*;
import br.com.joinup.ra3.servico.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static br.com.joinup.ra3.servico.DadosRa3.*;
import static org.junit.jupiter.api.Assertions.*;

class CrudRa3MySqlIT {
    private static EntityManagerFactory fabrica;
    private static ServicoCrudRa3 servico;
    private static final LocalDateTime UTC = LocalDateTime.of(2030, 11, 12, 18, 30);
    private final String marca = "r2-" + UUID.randomUUID().toString().replace("-", "");
    private record Chave(TipoRegistro tipo, int id) { }
    private final List<Chave> proprios = new ArrayList<>();
    private Integer genero, organizador, participante, evento, comunidade;

    @BeforeAll static void abrir() throws Exception {
        try (var c = ConexoesRa3IT.abrir()) { SchemaRa3MySqlIT.verificar(c); }
        fabrica = PersistenciaRa3.abrir(ConexoesRa3IT.configuracao()); servico = new ServicoCrudRa3(fabrica);
    }
    @AfterAll static void fechar() { if (fabrica != null) fabrica.close(); }

    @ParameterizedTest @EnumSource(TipoRegistro.class)
    void quatroAcoesCrudPorEntidadeEmContextosNovos(TipoRegistro tipo) throws Exception {
        int id = criar(tipo, cadastro(tipo));
        assertEquals(1, contarJdbc(tipo, id)); prova(tipo, "CRIAR", id);
        var lido = servico.consultar(tipo, id); // EM próprio do serviço.
        assertEquals(id, lido.id()); assertTrue(lido.resumo().contains(marca));
        assertFalse(lido.resumo().contains(ServicoCrudRa3.MARCADOR_SEM_LOGIN)); prova(tipo, "CONSULTAR", id);
        servico.atualizar(alteracao(tipo, id));
        try (var em = fabrica.createEntityManager()) { verificarAtualizacao(em.find(tipo.entidade(), id)); }
        assertTrue(servico.consultar(tipo, id).resumo().contains("atual")); prova(tipo, "ATUALIZAR", id);
        assertFalse(servico.excluir(tipo, id, "NAO")); assertEquals(1, contarJdbc(tipo, id));
        assertTrue(servico.excluir(tipo, id, "SIM"));
        try (var em = fabrica.createEntityManager()) { assertNull(em.find(tipo.entidade(), id)); }
        assertEquals(0, contarJdbc(tipo, id)); prova(tipo, "EXCLUIR", id);
        assertEquals(FalhaRa3.Motivo.NAO_ENCONTRADO, assertThrows(FalhaRa3.class, () -> servico.consultar(tipo, id)).motivo());
    }

    @ParameterizedTest @EnumSource(value = TipoRegistro.class, names = {"GENERO", "ORGANIZADOR", "PARTICIPANTE", "EVENTO", "COMUNIDADE"})
    void paisComDependentesNaoSaoRemovidosNemPorCascadeSql(TipoRegistro tipo) throws Exception {
        int id = switch (tipo) {
            case GENERO -> { evento(); yield genero; }
            case ORGANIZADOR -> { evento(); yield organizador; }
            case PARTICIPANTE -> { postagem(); yield participante; }
            case EVENTO -> { comunidade(); yield evento; } // Só comunidade: FK CASCADE perigosa.
            case COMUNIDADE -> { postagem(); yield comunidade; } // Postagem: FK CASCADE perigosa.
            default -> throw new AssertionError();
        };
        assertEquals(FalhaRa3.Motivo.VINCULO, assertThrows(FalhaRa3.class, () -> servico.excluir(tipo, id, "SIM")).motivo());
        for (var chave : proprios) assertEquals(1, contarJdbc(chave.tipo(), chave.id()));
        System.out.println("RA3_EXCLUSAO_PROTEGIDA entidade=" + tipo + " pai_e_filhos=preservados");
    }

    @Test void eventoComTipoTambemTemExclusaoRecusada() throws Exception {
        int id = evento(); criar(TipoRegistro.TIPO_INGRESSO, new NovoTipo(marca, BigDecimal.ONE, 1, id));
        assertEquals(FalhaRa3.Motivo.VINCULO, assertThrows(FalhaRa3.class, () -> servico.excluir(TipoRegistro.EVENTO, id, "SIM")).motivo());
        for (var chave : proprios) assertEquals(1, contarJdbc(chave.tipo(), chave.id()));
    }

    @ParameterizedTest @EnumSource(TipoRegistro.class)
    void idsInexistentesNaoCriamOuAlteramRegistros(TipoRegistro tipo) {
        int id = Integer.MAX_VALUE;
        assertEquals(FalhaRa3.Motivo.NAO_ENCONTRADO, assertThrows(FalhaRa3.class, () -> servico.consultar(tipo, id)).motivo());
        assertEquals(FalhaRa3.Motivo.NAO_ENCONTRADO, assertThrows(FalhaRa3.class, () -> servico.atualizar(alteracao(tipo, id))).motivo());
        assertEquals(FalhaRa3.Motivo.NAO_ENCONTRADO, assertThrows(FalhaRa3.class, () -> servico.excluir(tipo, id, "SIM")).motivo());
    }

    @Test void listagemTemOrdemLimiteEPaginacaoReal() {
        var ids = new ArrayList<Integer>();
        for (int i = 0; i < 21; i++) ids.add(criar(TipoRegistro.GENERO, new NovoGenero(marca + i)));
        var primeira = servico.listar(TipoRegistro.GENERO, 1); var segunda = servico.listar(TipoRegistro.GENERO, 2);
        assertEquals(ids.subList(0, 20), primeira.stream().map(OperacoesRa3.Registro::id).toList());
        assertEquals(List.of(ids.get(20)), segunda.stream().map(OperacoesRa3.Registro::id).toList());
        System.out.println("RA3_LISTAGEM_OK pagina1=20 pagina2=1 ordem=ID_ASC");
    }

    @Test void duplicidadeEFkInexistenteSaoSanitizadasSemInsertParcial() {
        String email = UUID.randomUUID().toString().replace("-", "") + "@t.test";
        criar(TipoRegistro.ORGANIZADOR, new NovoOrganizador(marca, email, documento(18)));
        assertEquals(FalhaRa3.Motivo.DUPLICIDADE, assertThrows(FalhaRa3.class,
                () -> servico.cadastrar(new NovoOrganizador(marca + "dup", email, documento(18)))).motivo());
        int g = genero();
        assertEquals(FalhaRa3.Motivo.NAO_ENCONTRADO, assertThrows(FalhaRa3.class,
                () -> servico.cadastrar(new NovoEvento(marca, UTC, "D", "L", "C", "SP", "12345678", Integer.MAX_VALUE, g))).motivo());
        assertTrue(servico.listar(TipoRegistro.EVENTO, 1).isEmpty());
    }

    @Test void unidadeDeTrabalhoReverteAlteracaoJaEnviadaAoBanco() {
        int id = genero();
        assertThrows(IllegalArgumentException.class, () -> new UnidadeTrabalhoRa3(fabrica).escrever(em -> {
            em.find(GeneroMusical.class, id).renomear(marca + "falha"); em.flush();
            throw new IllegalArgumentException("Falha provocada após flush");
        }));
        assertEquals(marca + "g", servico.consultar(TipoRegistro.GENERO, id).resumo());
        System.out.println("RA3_CRUD_ROLLBACK_OK alteracao_apos_flush=desfeita novo_EM=confirmado");
    }

    @Test void filhoConcorrenteConfirmadoAntesDoLockImpedeExcluirEventoSemCascade() throws Exception {
        int id = evento(); var pool = Executors.newSingleThreadExecutor();
        var concorrente = fabrica.createEntityManager(); var tx = concorrente.getTransaction();
        try {
            PersistenciaRa3.verificarCatalogo(concorrente); tx.begin();
            var filho = new Comunidade(marca + "race", concorrente.find(Evento.class, id), UTC);
            concorrente.persist(filho); concorrente.flush();
            proprios.add(new Chave(TipoRegistro.COMUNIDADE, filho.getId()));
            var iniciou = new CountDownLatch(1);
            var exclusao = pool.submit(() -> {
                iniciou.countDown();
                return assertThrows(FalhaRa3.class, () -> servico.excluir(TipoRegistro.EVENTO, id, "SIM")).motivo();
            });
            assertTrue(iniciou.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> exclusao.get(250, TimeUnit.MILLISECONDS));
            tx.commit();
            assertEquals(FalhaRa3.Motivo.VINCULO, exclusao.get(10, TimeUnit.SECONDS));
            assertEquals(1, contarJdbc(TipoRegistro.EVENTO, id)); assertEquals(1, contarJdbc(TipoRegistro.COMUNIDADE, filho.getId()));
            System.out.println("RA3_CONCORRENCIA_OK pai_bloqueado=aguardou filho_confirmado=preservado cascade=impedido");
        } finally {
            if (tx.isActive()) tx.rollback(); concorrente.close(); pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test void paiExcluidoEntreConsultaEInsertProduzFkRealERollbackSanitizado() throws Exception {
        int pai = evento(); int controle = criar(TipoRegistro.GENERO, new NovoGenero(marca + "ctrl"));
        var leuPai = new CountDownLatch(1); var podeInserir = new CountDownLatch(1);
        var codigoSql = new java.util.concurrent.atomic.AtomicInteger(); var pool = Executors.newSingleThreadExecutor();
        try {
            var filho = pool.submit(() -> assertThrows(FalhaRa3.class, () -> new UnidadeTrabalhoRa3(fabrica).escrever(em -> {
                em.find(GeneroMusical.class, controle).renomear(marca + "falha"); em.flush();
                var eventoLido = em.find(Evento.class, pai); leuPai.countDown();
                try { if (!podeInserir.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout da fixture"); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("Fixture interrompida"); }
                try { em.persist(new TipoIngresso(marca + "fk", BigDecimal.ONE, 1, eventoLido)); em.flush(); }
                catch (PersistenceException e) {
                    Throwable causa = e;
                    while (causa != null && !(causa instanceof java.sql.SQLException)) causa = causa.getCause();
                    if (causa instanceof java.sql.SQLException sql) codigoSql.set(sql.getErrorCode());
                    throw e;
                }
                return null;
            })).motivo());
            assertTrue(leuPai.await(10, TimeUnit.SECONDS));
            assertTrue(servico.excluir(TipoRegistro.EVENTO, pai, "SIM")); podeInserir.countDown();
            assertEquals(FalhaRa3.Motivo.VINCULO, filho.get(10, TimeUnit.SECONDS));
            assertEquals(1452, codigoSql.get()); assertEquals(0, contarJdbc(TipoRegistro.EVENTO, pai));
            assertEquals(marca + "ctrl", servico.consultar(TipoRegistro.GENERO, controle).resumo());
            System.out.println("RA3_FK_CONCORRENTE_OK codigo=1452 mensagem=VINCULO rollback_apos_flush=confirmado");
        } finally {
            podeInserir.countDown(); pool.shutdownNow(); assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private Cadastro cadastro(TipoRegistro tipo) {
        return switch (tipo) {
            case GENERO -> new NovoGenero(marca + "g");
            case ORGANIZADOR -> new NovoOrganizador(marca + "o", UUID.randomUUID().toString().replace("-", "") + "@t.test", documento(18));
            case PARTICIPANTE -> new NovoParticipante(marca + "p", documento(11), LocalDate.of(2000, 1, 2), UUID.randomUUID().toString().replace("-", "") + "@t.test");
            case EVENTO -> new NovoEvento(marca + "e", UTC, marca, "Local", "Cidade", "SP", "12345678", organizador(), genero());
            case TIPO_INGRESSO -> new NovoTipo(marca + "t", new BigDecimal("12.50"), 10, evento());
            case COMUNIDADE -> new NovaComunidade(marca + "c", evento(), UTC);
            case POSTAGEM -> new NovaPostagem(marca + "post", null, participante(), comunidade(), UTC);
        };
    }
    private Alteracao alteracao(TipoRegistro tipo, int id) {
        String nome = marca + "atual";
        return switch (tipo) {
            case GENERO -> new AlterarGenero(id, nome);
            case ORGANIZADOR -> new AlterarOrganizador(id, nome);
            case PARTICIPANTE -> new AlterarParticipante(id, nome);
            case EVENTO -> new AlterarEvento(id, nome, "fixture.png");
            case TIPO_INGRESSO -> new AlterarTipo(id, nome, new BigDecimal("42.50"), 4, 1);
            case COMUNIDADE -> new AlterarComunidade(id, nome, "Nova descrição", "fixture.png");
            case POSTAGEM -> new AlterarPostagem(id, nome, "fixture.png");
        };
    }
    private void verificarAtualizacao(Object entidade) {
        String nome = marca + "atual";
        switch (entidade) {
            case GeneroMusical e -> assertEquals(nome, e.getNome());
            case Organizador e -> assertEquals(nome, e.getNome());
            case Participante e -> assertEquals(nome, e.getNome());
            case Evento e -> { assertEquals(nome, e.getNome()); assertEquals("fixture.png", e.getImagem()); }
            case TipoIngresso e -> { assertEquals(nome, e.getNome()); assertEquals(new BigDecimal("42.50"), e.getValor()); assertEquals(4, e.getQuantidadeDisponivel()); assertEquals(1, e.getQuantidadeVendida()); }
            case Comunidade e -> { assertEquals(nome, e.getNome()); assertEquals("Nova descrição", e.getDescricao()); assertEquals("fixture.png", e.getImagem()); }
            case Postagem e -> { assertEquals(nome, e.getDescricao()); assertEquals("fixture.png", e.getImagem()); }
            default -> fail("Entidade desconhecida");
        }
    }
    private int criar(TipoRegistro tipo, Cadastro dados) { int id = servico.cadastrar(dados).id(); proprios.add(new Chave(tipo, id)); return id; }
    private int genero() { if (genero == null) genero = criar(TipoRegistro.GENERO, cadastro(TipoRegistro.GENERO)); return genero; }
    private int organizador() { if (organizador == null) organizador = criar(TipoRegistro.ORGANIZADOR, cadastro(TipoRegistro.ORGANIZADOR)); return organizador; }
    private int participante() { if (participante == null) participante = criar(TipoRegistro.PARTICIPANTE, cadastro(TipoRegistro.PARTICIPANTE)); return participante; }
    private int evento() { if (evento == null) evento = criar(TipoRegistro.EVENTO, cadastro(TipoRegistro.EVENTO)); return evento; }
    private int comunidade() { if (comunidade == null) comunidade = criar(TipoRegistro.COMUNIDADE, cadastro(TipoRegistro.COMUNIDADE)); return comunidade; }
    private int postagem() { return criar(TipoRegistro.POSTAGEM, cadastro(TipoRegistro.POSTAGEM)); }
    private static String documento(int tamanho) { return new java.math.BigInteger(UUID.randomUUID().toString().replace("-", ""), 16).toString().substring(0, tamanho); }
    private static void prova(TipoRegistro tipo, String acao, int id) { System.out.println("RA3_CRUD_OK entidade=" + tipo + " acao=" + acao + " id=" + id); }

    @AfterEach void limparSomenteIdsProprios() throws Exception {
        for (var chave : proprios.reversed()) {
            boolean existe;
            try (var em = fabrica.createEntityManager()) { existe = em.find(chave.tipo().entidade(), chave.id()) != null; }
            if (existe) assertTrue(servico.excluir(chave.tipo(), chave.id(), "SIM"));
        }
        for (var chave : proprios) assertEquals(0, contarJdbc(chave.tipo(), chave.id()));
        System.out.println("RA3_CRUD_FIXTURE_LIMPA ids_proprios=" + proprios.size() + " restantes=0");
    }
    private static int contarJdbc(TipoRegistro tipo, int id) throws Exception {
        String tabela = tipo.entidade().getAnnotation(Table.class).name();
        String pk = switch (tipo) {
            case GENERO -> "id_genero_musical"; case ORGANIZADOR -> "id_organizador"; case PARTICIPANTE -> "id_participante";
            case EVENTO -> "id_evento"; case TIPO_INGRESSO -> "id_tipos_ingressos"; case COMUNIDADE -> "id_comunidade"; case POSTAGEM -> "id_postagem";
        };
        try (var c = ConexoesRa3IT.abrir(); var ps = c.prepareStatement("SELECT COUNT(*) FROM joinup_poo_ra3." + tabela + " WHERE " + pk + "=?")) {
            ps.setInt(1, id); try (var rs = ps.executeQuery()) { assertTrue(rs.next()); return rs.getInt(1); }
        }
    }
}
