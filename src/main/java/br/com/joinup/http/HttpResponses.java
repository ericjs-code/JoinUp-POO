package br.com.joinup.http;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.Map;

public final class HttpResponses {
    private final JsonCodec json;

    public HttpResponses(JsonCodec json) { this.json = json; }

    public void json(HttpExchange exchange, int status, Object valor) throws IOException {
        bytes(exchange, status, "application/json; charset=utf-8", json.escrever(valor));
    }

    public void erro(HttpExchange exchange, int status, String codigo, String mensagem) throws IOException {
        json(exchange, status, Map.of("erro", Map.of(
                "codigo", codigo, "mensagem", mensagem, "detalhes", Map.of())));
    }

    public void bytes(HttpExchange exchange, int status, String tipo, byte[] corpo) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", tipo);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("Content-Security-Policy",
                "default-src 'self'; img-src 'self' blob:; script-src 'self'; style-src 'self'; object-src 'none'; frame-ancestors 'none'; base-uri 'none'");
        exchange.getResponseHeaders().set("Referrer-Policy", "no-referrer");
        if ("HEAD".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Content-Length", Integer.toString(corpo.length));
            exchange.sendResponseHeaders(status, -1);
        } else if (status == 204) {
            exchange.sendResponseHeaders(status, -1);
        } else {
            exchange.sendResponseHeaders(status, corpo.length);
            exchange.getResponseBody().write(corpo);
        }
    }
}
