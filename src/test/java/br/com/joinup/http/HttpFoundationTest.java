package br.com.joinup.http;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static org.junit.jupiter.api.Assertions.*;

class HttpFoundationTest {
    private static HttpServer servidor;
    private static ExecutorService executor;
    private static HttpClient cliente;
    private static String base;
    private static final JsonCodec JSON = new JsonCodec();

    @BeforeAll static void iniciar() throws Exception {
        var respostas = new HttpResponses(JSON);
        var router = new Router(respostas, new StaticFileHandler(respostas), 1024);
        router.registrar("GET", "/api/health", new HealthHandler(respostas));
        // Rotas exclusivas de teste; não existem na composição de produção.
        router.registrar("POST", "/api/teste/json", (exchange, corpo) ->
                respostas.json(exchange, 200, JSON.ler(corpo, Map.class)));
        router.registrar("GET", "/api/teste/falha", (exchange, corpo) -> {
            throw new IllegalStateException("senha-interna-sql-nao-expor");
        });
        router.iniciar();
        assertThrows(IllegalStateException.class, () -> router.registrar("GET", "/tardia", new HealthHandler(respostas)));
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newFixedThreadPool(4);
        servidor.setExecutor(executor);
        servidor.createContext("/", router);
        servidor.start();
        base = "http://127.0.0.1:" + servidor.getAddress().getPort();
        cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @AfterAll static void fechar() {
        cliente.close();
        servidor.stop(0);
        executor.shutdownNow();
    }

    @Test void healthMinimoSemConfiguracao() throws Exception {
        var resposta = pedir("GET", "/api/health?verificacao=1", "");
        assertEquals(200, resposta.statusCode());
        assertEquals(Map.of("dados", Map.of("status", "UP")), JSON.ler(resposta.body().getBytes(StandardCharsets.UTF_8), Map.class));
        assertEquals("application/json; charset=utf-8", resposta.headers().firstValue("Content-Type").orElseThrow());
        assertTrue(resposta.headers().firstValue("Access-Control-Allow-Origin").isEmpty());
        assertTrue(resposta.headers().firstValue("Set-Cookie").isEmpty());
    }

    @ParameterizedTest @ValueSource(strings = {"/api/inexistente", "/api/health/extra", "/api/health/", "/api", "/ausente.html",
            "/db/schema_v0.2.sql", "/db/seed_demo_v0.2.sql", "/pom.xml", "/public/index.html"})
    void recursoDesconhecidoNaoVazaArquivos(String caminho) throws Exception {
        assertErro(pedir("GET", caminho, ""), 404, "RECURSO_NAO_ENCONTRADO");
    }

    @ParameterizedTest @ValueSource(strings = {"POST", "PATCH", "DELETE", "OPTIONS", "PUT"})
    void metodoInvalidoEmRotaConhecida(String metodo) throws Exception {
        var resposta = pedir(metodo, "/api/health", "");
        assertErro(resposta, 405, "METODO_NAO_PERMITIDO");
        assertEquals("GET", resposta.headers().firstValue("Allow").orElseThrow());
        assertErro(pedir(metodo, "/api/desconhecida", ""), 404, "RECURSO_NAO_ENCONTRADO");
    }

    @Test void htmlCssJsEHead() throws Exception {
        var html = pedir("GET", "/", "");
        assertEquals(200, html.statusCode());
        assertTrue(html.body().contains("<html lang=\"pt-BR\">"));
        assertEquals(200, pedir("GET", "/index.html", "").statusCode());
        assertTrue(pedir("GET", "/css/inicio.css", "").headers().firstValue("Content-Type").orElseThrow().startsWith("text/css"));
        assertTrue(pedir("GET", "/js/inicio.js", "").body().contains("/api/health"));
        var head = pedir("HEAD", "/", "");
        assertEquals(200, head.statusCode());
        assertEquals("", head.body());
        assertTrue(head.headers().firstValue("Content-Length").isPresent());
        var post = pedir("POST", "/index.html", "");
        assertErro(post, 405, "METODO_NAO_PERMITIDO");
        assertEquals("GET, HEAD", post.headers().firstValue("Allow").orElseThrow());
        var headHealth = pedir("HEAD", "/api/health", "");
        assertEquals(405, headHealth.statusCode());
        assertEquals("", headHealth.body());
    }

    @ParameterizedTest @ValueSource(strings = {"/../db/schema_v0.2.sql", "/%2e%2e/db/schema_v0.2.sql",
            "/css/../../db/seed_demo_v0.2.sql", "/%252e%252e/db/schema_v0.2.sql", "/%5c..%5cdb/schema_v0.2.sql",
            "/%00index.html", "/css/%2e%2e/%2e%2e/db/schema_v0.2.sql", "/css/%2F..%2Fdb/schema_v0.2.sql"})
    void bloqueiaTraversalSimplesECodificado(String caminho) throws Exception {
        assertErro(pedir("GET", caminho, ""), 400, "REQUISICAO_INVALIDA");
    }

    @ParameterizedTest @ValueSource(strings = {"{", "", "null", "[]", "{\"a\":1} {\"b\":2}", "{\"a\":1,\"a\":2}"})
    void jsonInvalidoTemErroPadronizado(String corpo) throws Exception {
        assertErro(pedir("POST", "/api/teste/json", corpo), 400, "REQUISICAO_INVALIDA");
    }

    @Test void limitaContentLengthEChunked() throws Exception {
        assertErro(pedir("POST", "/api/teste/json", "x".repeat(1025)), 413, "REQUISICAO_MUITO_GRANDE");
        var publisher = HttpRequest.BodyPublishers.ofInputStream(() ->
                new java.io.ByteArrayInputStream("x".repeat(1025).getBytes(StandardCharsets.UTF_8)));
        assertEquals(-1, publisher.contentLength());
        var requisicao = HttpRequest.newBuilder(URI.create(base + "/api/teste/json"))
                .timeout(Duration.ofSeconds(5)).POST(publisher).build();
        assertErro(cliente.send(requisicao, HttpResponse.BodyHandlers.ofString()), 413, "REQUISICAO_MUITO_GRANDE");
        assertEquals(200, pedir("GET", "/api/health", "").statusCode());
        assertEquals(200, pedir("POST", "/api/teste/json", "{\"a\":\"" + "x".repeat(1016) + "\"}").statusCode());
    }

    @Test void excecaoInternaNaoRevelaMensagemOriginal() throws Exception {
        var resposta = pedir("GET", "/api/teste/falha", "");
        assertErro(resposta, 500, "ERRO_INTERNO");
        assertFalse(resposta.body().contains("senha-interna"));
        assertEquals(200, pedir("GET", "/api/health", "").statusCode());
    }

    @Test void corposConcorrentesPermanecemIsolados() {
        var chamadas = new ArrayList<CompletableFuture<Void>>();
        for (int i = 0; i < 32; i++) {
            String corpo = "{\"id\":" + i + "}";
            var requisicao = HttpRequest.newBuilder(URI.create(base + "/api/teste/json"))
                    .timeout(Duration.ofSeconds(10)).POST(HttpRequest.BodyPublishers.ofString(corpo)).build();
            chamadas.add(cliente.sendAsync(requisicao, HttpResponse.BodyHandlers.ofString()).thenAccept(resposta -> {
                assertEquals(200, resposta.statusCode());
                assertEquals(corpo, resposta.body());
            }));
        }
        CompletableFuture.allOf(chamadas.toArray(CompletableFuture[]::new)).join();
    }

    private static HttpResponse<String> pedir(String metodo, String caminho, String corpo) throws Exception {
        var requisicao = HttpRequest.newBuilder(URI.create(base + caminho)).timeout(Duration.ofSeconds(5))
                .method(metodo, HttpRequest.BodyPublishers.ofString(corpo)).build();
        return cliente.send(requisicao, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static void assertErro(HttpResponse<String> resposta, int status, String codigo) {
        assertEquals(status, resposta.statusCode());
        Map<?, ?> envelope = JSON.ler(resposta.body().getBytes(StandardCharsets.UTF_8), Map.class);
        Map<?, ?> erro = (Map<?, ?>) envelope.get("erro");
        assertEquals(codigo, erro.get("codigo"));
        assertInstanceOf(String.class, erro.get("mensagem"));
        assertEquals(Map.of(), erro.get("detalhes"));
        assertEquals("nosniff", resposta.headers().firstValue("X-Content-Type-Options").orElseThrow());
    }
}
