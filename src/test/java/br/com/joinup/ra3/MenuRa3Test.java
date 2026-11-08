package br.com.joinup.ra3;

import br.com.joinup.ra3.apresentacao.*;
import br.com.joinup.ra3.persistencia.UnidadeTrabalhoRa3;
import br.com.joinup.ra3.servico.*;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class MenuRa3Test {
    @ParameterizedTest @EnumSource(TipoRegistro.class)
    void cancelarEmCadaCampoDoCadastroNaoEnviaComando(TipoRegistro tipo) throws Exception {
        var campos = camposCadastro(tipo);
        for (int campo = 0; campo < campos.size(); campo++) {
            var linhas = new ArrayList<String>(List.of("" + (tipo.ordinal() + 1), "1"));
            linhas.addAll(campos.subList(0, campo)); linhas.add("/cancelar"); linhas.addAll(List.of("0", "0"));
            var fake = new OperacoesTeste();
            String texto = executar(linhas, fake);
            assertTrue(fake.cadastros.isEmpty(), tipo + " campo " + campo);
            assertTrue(texto.contains("Operação cancelada. Nenhuma alteração foi enviada."));
            assertTrue(texto.contains("/cancelar em qualquer campo"));
            assertTrue(texto.contains("Programa encerrado."));
        }
    }

    @ParameterizedTest @EnumSource(TipoRegistro.class)
    void cancelarNoIdEOuEmCadaCampoDaAtualizacaoNaoEnviaComando(TipoRegistro tipo) throws Exception {
        var campos = new ArrayList<String>(List.of("1")); campos.addAll(camposAlteracao(tipo));
        for (int campo = 0; campo < campos.size(); campo++) {
            var linhas = new ArrayList<String>(List.of("" + (tipo.ordinal() + 1), "4"));
            linhas.addAll(campos.subList(0, campo)); linhas.add(" /cancelar "); linhas.addAll(List.of("0", "0"));
            var fake = new OperacoesTeste();
            String texto = executar(linhas, fake);
            assertTrue(fake.alteracoes.isEmpty(), tipo + " campo " + campo);
            assertTrue(texto.contains("Operação cancelada. Nenhuma alteração foi enviada."));
        }
    }

    @Test void zeroContinuaValidoParaValorEQuantidades() throws Exception {
        var fake = new OperacoesTeste();
        executar(List.of("5", "1", "Gratuito", "0", "0", "1", "4", "1", "Gratuito", "0", "0", "0", "0", "0"), fake);
        var criado = (DadosRa3.NovoTipo) fake.cadastros.getFirst();
        var atualizado = (DadosRa3.AlterarTipo) fake.alteracoes.getFirst();
        assertEquals(new java.math.BigDecimal("0.00"), criado.valor()); assertEquals(0, criado.quantidade());
        assertEquals(0, atualizado.disponivel()); assertEquals(0, atualizado.vendida());
    }

    @ParameterizedTest @EnumSource(TipoRegistro.class)
    void eofNoUltimoCampoDoCadastroEDaAtualizacaoNaoEnviaMutacao(TipoRegistro tipo) throws Exception {
        for (boolean atualizar : List.of(false, true)) {
            var campos = atualizar ? camposAlteracao(tipo) : camposCadastro(tipo);
            var linhas = new ArrayList<String>(List.of("" + (tipo.ordinal() + 1), atualizar ? "4" : "1"));
            if (atualizar) linhas.add("1");
            linhas.addAll(campos.subList(0, campos.size() - 1));
            var fake = new OperacoesTeste(); var out = new StringWriter();
            new MenuRa3(new StringReader(String.join("\n", linhas) + "\n"), new PrintWriter(out), fake).executar();
            assertTrue(fake.cadastros.isEmpty()); assertTrue(fake.alteracoes.isEmpty());
            assertTrue(out.toString().contains("Fim da entrada."));
        }
    }

    @Test void cancelarConsultaPaginaEConfirmacaoRetornaAoSubmenu() throws Exception {
        var fake = new OperacoesTeste();
        String texto = executar(List.of("/cancelar", "1", "/cancelar", "2", "/cancelar", "3", "/cancelar", "5", "1", "/cancelar", "0", "0"), fake);
        assertEquals(1, fake.consultas); assertTrue(fake.confirmacoes.isEmpty());
        assertEquals(3, texto.split("Operação cancelada", -1).length - 1);
        assertTrue(texto.contains("Número inválido"));
    }

    @ParameterizedTest @EnumSource(TipoRegistro.class)
    void seteMenusEncaminhamCadastroConsultaListagemAtualizacaoEExclusao(TipoRegistro tipo) throws Exception {
        var entrada = new ArrayList<String>();
        entrada.add("" + (tipo.ordinal() + 1)); entrada.add("1"); entrada.addAll(camposCadastro(tipo));
        entrada.addAll(List.of("2", "1", "3", "1", "4", "1")); entrada.addAll(camposAlteracao(tipo));
        entrada.addAll(List.of("5", "1", "NAO", "5", "1", "SIM", "0", "0"));
        var fake = new OperacoesTeste();
        String texto = executar(entrada, fake);
        assertEquals(1, fake.cadastros.size()); assertEquals(1, fake.alteracoes.size());
        assertEquals(List.of("NAO", "SIM"), fake.confirmacoes);
        assertEquals(4, fake.consultas);
        assertTrue(texto.contains("Cadastrado. ID: 1")); assertTrue(texto.contains("Atualizado. ID: 1"));
        assertTrue(texto.contains("Exclusão cancelada.")); assertTrue(texto.contains("Excluído. ID: 1"));
        assertTrue(texto.contains("limite 20")); assertTrue(texto.contains("Programa encerrado."));
        assertFalse(texto.contains(ServicoCrudRa3.MARCADOR_SEM_LOGIN));
    }

    @Test void opcoesIdsETextoInvalidosSaoRecuperaveis() throws Exception {
        var fake = new OperacoesTeste();
        String texto = executar(List.of("abc", "8", "1", "9", "1", "", "Nome", "2", "0", "-1", "abc", "1", "0", "0"), fake);
        assertEquals(1, fake.cadastros.size()); assertEquals(1, fake.consultas);
        assertTrue(texto.contains("Número inválido")); assertTrue(texto.contains("Texto obrigatório"));
    }

    @Test void decimalQuantidadeEDatasInvalidasNaoEncerramMenu() throws Exception {
        var fake = new OperacoesTeste();
        String texto = executar(List.of("5", "1", "Pista", "abc", "1.001", "12,50", "12.50", "-1", "2", "1", "0",
                "3", "1", "Pessoa", "00000000000", "2000-02-30", "2000-01-02", "p@t.test", "0",
                "6", "1", "Comunidade", "1", "amanha", "2030-11-12T18:30:00", "0", "0"), fake);
        assertEquals(3, fake.cadastros.size());
        assertTrue(texto.contains("Valor inválido")); assertTrue(texto.contains("Data inválida")); assertTrue(texto.contains("Data UTC inválida"));
    }

    @ParameterizedTest @ValueSource(strings = {"", "1\n", "1\n1\n", "1\n5\n1\n"})
    void eofEmQualquerNivelTerminaNormalmenteSemCadastrarOuExcluir(String script) throws Exception {
        var fake = new OperacoesTeste(); var out = new StringWriter();
        new MenuRa3(new StringReader(script), new PrintWriter(out), fake).executar();
        assertEquals(0, fake.cadastros.size()); assertTrue(fake.confirmacoes.isEmpty());
        assertTrue(out.toString().contains("Fim da entrada.")); assertTrue(out.toString().contains("Programa encerrado."));
    }

    @Test void diagnosticoNaoLeEntradaEFechaUnicaFabrica() {
        var fechamentos = new AtomicInteger();
        var app = new AplicacaoRa3(c -> fabricaSemOperacoes(fechamentos));
        Reader entrada = new Reader() { public int read(char[] b, int o, int n) { throw new AssertionError("Diagnóstico não lê console"); } public void close() { } };
        var out = new StringWriter();
        assertEquals(0, app.executar(new String[]{"--diagnostico"}, ambiente(), entrada, new PrintWriter(out), new PrintWriter(out)));
        assertEquals(1, fechamentos.get()); assertTrue(out.toString().contains("RA3_CONEXAO_OK"));
        assertFalse(out.toString().contains("1. Cadastrar"));
    }

    @Test void menuEofFechaUnicaFabricaSemOperacoesDeBanco() {
        var fechamentos = new AtomicInteger(); var out = new StringWriter();
        assertEquals(0, new AplicacaoRa3(c -> fabricaSemOperacoes(fechamentos)).executar(new String[0], ambiente(), Reader.nullReader(), new PrintWriter(out), new PrintWriter(out)));
        assertEquals(1, fechamentos.get()); assertTrue(out.toString().contains("JOINUP-POO | RA3"));
    }

    @Test void falhaDeConexaoEArgumentosNaoVazamDetalhes() {
        var out = new StringWriter();
        var app = new AplicacaoRa3(c -> { throw new IllegalStateException("jdbc:mysql://localhost senha=SEGREDO_SENTINELA"); });
        assertEquals(1, app.executar(new String[]{"--diagnostico"}, ambiente(), Reader.nullReader(), new PrintWriter(out), new PrintWriter(out)));
        assertEquals(1, app.executar(new String[]{"argumento-secreto"}, ambiente(), Reader.nullReader(), new PrintWriter(out), new PrintWriter(out)));
        assertTrue(out.toString().contains("RA3_CONEXAO_FALHOU"));
        assertFalse(out.toString().contains("SEGREDO_SENTINELA")); assertFalse(out.toString().contains("jdbc:mysql"));
        assertFalse(out.toString().contains("argumento-secreto"));
    }

    @ParameterizedTest @ValueSource(ints = {1062, 1451, 1452, 1048, 1213, 1205, 3819})
    void mensagensDeFalhaSqlSaoSanitizadas(int codigo) {
        var erro = UnidadeTrabalhoRa3.traduzir(new PersistenceException(new SQLException("SEGREDO_SENTINELA jdbc:mysql SELECT *", "23000", codigo)));
        assertNull(erro.getCause()); assertFalse(erro.getMessage().contains("SEGREDO")); assertFalse(erro.getMessage().contains("SELECT"));
        assertFalse(erro.getMessage().contains("jdbc:"));
    }

    private static EntityManagerFactory fabricaSemOperacoes(AtomicInteger fechamentos) {
        return (EntityManagerFactory) java.lang.reflect.Proxy.newProxyInstance(EntityManagerFactory.class.getClassLoader(), new Class<?>[]{EntityManagerFactory.class},
                (p, metodo, args) -> { if (metodo.getName().equals("close")) { fechamentos.incrementAndGet(); return null; } throw new AssertionError("Operação inesperada: " + metodo.getName()); });
    }
    private static Map<String, String> ambiente() { return Map.of("JOINUP_RA3_JDBC_URL", "jdbc:mysql://localhost/joinup_poo_ra3", "JOINUP_RA3_DB_USER", "fixture", "JOINUP_RA3_DB_PASSWORD", "SEGREDO_SENTINELA"); }
    private static String executar(List<String> linhas, OperacoesTeste fake) throws Exception {
        var out = new StringWriter(); new MenuRa3(new StringReader(String.join("\n", linhas) + "\n"), new PrintWriter(out), fake).executar(); return out.toString();
    }
    static List<String> camposCadastro(TipoRegistro tipo) {
        return switch (tipo) {
            case GENERO -> List.of("Nome");
            case ORGANIZADOR -> List.of("Nome", "o@t.test", "000000000000000000");
            case PARTICIPANTE -> List.of("Nome", "00000000000", "2000-01-02", "p@t.test");
            case EVENTO -> List.of("Nome", "2030-11-12T18:30:00", "Descrição", "Local", "Cidade", "SP", "12345678", "1", "1");
            case TIPO_INGRESSO -> List.of("Nome", "10.50", "20", "1");
            case COMUNIDADE -> List.of("Nome", "1", "2030-11-12T18:30:00");
            case POSTAGEM -> List.of("Conteúdo", "", "1", "1", "2030-11-12T18:30:00");
        };
    }
    static List<String> camposAlteracao(TipoRegistro tipo) {
        return switch (tipo) {
            case GENERO, ORGANIZADOR, PARTICIPANTE -> List.of("Atual");
            case EVENTO -> List.of("Atual", "imagem.png");
            case TIPO_INGRESSO -> List.of("Atual", "20.50", "30", "1");
            case COMUNIDADE -> List.of("Atual", "Descrição", "imagem.png");
            case POSTAGEM -> List.of("Atual", "imagem.png");
        };
    }
    private static final class OperacoesTeste implements OperacoesRa3 {
        final List<DadosRa3.Cadastro> cadastros = new ArrayList<>(); final List<DadosRa3.Alteracao> alteracoes = new ArrayList<>();
        final List<String> confirmacoes = new ArrayList<>(); int consultas;
        public Registro cadastrar(DadosRa3.Cadastro d) { cadastros.add(d); return new Registro(1, "Registro"); }
        public Registro consultar(TipoRegistro t, int id) { consultas++; return new Registro(id, "Registro"); }
        public List<Registro> listar(TipoRegistro t, int pagina) { return List.of(new Registro(1, "Registro")); }
        public Registro atualizar(DadosRa3.Alteracao a) { alteracoes.add(a); return new Registro(a.id(), "Registro"); }
        public boolean excluir(TipoRegistro t, int id, String c) { confirmacoes.add(c); return c.equals("SIM"); }
    }
}
