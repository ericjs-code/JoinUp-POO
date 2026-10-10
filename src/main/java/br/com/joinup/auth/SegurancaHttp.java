package br.com.joinup.auth;

import br.com.joinup.http.HttpException;
import com.sun.net.httpserver.HttpExchange;
import java.net.URI;
import java.util.function.Supplier;

/** Origin fixo de configuração, nunca derivado de Host/X-Forwarded-* recebidos. */
public final class SegurancaHttp {
    public static final String COOKIE = "joinup_sessao";
    private final Supplier<String> origem;
    public SegurancaHttp(Supplier<String> origem) { this.origem = origem; }
    public void exigirCsrf(HttpExchange exchange) {
        var headers = exchange.getRequestHeaders();
        String fetchSite = headers.getFirst("Sec-Fetch-Site");
        if (fetchSite != null && !fetchSite.equals("same-origin")) throw csrf();
        var origins = headers.get("Origin");
        if (origins != null) {
            if (origins.size() != 1 || !origem.get().equals(origins.getFirst())) throw csrf();
        } else {
            var referers = headers.get("Referer");
            if (referers == null || referers.size() != 1) throw csrf();
            try {
                URI uri = URI.create(referers.getFirst());
                if (uri.getUserInfo() != null || !origin(uri).equals(origem.get())) throw csrf();
            } catch (IllegalArgumentException e) { throw csrf(); }
        }
    }
    public static String origin(URI uri) {
        if (uri.getHost() == null || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) throw new IllegalArgumentException("Origem inválida");
        int porta = uri.getPort();
        boolean padrao = porta == -1 || (uri.getScheme().equals("https") && porta == 443) || (uri.getScheme().equals("http") && porta == 80);
        return uri.getScheme() + "://" + uri.getHost() + (padrao ? "" : ":" + porta);
    }
    public static String token(HttpExchange exchange) {
        String token = null;
        var cookies = exchange.getRequestHeaders().get("Cookie");
        if (cookies == null) return null;
        for (String linha : cookies) for (String par : linha.split(";")) {
            String[] partes = par.strip().split("=", 2);
            if (partes[0].equals(COOKIE)) {
                if (token != null || partes.length != 2) throw GerenciadorSessao.naoAutenticado();
                token = partes[1];
            }
        }
        return token;
    }
    public void emitirCookie(HttpExchange exchange, SessaoEmitida sessao) {
        cookie(exchange, sessao.token(), sessao.duracaoSegundos());
    }
    public void limparCookie(HttpExchange exchange) { cookie(exchange, "", 0); }
    private void cookie(HttpExchange exchange, String token, long segundos) {
        exchange.getResponseHeaders().add("Set-Cookie", COOKIE + "=" + token + "; Path=/; HttpOnly; SameSite=Lax; Max-Age=" + segundos
                + (origem.get().startsWith("https://") ? "; Secure" : ""));
    }
    public static void exigirJson(HttpExchange exchange) {
        String tipo = exchange.getRequestHeaders().getFirst("Content-Type");
        if (tipo == null || !tipo.split(";", 2)[0].strip().equalsIgnoreCase("application/json"))
            throw new HttpException(415, "TIPO_NAO_SUPORTADO", "Envie application/json");
    }
    private static HttpException csrf() { return new HttpException(403, "CSRF_NEGADO", "Origem da requisição não autorizada"); }
}
