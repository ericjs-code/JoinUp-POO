package br.com.joinup.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/** Rotas registradas na composição; sem dependência de persistência. */
public final class Router implements HttpHandler {
    private static final System.Logger LOG = System.getLogger(Router.class.getName());
    private final Map<String, Map<String, RouteHandler>> rotas = new HashMap<>();
    private final HttpResponses respostas;
    private final StaticFileHandler estaticos;
    private final int limiteCorpo;
    private boolean iniciado;

    public Router(HttpResponses respostas, StaticFileHandler estaticos, int limiteCorpo) {
        this.respostas = respostas;
        this.estaticos = estaticos;
        this.limiteCorpo = limiteCorpo;
    }

    public synchronized void registrar(String metodo, String caminho, RouteHandler handler) {
        if (iniciado) throw new IllegalStateException("Rotas já iniciadas");
        var metodos = rotas.computeIfAbsent(caminho, ignored -> new TreeMap<>());
        if (metodos.putIfAbsent(metodo, handler) != null) throw new IllegalArgumentException("Rota duplicada");
    }

    public synchronized void iniciar() { iniciado = true; }

    @Override public void handle(HttpExchange exchange) throws IOException {
        try {
            String caminho = RequestPath.validar(exchange.getRequestURI());
            // O corpo pertence à requisição; nunca é guardado nos atributos compartilhados do contexto.
            byte[] corpo = RequestBody.ler(exchange, limiteCorpo);
            Map<String, RouteHandler> metodos = rotas.get(caminho);
            if (metodos != null) {
                RouteHandler handler = metodos.get(exchange.getRequestMethod());
                if (handler == null) {
                    exchange.getResponseHeaders().set("Allow", String.join(", ", metodos.keySet()));
                    throw new HttpException(405, "METODO_NAO_PERMITIDO", "Método não permitido para esta rota");
                }
                handler.handle(exchange, corpo);
            } else if (caminho.equals("/api") || caminho.startsWith("/api/")) {
                throw new HttpException(404, "RECURSO_NAO_ENCONTRADO", "Recurso não encontrado");
            } else {
                estaticos.servir(exchange);
            }
        } catch (HttpException e) {
            respostas.erro(exchange, e.status(), e.codigo(), e.getMessage());
        } catch (Exception e) {
            // Não registrar payload, URL JDBC, credenciais ou mensagens internas de exceções.
            LOG.log(System.Logger.Level.ERROR, "Falha interna no processamento HTTP");
            if (exchange.getResponseCode() == -1) {
                respostas.erro(exchange, 500, "ERRO_INTERNO", "Não foi possível processar a requisição");
            }
        } finally {
            exchange.close();
        }
    }
}
