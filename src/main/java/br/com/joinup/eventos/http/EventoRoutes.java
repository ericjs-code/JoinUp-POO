package br.com.joinup.eventos.http;

import br.com.joinup.auth.SegurancaHttp;
import br.com.joinup.eventos.dto.*;
import br.com.joinup.eventos.facade.EventoFacade;
import br.com.joinup.http.*;
import br.com.joinup.usuarios.domain.Perfil;
import br.com.joinup.usuarios.service.ContaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import java.util.*;

public final class EventoRoutes {
    private final JsonCodec json;
    private final HttpResponses respostas;
    private final SegurancaHttp seguranca;
    private final ContaService contas;
    private final EventoFacade facade;
    public EventoRoutes(JsonCodec j,HttpResponses r,SegurancaHttp s,ContaService c,EventoFacade f) { json=j; respostas=r; seguranca=s; contas=c; facade=f; }
    public void registrar(Router router) {
        router.registrar("GET","/api/eventos",(e,b,p) -> respostas.json(e,200,facade.explorar(FiltroEventos.de(p.query()))));
        router.registrar("GET","/api/eventos/{eventoId}",(e,b,p) -> respostas.json(e,200,Map.of("dados",facade.consultar(id(p,"eventoId")))));
        router.registrar("GET","/api/eventos/{eventoId}/tipos-ingresso",(e,b,p) -> respostas.json(e,200,Map.of("dados",facade.tiposIngresso(id(p,"eventoId")))));
        router.registrar("GET","/api/generos-musicais",(e,b) -> respostas.json(e,200,Map.of("dados",facade.generos())));
        router.registrar("GET","/api/artistas",(e,b,p) -> {
            if(!Set.of("busca").containsAll(p.query().keySet())) throw invalido();
            respostas.json(e,200,Map.of("dados",facade.artistas(p.query().getOrDefault("busca",""))));
        });
        router.registrar("GET","/api/organizador/eventos",(e,b,p) -> respostas.json(e,200,facade.meusEventos(SegurancaHttp.token(e),FiltroEventos.de(p.query()))));
        router.registrar("POST","/api/organizador/eventos",(e,b) -> {
            exigir(e); respostas.json(e,201,Map.of("dados",facade.criarEvento(SegurancaHttp.token(e),payload(b,CriarEventoDados.class,false))));
        });
        router.registrar("PATCH","/api/organizador/eventos/{eventoId}",(e,b,p) -> {
            exigir(e); respostas.json(e,200,Map.of("dados",facade.editarEvento(SegurancaHttp.token(e),id(p,"eventoId"),payload(b,EditarEventoDados.class,true))));
        });
        router.registrar("POST","/api/organizador/eventos/{eventoId}/tipos-ingresso",(e,b,p) -> {
            exigir(e); respostas.json(e,201,Map.of("dados",facade.adicionarTipo(SegurancaHttp.token(e),id(p,"eventoId"),payload(b,TipoIngressoDados.class,false))));
        });
        router.registrar("PATCH","/api/organizador/eventos/{eventoId}/tipos-ingresso/{tipoId}",(e,b,p) -> {
            exigir(e); respostas.json(e,200,Map.of("dados",facade.editarTipo(SegurancaHttp.token(e),id(p,"eventoId"),id(p,"tipoId"),payload(b,TipoIngressoDados.class,true))));
        });
    }
    private void exigir(HttpExchange e) {
        contas.identificar(SegurancaHttp.token(e),Perfil.ORGANIZADOR);
        seguranca.exigirCsrf(e); SegurancaHttp.exigirJson(e);
    }
    private <T> T payload(byte[] b,Class<T> tipo,boolean patch) {
        JsonNode n=json.ler(b,JsonNode.class); if(!n.isObject()) throw invalido();
        validarNumeros(n);
        if(patch) n.properties().forEach(e -> { if(e.getValue().isNull()) throw invalido(); });
        return json.ler(b,tipo);
    }
    private void validarNumeros(JsonNode n) {
        if(n.isObject()) n.properties().forEach(e -> {
            JsonNode v=e.getValue();
            if(Set.of("generoId","quantidade").contains(e.getKey()) && !v.isIntegralNumber()) throw invalido();
            if(e.getKey().equals("preco") && !v.isNumber()) throw invalido();
            if(e.getKey().equals("artistaIds")) {
                if(!v.isArray()) throw invalido(); for(JsonNode id:v) if(!id.isIntegralNumber()) throw invalido();
            }
            validarNumeros(v);
        });
        else if(n.isArray()) for(JsonNode v:n) validarNumeros(v);
    }
    private static long id(RouteParameters p,String nome) { return RouteParameters.id(p.caminho().get(nome)); }
    private static HttpException invalido() { return new HttpException(400,"REQUISICAO_INVALIDA","Payload ou parâmetros inválidos"); }
}
