package br.com.joinup.http;

import br.com.joinup.auth.SegurancaHttp;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class RouterParametersTest {
    private static HttpServer server;
    private static ExecutorService executor;
    private static HttpClient client;
    private static String base;
    @BeforeAll static void iniciar() throws Exception {
        var json = new JsonCodec(); var r = new HttpResponses(json);
        var router = new Router(r, new StaticFileHandler(r), 65536);
        router.registrar("GET", "/api/contas/{id}", (e, b, p) -> r.json(e, 200, Map.of("id", RouteParameters.id(p.caminho().get("id")), "query", p.query())));
        router.registrar("POST", "/api/contas/fixa", (e, b) -> r.json(e, 200, Map.of("fixa", true)));
        router.registrar("POST", "/api/csrf", (e, b) -> { new SegurancaHttp(() -> base).exigirCsrf(e); r.json(e, 200, Map.of()); });
        router.registrar("GET", "/api/cookie", (e, b) -> {
            var seg = new SegurancaHttp(() -> "https://joinup.example"); seg.limparCookie(e);
            r.json(e, 200, Map.of("token", Optional.ofNullable(SegurancaHttp.token(e)).orElse("")));
        });
        router.iniciar(); server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newFixedThreadPool(4); server.setExecutor(executor); server.createContext("/", router); server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort(); client = HttpClient.newHttpClient();
    }
    @AfterAll static void fechar() { client.close(); server.stop(0); executor.shutdownNow(); }
    private HttpResponse<String> pedir(String metodo, String path, String... headers) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(base + path)).method(metodo, HttpRequest.BodyPublishers.noBody());
        if (headers.length > 0) builder.headers(headers);
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
    @Test void precedenciaEstaticaParametrosE405() throws Exception {
        assertTrue(pedir("GET", "/api/contas/12?tipo=a%20b").body().contains("\"tipo\":\"a b\""));
        assertEquals(200, pedir("POST", "/api/contas/fixa").statusCode());
        var r = pedir("GET", "/api/contas/fixa"); assertEquals(405, r.statusCode()); assertEquals("POST", r.headers().firstValue("Allow").orElseThrow());
        assertEquals(404, pedir("GET", "/api/contas/1/extra").statusCode());
        assertEquals(404, pedir("GET", "/api/contas/").statusCode());
        assertEquals(405, pedir("PATCH", "/api/contas/1").statusCode());
    }
    @ParameterizedTest @ValueSource(strings = {"/api/contas/0", "/api/contas/-1", "/api/contas/01", "/api/contas/2147483648", "/api/contas/a",
            "/api/contas/1?tipo=a&tipo=b", "/api/contas/1?x=%FF", "/api/contas/1?x=%00", "/api/contas/1?=x", "/api/contas/1%2f2", "/api/contas/%2e%2e"})
    void transporteInvalido(String caminho) throws Exception { assertEquals(400, pedir("GET", caminho).statusCode()); }
    @Test void ambiguidadesRecusadas() {
        var r = new HttpResponses(new JsonCodec()); var router = new Router(r, new StaticFileHandler(r), 65536);
        RouteHandler h = (e, b) -> { };
        router.registrar("GET", "/a/{x}/c", h);
        assertThrows(IllegalArgumentException.class, () -> router.registrar("POST", "/a/{y}/c", h));
        assertThrows(IllegalArgumentException.class, () -> router.registrar("GET", "/a/b/{z}", h));
        assertThrows(IllegalArgumentException.class, () -> router.registrar("GET", "/b/{x}/{x}", h));
        assertThrows(IllegalArgumentException.class, () -> router.registrar("GET", "/b/{x", h));
    }
    @Test void csrfOriginRefererEFetchSite() throws Exception {
        assertEquals(200, pedir("POST", "/api/csrf", "Origin", base).statusCode());
        assertEquals(200, pedir("POST", "/api/csrf", "Referer", base + "/perfil").statusCode());
        assertEquals(403, pedir("POST", "/api/csrf").statusCode());
        for (String origin : List.of("null", "https://evil.test", base + ".evil.test", base + "/", "http://127.0.0.1:9"))
            assertEquals(403, pedir("POST", "/api/csrf", "Origin", origin).statusCode());
        assertEquals(403, pedir("POST", "/api/csrf", "Origin", "null", "Referer", base + "/").statusCode());
        assertEquals(403, pedir("POST", "/api/csrf", "Origin", base, "Sec-Fetch-Site", "cross-site").statusCode());
        assertEquals(403, pedir("POST", "/api/csrf", "Referer", base.replace("127.0.0.1", "user@127.0.0.1") + "/").statusCode());
    }
    @Test void cookieSecureERejeicaoDeDuplicata() throws Exception {
        var resposta = pedir("GET", "/api/cookie", "Cookie", "outro=x; joinup_sessao=abc");
        assertTrue(resposta.body().contains("abc"));
        String cookie = resposta.headers().firstValue("Set-Cookie").orElseThrow();
        assertTrue(cookie.contains("Secure")); assertTrue(cookie.contains("HttpOnly")); assertTrue(cookie.contains("SameSite=Lax"));
        assertEquals(401, pedir("GET", "/api/cookie", "Cookie", "joinup_sessao=a; joinup_sessao=b").statusCode());
    }
    @Test void parametrosConcorrentesIsolados() {
        var futures = new ArrayList<CompletableFuture<Void>>();
        for (int i = 1; i <= 30; i++) {
            int id = i;
            futures.add(client.sendAsync(HttpRequest.newBuilder(URI.create(base + "/api/contas/" + i)).build(), HttpResponse.BodyHandlers.ofString())
                    .thenAccept(r -> {
                        assertEquals(200, r.statusCode());
                        var dados = new JsonCodec().ler(r.body().getBytes(java.nio.charset.StandardCharsets.UTF_8), Map.class);
                        assertEquals(id, ((Number) dados.get("id")).intValue());
                    }));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
    }
}
