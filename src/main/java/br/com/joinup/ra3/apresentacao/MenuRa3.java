package br.com.joinup.ra3.apresentacao;

import br.com.joinup.ra3.servico.*;
import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.function.Function;
import static br.com.joinup.ra3.servico.DadosRa3.*;

/** Loops iterativos, entrada/saída injetáveis; não conhece credenciais ou entidades gerenciadas. */
public final class MenuRa3 {
    private final BufferedReader entrada;
    private final PrintWriter saida;
    private final OperacoesRa3 operacoes;
    public MenuRa3(Reader entrada, PrintWriter saida, OperacoesRa3 operacoes) {
        this.entrada = new BufferedReader(entrada); this.saida = saida; this.operacoes = operacoes;
    }

    public void executar() throws IOException {
        saida.println("====================================\n         JOINUP-POO | RA3\n       JPA + EclipseLink / MySQL\n====================================");
        saida.println("Demonstração acadêmica: use somente CPF/CNPJ e dados fictícios. Não há login ou senha pessoal.");
        try {
            while (true) {
                for (int i = 0; i < TipoRegistro.values().length; i++) saida.println((i + 1) + ". " + TipoRegistro.values()[i].titulo());
                saida.println("0. Encerrar");
                int opcao = inteiro("Opção:", 0, 7);
                if (opcao == 0) break;
                submenu(TipoRegistro.values()[opcao - 1]);
            }
        } catch (FimEntrada ignored) { saida.println("Fim da entrada."); }
        saida.println("Programa encerrado."); saida.flush();
    }

    private void submenu(TipoRegistro tipo) throws IOException {
        while (true) {
            saida.println("\n" + tipo.titulo() + "\n1. Cadastrar\n2. Consultar por ID\n3. Listar\n4. Atualizar\n5. Excluir\n0. Voltar");
            int acao = inteiro("Ação:", 0, 5);
            if (acao == 0) return;
            try {
                switch (acao) {
                    case 1 -> mostrar("Cadastrado", operacoes.cadastrar(cadastro(tipo)));
                    case 2 -> mostrar("Consulta", operacoes.consultar(tipo, id()));
                    case 3 -> listar(tipo, inteiro("Página (20 registros por página, começando em 1):", 1, Integer.MAX_VALUE / OperacoesRa3.LIMITE));
                    case 4 -> {
                        int id = id(); mostrar("Atual", operacoes.consultar(tipo, id));
                        mostrar("Atualizado", operacoes.atualizar(alteracao(tipo, id)));
                    }
                    case 5 -> {
                        int id = id(); mostrar("Excluir", operacoes.consultar(tipo, id));
                        String confirmacao = linha("Digite SIM para excluir somente este registro; qualquer outra resposta cancela:").strip();
                        saida.println(operacoes.excluir(tipo, id, confirmacao) ? "Excluído. ID: " + id : "Exclusão cancelada.");
                    }
                    default -> throw new IllegalStateException("Ação fora do menu.");
                }
            } catch (FalhaRa3 | IllegalArgumentException e) {
                saida.println(e.getMessage());
            }
        }
    }

    private Cadastro cadastro(TipoRegistro tipo) throws IOException {
        return switch (tipo) {
            case GENERO -> new NovoGenero(texto("Nome:"));
            case ORGANIZADOR -> new NovoOrganizador(texto("Nome:"), texto("E-mail fictício:"), texto("CNPJ fictício (até 18 caracteres):"));
            case PARTICIPANTE -> new NovoParticipante(texto("Nome:"), texto("CPF fictício (até 11 caracteres):"), nascimento(), texto("E-mail fictício:"));
            case EVENTO -> new NovoEvento(texto("Nome:"), data(), texto("Descrição:"), texto("Localização:"), texto("Cidade:"),
                    texto("UF (ex.: SP):"), texto("CEP fictício (até 8 caracteres):"), pai(TipoRegistro.ORGANIZADOR), pai(TipoRegistro.GENERO));
            case TIPO_INGRESSO -> new NovoTipo(texto("Nome:"), valor(), quantidade("Quantidade disponível:"), pai(TipoRegistro.EVENTO));
            case COMUNIDADE -> new NovaComunidade(texto("Nome:"), pai(TipoRegistro.EVENTO), data());
            case POSTAGEM -> new NovaPostagem(opcional("Descrição (vazio se somente imagem):"), opcional("Imagem (referência opcional):"),
                    pai(TipoRegistro.PARTICIPANTE), pai(TipoRegistro.COMUNIDADE), data());
        };
    }

