package br.com.joinup.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.*;

/** Correspondência por segmento; precedência estática independente do método. */
public final class Router implements HttpHandler {
    private static final System.Logger LOG = System.getLogger(Router.class.getName());
    private record Rota(String padrao, List<String> segmentos, Map<String, ParameterizedRouteHandler> metodos) { }
    private final List<Rota> rotas = new ArrayList<>();
    private final HttpResponses respostas;
    private final StaticFileHandler estaticos;
    private final int limiteCorpo;
    private boolean iniciado;
    public Router(HttpResponses respostas, StaticFileHandler estaticos, int limiteCorpo) {
        this.respostas = respostas; this.estaticos = estaticos; this.limiteCorpo = limiteCorpo;
    }
    public synchronized void registrar(String metodo, String caminho, RouteHandler handler) {
        registrar(metodo, caminho, (exchange, corpo, parametros) -> handler.handle(exchange, corpo));
    }
    public synchronized void registrar(String metodo, String caminho, ParameterizedRouteHandler handler) {
        if (iniciado) throw new IllegalStateException("Rotas já iniciadas");
        if (!metodo.matches("[A-Z]+") || !caminho.startsWith("/")) throw new IllegalArgumentException("Rota inválida");
        List<String> segmentos = segmentos(caminho);
        var nomes = new HashSet<String>();
        for (String s : segmentos) {
            if (variavel(s)) {
                if (!nomes.add(s)) throw new IllegalArgumentException("Parâmetro repetido");
            } else if (!s.matches("[A-Za-z0-9._-]*") || s.equals(".") || s.equals("..")) throw new IllegalArgumentException("Segmento inválido");
        }
        Rota rota = rotas.stream().filter(r -> r.padrao().equals(caminho)).findFirst().orElse(null);
        if (rota == null) {
            for (Rota existente : rotas) {
                if (ambigua(segmentos, existente.segmentos())) throw new IllegalArgumentException("Padrões de rota ambíguos");
            }
            rota = new Rota(caminho, segmentos, new TreeMap<>()); rotas.add(rota);
            rotas.sort((a, b) -> comparar(a.segmentos(), b.segmentos()));
        }
        if (rota.metodos().putIfAbsent(metodo, handler) != null) throw new IllegalArgumentException("Rota duplicada");
    }
    public synchronized void iniciar() { iniciado = true; }
    private static List<String> segmentos(String caminho) { return List.of(caminho.substring(1).split("/", -1)); }
    private static boolean variavel(String s) { return s.matches("\\{[A-Za-z][A-Za-z0-9]*}"); }
    private static boolean ambigua(List<String> a, List<String> b) {
        if (a.size() != b.size()) return false;
        boolean aMais = false, bMais = false;
        for (int i = 0; i < a.size(); i++) {
            boolean av = variavel(a.get(i)), bv = variavel(b.get(i));
            if (!av && !bv && !a.get(i).equals(b.get(i))) return false;
            if (av && !bv) bMais = true;
            if (!av && bv) aMais = true;
        }
        return aMais == bMais;
    }
    private static int comparar(List<String> a, List<String> b) {
        for (int i = 0; i < Math.min(a.size(), b.size()); i++) {
            if (variavel(a.get(i)) != variavel(b.get(i))) return variavel(a.get(i)) ? 1 : -1;
        }
        return Integer.compare(a.size(), b.size());
    }
    private static Map<String, String> corresponder(Rota rota, List<String> partes) {
        if (rota.segmentos().size() != partes.size()) return null;
        var valores = new HashMap<String, String>();
        for (int i = 0; i < partes.size(); i++) {
            String s = rota.segmentos().get(i), p = partes.get(i);
            if (variavel(s)) {
                if (p.isEmpty()) return null;
                valores.put(s.substring(1, s.length() - 1), p);
            } else if (!s.equals(p)) return null;
        }
        return valores;
    }
    @Override public void handle(HttpExchange exchange) throws IOException {
        try {
            String caminho = RequestPath.validar(exchange.getRequestURI());
            Map<String, String> query = RouteParameters.lerQuery(exchange.getRequestURI());
            byte[] corpo = RequestBody.ler(exchange, limiteCorpo);
            Rota selecionada = null; Map<String, String> parametros = null;
            for (Rota rota : rotas) {
                parametros = corresponder(rota, segmentos(caminho));
                if (parametros != null) { selecionada = rota; break; }
            }
            if (selecionada != null) {
                var handler = selecionada.metodos().get(exchange.getRequestMethod());
                if (handler == null) {
                    exchange.getResponseHeaders().set("Allow", String.join(", ", selecionada.metodos().keySet()));
                    throw new HttpException(405, "METODO_NAO_PERMITIDO", "Método não permitido para esta rota");
                }
                handler.handle(exchange, corpo, new RouteParameters(parametros, query));
            } else if (caminho.equals("/api") || caminho.startsWith("/api/")) {
                throw new HttpException(404, "RECURSO_NAO_ENCONTRADO", "Recurso não encontrado");
            } else estaticos.servir(exchange);
        } catch (HttpException e) {
            respostas.erro(exchange, e.status(), e.codigo(), e.getMessage());
        } catch (Exception e) {
            LOG.log(System.Logger.Level.ERROR, "Falha interna no processamento HTTP");
            if (exchange.getResponseCode() == -1) respostas.erro(exchange, 500, "ERRO_INTERNO", "Não foi possível processar a requisição");
        } finally { exchange.close(); }
    }
}
