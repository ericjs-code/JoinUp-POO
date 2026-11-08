package br.com.joinup.ra3;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class Ra3MainTest {
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void diagnosticoNegativoEmOutraJvmFalhaSemStacktraceOuSegredo(boolean configurado) throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        var comando = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"),
                Ra3Main.class.getName(), "--diagnostico").redirectErrorStream(true);
        var ambiente = comando.environment();
        ambiente.keySet().removeIf(k -> k.startsWith("JOINUP_RA3_"));
        if (configurado) {
            ambiente.put("JOINUP_RA3_JDBC_URL", "jdbc:mysql://127.0.0.1:1/joinup_poo_ra3?connectTimeout=1000");
            ambiente.put("JOINUP_RA3_DB_USER", "fixture");
            ambiente.put("JOINUP_RA3_DB_PASSWORD", "SEGREDO_SENTINELA_RA3");
        }
        var processo = comando.start();
        try {
            // stdin aberto durante a espera, também no caso de falha.
            assertTrue(processo.waitFor(15, TimeUnit.SECONDS));
            String texto = new String(processo.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertNotEquals(0, processo.exitValue());
            assertTrue(texto.contains(configurado ? "RA3_CONEXAO_FALHOU" : "RA3_CONFIGURACAO_INVALIDA"));
            assertFalse(texto.contains("\tat ")); assertFalse(texto.contains("jdbc:mysql:"));
            assertFalse(texto.contains("SEGREDO_SENTINELA_RA3")); assertFalse(texto.contains("SELECT "));
        } finally {
            processo.destroyForcibly(); processo.getOutputStream().close();
        }
    }
}