    private Alteracao alteracao(TipoRegistro tipo, int id) throws IOException {
        return switch (tipo) {
            case GENERO -> new AlterarGenero(id, texto("Novo nome:"));
            case ORGANIZADOR -> new AlterarOrganizador(id, texto("Novo nome:"));
            case PARTICIPANTE -> new AlterarParticipante(id, texto("Novo nome:"));
            case EVENTO -> new AlterarEvento(id, texto("Novo nome:"), opcional("Imagem (vazio limpa):"));
            case TIPO_INGRESSO -> new AlterarTipo(id, texto("Novo nome:"), valor(), quantidade("Quantidade disponível:"), quantidade("Quantidade vendida:"));
            case COMUNIDADE -> new AlterarComunidade(id, texto("Novo nome:"), opcional("Descrição (vazio limpa):"), opcional("Imagem (vazio limpa):"));
            case POSTAGEM -> new AlterarPostagem(id, opcional("Descrição:"), opcional("Imagem:"));
        };
    }

    private void listar(TipoRegistro tipo, int pagina) {
        var registros = operacoes.listar(tipo, pagina);
        saida.println(tipo.titulo() + " — página " + pagina + ", limite " + OperacoesRa3.LIMITE + ", ordem por ID.");
        if (registros.isEmpty()) saida.println("Nenhum registro nesta página.");
        registros.forEach(r -> mostrar("Registro", r));
        saida.println("Para outra página, escolha Listar novamente e informe o número.");
    }
    private int pai(TipoRegistro tipo) throws IOException {
        listar(tipo, 1);
        return inteiro("ID existente de " + tipo.titulo() + " (consulte outras páginas no respectivo menu):", 1, Integer.MAX_VALUE);
    }
    private void mostrar(String acao, OperacoesRa3.Registro registro) {
        saida.println(acao + ". ID: " + registro.id() + " | " + registro.resumo());
    }
    private int id() throws IOException { return inteiro("ID:", 1, Integer.MAX_VALUE); }
    private int quantidade(String rotulo) throws IOException { return inteiro(rotulo, 0, Integer.MAX_VALUE); }
    private int inteiro(String rotulo, int minimo, int maximo) throws IOException {
        return ler(rotulo, s -> {
            int n = Integer.parseInt(s);
            if (n < minimo || n > maximo) throw new IllegalArgumentException();
            return n;
        }, "Número inválido. Informe inteiro entre " + minimo + " e " + maximo + ".");
    }
    private BigDecimal valor() throws IOException {
        return ler("Valor (ex.: 125.50):", s -> {
            if (!s.matches("[0-9]+(?:\\.[0-9]{1,2})?")) throw new IllegalArgumentException();
            var valor = new BigDecimal(s);
            if (valor.compareTo(new BigDecimal("999999.99")) > 0) throw new IllegalArgumentException();
            return valor;
        }, "Valor inválido. Use ponto e no máximo duas casas, entre 0.00 e 999999.99.");
    }
    private LocalDate nascimento() throws IOException {
        return ler("Nascimento fictício (yyyy-MM-dd):", LocalDate::parse, "Data inválida. Use yyyy-MM-dd.");
    }
    private LocalDateTime data() throws IOException {
        return ler("Data UTC (yyyy-MM-ddTHH:mm:ss, ex.: 2030-11-12T18:30:00):", s -> {
            if (!s.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}")) throw new IllegalArgumentException();
            return LocalDateTime.parse(s);
        }, "Data UTC inválida. Use yyyy-MM-ddTHH:mm:ss.");
    }
    private String texto(String rotulo) throws IOException {
        return ler(rotulo, s -> { if (s.isBlank()) throw new IllegalArgumentException(); return s; }, "Texto obrigatório não pode ficar em branco.");
    }
    private String opcional(String rotulo) throws IOException { String s = linha(rotulo); return s.isBlank() ? null : s; }
    private <T> T ler(String rotulo, Function<String, T> conversor, String mensagem) throws IOException {
        while (true) {
            String valor = linha(rotulo).strip();
            try { return conversor.apply(valor); }
            catch (IllegalArgumentException | DateTimeParseException e) { saida.println(mensagem); }
        }
    }
    private String linha(String rotulo) throws IOException {
        saida.println(rotulo); saida.flush();
        String linha = entrada.readLine();
        if (linha == null) throw new FimEntrada();
        return linha;
    }
    private static final class FimEntrada extends RuntimeException { }
}
