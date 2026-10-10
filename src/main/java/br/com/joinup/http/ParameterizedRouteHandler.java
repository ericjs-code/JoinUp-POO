package br.com.joinup.http;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;

@FunctionalInterface
public interface ParameterizedRouteHandler {
    void handle(HttpExchange exchange, byte[] corpo, RouteParameters parametros) throws IOException;
}
