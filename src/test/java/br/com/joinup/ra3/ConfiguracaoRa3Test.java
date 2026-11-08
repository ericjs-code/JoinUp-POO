package br.com.joinup.ra3;

import br.com.joinup.ra3.apresentacao.DiagnosticoRa3;
import br.com.joinup.ra3.persistencia.ConfiguracaoRa3;
import br.com.joinup.ra3.persistencia.PersistenciaRa3;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ConfiguracaoRa3Test {
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
        "jdbc:mysql://localhost/joinup", "jdbc:mysql://localhost/joinup_poo",
        "jdbc:mysql://localhost/joinup_poo_ra3_extra", "jdbc:mysql://localhost/JOINUP_POO_RA3",
        "jdbc:mysql://localhost/joinup_poo_ra3/", "jdbc:mysql://localhost:0/joinup_poo_ra3",
        "jdbc:mysql://localhost:65536/joinup_poo_ra3", "jdbc:mysql://localhost,other/joinup_poo_ra3",
        "jdbc:mysql:loadbalance://localhost/joinup_poo_ra3", "jdbc:h2:mem:joinup_poo_ra3",
        "jdbc:mysql://user:secret@localhost/joinup_poo_ra3", "jdbc:mysql://localhost/%6aoinup_poo_ra3",
        "jdbc:mysql://localhost/joinup_poo_ra3?databaseName=joinup_poo",
        "jdbc:mysql://localhost/joinup_poo_ra3?sessionVariables=sql_mode=ANSI",
        "jdbc:mysql://localhost/joinup_poo_ra3?propertiesTransform=some.Class",
        "jdbc:mysql://localhost/joinup_poo_ra3?password=secret",
        "jdbc:mysql://localhost/joinup_poo_ra3?connectTimeout=1&connectTimeout=2",
        "jdbc:mysql://localhost/joinup_poo_ra3?connectTimeout=0",
        "jdbc:mysql://localhost/joinup_poo_ra3?connectTimeout=1&",
        "jdbc:mysql://localhost/joinup_poo_ra3#joinup_poo"
    })
    void recusaDestinosOuParametrosQuePodemDesviarConexao(String url) {
        var erro = assertThrows(IllegalArgumentException.class, () -> ConfiguracaoRa3.validarUrl(url));
        assertFalse(erro.getMessage().contains("secret"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"jdbc:mysql://localhost/joinup_poo_ra3",
        "jdbc:mysql://127.0.0.1:13316/joinup_poo_ra3?connectTimeout=5000&socketTimeout=10000&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true",
        "jdbc:mysql://mysql.example:3306/joinup_poo_ra3?useSSL=true&characterEncoding=UTF-8"})
    void aceitaSomenteFormatoSimplesPermitido(String url) {
        assertDoesNotThrow(() -> ConfiguracaoRa3.validarUrl(url));
    }

    @Test void naoHerdaCredenciaisDaAplicacaoOriginal() {
        var erro = assertThrows(IllegalArgumentException.class, () -> ConfiguracaoRa3.doAmbiente(Map.of(
                "JOINUP_JDBC_URL", "jdbc:mysql://localhost/joinup_poo",
                "JOINUP_DB_USER", "antigo", "JOINUP_DB_PASSWORD", "segredo")));
        assertEquals("Configure JOINUP_RA3_JDBC_URL.", erro.getMessage());
    }

    @Test void propriedadesRecebemSegredoSemExporEmDiagnostico() {
        var ambiente = Map.of("JOINUP_RA3_JDBC_URL", "jdbc:mysql://localhost/joinup_poo_ra3",
                "JOINUP_RA3_DB_USER", "usuario", "JOINUP_RA3_DB_PASSWORD", "segredo-unico");
        var c = ConfiguracaoRa3.doAmbiente(ambiente);
        assertEquals("segredo-unico", c.propriedadesJpa().get("jakarta.persistence.jdbc.password"));
        assertFalse(c.toString().contains("segredo-unico"));
        assertFalse(c.toString().contains("usuario"));
    }

    @Test void startupRecusaUrlAntesDeConectarESemExporSegredo() {
        var buffer = new ByteArrayOutputStream();
        try (var saida = new PrintStream(buffer)) {
            assertEquals(1, DiagnosticoRa3.executar(Map.of(
                "JOINUP_RA3_JDBC_URL", "jdbc:mysql://localhost/joinup_poo?password=segredo-unico",
                "JOINUP_RA3_DB_USER", "usuario", "JOINUP_RA3_DB_PASSWORD", "segredo-unico"), saida, saida));
        }
        assertTrue(buffer.toString().contains("RA3_CONFIGURACAO_INVALIDA"));
        assertFalse(buffer.toString().contains("segredo-unico"));
        assertFalse(buffer.toString().contains("jdbc:mysql"));
    }

    @Test void catalogoRealDiferenteDaUrlTambemEhRecusado() {
        var consulta = (Query) java.lang.reflect.Proxy.newProxyInstance(Query.class.getClassLoader(),
                new Class<?>[]{Query.class}, (proxy, metodo, args) -> {
                    if (metodo.getName().equals("getSingleResult")) return "joinup_poo";
                    throw new AssertionError("Operação inesperada: " + metodo.getName());
                });
        var em = (EntityManager) java.lang.reflect.Proxy.newProxyInstance(EntityManager.class.getClassLoader(),
                new Class<?>[]{EntityManager.class}, (proxy, metodo, args) -> {
                    if (metodo.getName().equals("createNativeQuery")) {
                        assertEquals("SELECT DATABASE()", args[0]);
                        return consulta;
                    }
                    throw new AssertionError("Escrita ou operação inesperada: " + metodo.getName());
                });
        assertEquals("RA3_CATALOGO_RECUSADO: conexão fora do schema exclusivo.",
                assertThrows(IllegalStateException.class, () -> PersistenciaRa3.verificarCatalogo(em)).getMessage());
    }
}
