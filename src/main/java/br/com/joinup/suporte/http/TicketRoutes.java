package br.com.joinup.suporte.http;

import br.com.joinup.auth.SegurancaHttp;
import br.com.joinup.http.*;
import br.com.joinup.suporte.domain.StatusTicket;
import br.com.joinup.suporte.dto.*;
import br.com.joinup.suporte.service.TicketService;
import br.com.joinup.usuarios.domain.Perfil;
import br.com.joinup.usuarios.service.ContaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import java.util.Map;

public final class TicketRoutes {
    private final JsonCodec json;
    private final HttpResponses respostas;
    private final SegurancaHttp seguranca;
    private final ContaService contas;
    private final TicketService service;
    public TicketRoutes(JsonCodec json, HttpResponses respostas, SegurancaHttp seguranca, ContaService contas, TicketService service) {
        this.json = json; this.respostas = respostas; this.seguranca = seguranca; this.contas = contas; this.service = service;
    }
    public void registrar(Router router) {
        router.registrar("POST", "/api/chamados", (e,b,p) -> {
            proteger(e, false); SegurancaHttp.exigirJson(e);
            respostas.json(e, 201, Map.of("dados", service.criar(SegurancaHttp.token(e), json.ler(b, AbrirTicketDados.class))));
        });
        for (boolean suporte : new boolean[]{false, true}) {
            String raiz = suporte ? "/api/suporte/chamados" : "/api/me/chamados";
            router.registrar("GET", raiz, (e,b,p) -> respostas.json(e, 200,
                    service.listar(SegurancaHttp.token(e), suporte, status(p), numero(p,"pagina",1), numero(p,"tamanho",20))));
            router.registrar("GET", raiz + "/{ticketId}", (e,b,p) -> respostas.json(e, 200, Map.of("dados",
                    service.detalhe(SegurancaHttp.token(e), suporte, id(p), numero(p,"pagina",1), numero(p,"tamanho",50)))));
            router.registrar("POST", raiz + "/{ticketId}/mensagens", (e,b,p) -> {
                proteger(e, suporte); SegurancaHttp.exigirJson(e);
                var dados = json.ler(b, MensagemTicketDados.class);
                respostas.json(e, 200, Map.of("dados", suporte ? service.responder(SegurancaHttp.token(e), id(p), dados)
                        : service.retornar(SegurancaHttp.token(e), id(p), dados)));
            });
            router.registrar("POST", raiz + "/{ticketId}/" + (suporte ? "encerramento" : "resolucao"), (e,b,p) -> {
                proteger(e, suporte); vazio(e,b);
                respostas.json(e, 200, Map.of("dados", suporte ? service.encerrar(SegurancaHttp.token(e), id(p)) : service.resolver(SegurancaHttp.token(e), id(p))));
            });
        }
        router.registrar("PATCH", "/api/suporte/chamados/{ticketId}/associacao", (e,b,p) -> {
            proteger(e, true); vazio(e,b);
            respostas.json(e, 200, Map.of("dados", service.associar(SegurancaHttp.token(e), id(p))));
        });
    }
    private void proteger(HttpExchange e, boolean suporte) {
        if (suporte) contas.identificar(SegurancaHttp.token(e), Perfil.SUPORTE);
        else contas.identificar(SegurancaHttp.token(e), Perfil.PARTICIPANTE, Perfil.ORGANIZADOR);
        seguranca.exigirCsrf(e);
    }
    private void vazio(HttpExchange e, byte[] b) {
        if (b.length == 0) return;
        SegurancaHttp.exigirJson(e);
        JsonNode n = json.ler(b, JsonNode.class);
        if (!n.isObject() || !n.isEmpty()) throw invalido();
    }
    private static long id(RouteParameters p) { return RouteParameters.id(p.caminho().get("ticketId")); }
    private static StatusTicket status(RouteParameters p) {
        String s = p.query().get("status");
        if (s == null || s.isEmpty()) return null;
        try { return StatusTicket.valueOf(s); } catch (IllegalArgumentException ex) { throw invalido(); }
    }
    private static int numero(RouteParameters p, String chave, int padrao) {
        String n = p.query().get(chave);
        if (n == null) return padrao;
        try { if (!n.matches("[1-9][0-9]{0,4}")) throw invalido(); return Integer.parseInt(n); }
        catch (NumberFormatException ex) { throw invalido(); }
    }
    private static HttpException invalido() { return new HttpException(400,"REQUISICAO_INVALIDA","Parâmetros do chamado inválidos"); }
}
