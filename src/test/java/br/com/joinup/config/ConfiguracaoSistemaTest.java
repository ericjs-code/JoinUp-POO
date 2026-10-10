package br.com.joinup.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class ConfiguracaoSistemaTest {
    @Test void mesmaInstanciaEmChamadasConcorrentes() throws Exception {
        try (var executor = Executors.newFixedThreadPool(16)) {
            var chamadas = new ArrayList<java.util.concurrent.Callable<ConfiguracaoSistema>>();
            for (int i = 0; i < 100; i++) chamadas.add(ConfiguracaoSistema::getInstancia);
            var resultados = executor.invokeAll(chamadas);
            var instancia = ConfiguracaoSistema.getInstancia();
            for (var resultado : resultados) assertSame(instancia, resultado.get());
        }
    }

    @Test void estruturaImutavelEConstrutorPrivado() {
        assertTrue(Modifier.isFinal(ConfiguracaoSistema.class.getModifiers()));
        for (var construtor : ConfiguracaoSistema.class.getDeclaredConstructors()) {
            assertTrue(Modifier.isPrivate(construtor.getModifiers()));
        }
        for (var campo : ConfiguracaoSistema.class.getDeclaredFields()) {
            assertTrue(Modifier.isPrivate(campo.getModifiers()));
            assertTrue(Modifier.isFinal(campo.getModifiers()));
        }
    }

    @Test void propriedadesTemPrecedenciaESnapshotNaoMuda() throws Exception {
        assertEquals("9123:false:true", probe(List.of("-Djoinup.http.port=9123"), "8123"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-Djoinup.http.port=0", "-Djoinup.http.port=65536", "-Djoinup.http.port=abc",
            "-Djoinup.http.threads=0", "-Djoinup.maxRequestBytes=1048577", "-Djoinup.demo=talvez",
            "-Djoinup.jdbc.url=jdbc:mysql://localhost:3306/joinup"})
    void rejeitaConfiguracaoInvalida(String propriedade) throws Exception {
        assertEquals("INVALIDA", probe(List.of(propriedade), null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"jdbc:mysql://localhost:3306/joinup", "jdbc:mysql://localhost:3306/joinup_poo_outro",
            "jdbc:mysql://localhost:3306/%6aoinup_poo", "jdbc:mysql://usuario:senha@localhost/joinup_poo",
            "jdbc:mysql://localhost/joinup_poo#fragmento", "jdbc:h2:mem:joinup_poo"})
    void urlNuncaAceitaLegadoOuDestinoAmbiguo(String url) {
        var erro = assertThrows(IllegalArgumentException.class, () -> ConfiguracaoSistema.validarUrlJdbc(url));
        assertFalse(erro.getMessage().contains(url));
    }

    private String probe(List<String> propriedades, String portaAmbiente) throws Exception {
        var comando = new ArrayList<String>();
        comando.add(Path.of(System.getProperty("java.home"), "bin", "java.exe").toString());
        // Compatível com Linux/macOS também.
        if (!java.nio.file.Files.exists(Path.of(comando.getFirst()))) {
            comando.set(0, Path.of(System.getProperty("java.home"), "bin", "java").toString());
        }
        comando.addAll(propriedades);
        comando.addAll(List.of("-cp", System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
                ConfigProbe.class.getName()));
        var builder = new ProcessBuilder(comando).redirectErrorStream(true);
        builder.environment().keySet().removeIf(chave -> chave.startsWith("JOINUP_")
                || chave.equals("JAVA_TOOL_OPTIONS") || chave.equals("JDK_JAVA_OPTIONS") || chave.equals("_JAVA_OPTIONS"));
        if (portaAmbiente != null) builder.environment().put("JOINUP_HTTP_PORT", portaAmbiente);
        var processo = builder.start();
        try {
            assertTrue(processo.waitFor(10, TimeUnit.SECONDS));
            assertEquals(0, processo.exitValue());
            return new String(processo.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
        } finally {
            processo.destroyForcibly();
        }
    }

    public static class ConfigProbe {
        public static void main(String[] args) {
            try {
                var configuracao = ConfiguracaoSistema.getInstancia();
                int porta = configuracao.getPortaHttp();
                System.setProperty("joinup.http.port", "9999");
                System.out.print(porta + ":" + configuracao.isModoDemo() + ":"
                        + (porta == ConfiguracaoSistema.getInstancia().getPortaHttp()));
            } catch (ExceptionInInitializerError e) {
                System.out.print("INVALIDA");
            }
        }
    }
}
