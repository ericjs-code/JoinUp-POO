package br.com.joinup.ra3;

import br.com.joinup.ra3.persistencia.ConfiguracaoRa3;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;

/** Suporte de teste: conexão física independente, sem DDL e sem fallback RA2. */
final class ConexoesRa3IT {
    private ConexoesRa3IT() { }

    static ConfiguracaoRa3 configuracao() {
        assertEquals("instancia-descartavel-ra3", System.getenv("JOINUP_RA3_IT_DATABASE"));
        return ConfiguracaoRa3.doAmbiente(System.getenv());
    }

    static Connection abrir() throws SQLException {
        var p = configuracao().propriedadesJpa();
        var propriedades = new java.util.Properties();
        propriedades.setProperty("user", (String) p.get("jakarta.persistence.jdbc.user"));
        propriedades.setProperty("password", (String) p.get("jakarta.persistence.jdbc.password"));
        propriedades.setProperty("connectionTimeZone", "UTC");
        propriedades.setProperty("forceConnectionTimeZoneToSession", "true");
        propriedades.setProperty("connectTimeout", "5000");
        propriedades.setProperty("socketTimeout", "10000");
        var c = DriverManager.getConnection((String) p.get("jakarta.persistence.jdbc.url"), propriedades);
        try (var s = c.createStatement(); var rs = s.executeQuery("SELECT DATABASE()")) {
            assertTrue(rs.next());
            assertEquals(ConfiguracaoRa3.CATALOGO, rs.getString(1));
            return c;
        } catch (SQLException | RuntimeException | Error e) {
            c.close();
            throw e;
        }
    }
}
