package br.com.joinup.http;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;

@FunctionalInterface
public interface RouteHandler {
    void handle(HttpExchange exchange, byte[] corpo) throws IOException;
}
