package br.com.joinup.http;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.Map;

/** Expõe apenas arquivos públicos do classpath, inclusive dentro do JAR. */
public final class StaticFileHandler {
    private static final Map<String, String> TIPOS = Map.of(
            "html", "text/html; charset=utf-8", "css", "text/css; charset=utf-8",
            "js", "text/javascript; charset=utf-8", "png", "image/png",
            "jpg", "image/jpeg", "ico", "image/x-icon", "svg", "image/svg+xml");
    private final HttpResponses respostas;

    public StaticFileHandler(HttpResponses respostas) { this.respostas = respostas; }

    public void servir(HttpExchange exchange) throws IOException {
        String caminho = RequestPath.validar(exchange.getRequestURI());
        String arquivo = caminho.equals("/") ? "/index.html" : caminho;
        int ponto = arquivo.lastIndexOf('.');
        String tipo = ponto < 0 ? null : TIPOS.get(arquivo.substring(ponto + 1));
        if (tipo == null) throw naoEncontrado();
        try (var recurso = StaticFileHandler.class.getResourceAsStream("/public" + arquivo)) {
            if (recurso == null) throw naoEncontrado();
            if (!exchange.getRequestMethod().equals("GET") && !exchange.getRequestMethod().equals("HEAD")) {
                exchange.getResponseHeaders().set("Allow", "GET, HEAD");
                throw new HttpException(405, "METODO_NAO_PERMITIDO", "Método não permitido para este recurso");
            }
            respostas.bytes(exchange, 200, tipo, recurso.readAllBytes());
        }
    }

    private static HttpException naoEncontrado() {
        return new HttpException(404, "RECURSO_NAO_ENCONTRADO", "Recurso não encontrado");
    }
}
