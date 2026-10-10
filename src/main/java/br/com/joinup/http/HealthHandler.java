package br.com.joinup.http;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.Map;

/** Liveness do HTTP; não afirma conectividade ou prontidão do banco. */
public final class HealthHandler implements RouteHandler {
    private final HttpResponses respostas;

    public HealthHandler(HttpResponses respostas) { this.respostas = respostas; }

    @Override public void handle(HttpExchange exchange, byte[] corpo) throws IOException {
        respostas.json(exchange, 200, Map.of("dados", Map.of("status", "UP")));
    }
}
