package br.com.joinup.compras.http;

import br.com.joinup.auth.SegurancaHttp;
import br.com.joinup.compras.dto.CompraDados;
import br.com.joinup.compras.service.CompraService;
import br.com.joinup.http.*;
import br.com.joinup.revendas.dto.*;
import br.com.joinup.revendas.service.RevendaService;
import br.com.joinup.usuarios.domain.Perfil;
import br.com.joinup.usuarios.service.ContaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import java.util.*;

/** Transporte e autorização HTTP; SQL e regras de negócio ficam nos módulos. */
public final class ComercioRoutes {
    private final JsonCodec json;
    private final HttpResponses respostas;
    private final SegurancaHttp seguranca;
    private final ContaService contas;
    private final CompraService compras;
    private final RevendaService revendas;
    public ComercioRoutes(JsonCodec j,HttpResponses r,SegurancaHttp s,ContaService c,CompraService compras,RevendaService revendas) {
        json=j;respostas=r;seguranca=s;contas=c;this.compras=compras;this.revendas=revendas;
    }
    public void registrar(Router router) {
        router.registrar("POST","/api/compras",(e,b,p) -> { exigir(e); semQuery(p); responder(e,201,compras.comprar(SegurancaHttp.token(e),payload(b,CompraDados.class))); });
        router.registrar("GET","/api/me/compras",(e,b,p) -> { semQuery(p); respostas.json(e,200,compras.historico(SegurancaHttp.token(e))); });
        router.registrar("GET","/api/me/ingressos",(e,b,p) -> { semQuery(p); responder(e,200,compras.ingressos(SegurancaHttp.token(e))); });
        router.registrar("GET","/api/me/ingressos/{ingressoId}",(e,b,p) -> { semQuery(p); responder(e,200,compras.ingresso(SegurancaHttp.token(e),id(p,"ingressoId"))); });
        router.registrar("GET","/api/revendas",(e,b,p) -> respostas.json(e,200,revendas.listar(SegurancaHttp.token(e),FiltroRevendas.de(p.query()))));
        router.registrar("GET","/api/revendas/{anuncioId}",(e,b,p) -> { semQuery(p); responder(e,200,revendas.detalhe(SegurancaHttp.token(e),id(p,"anuncioId"))); });
        router.registrar("GET","/api/me/revendas",(e,b,p) -> { semQuery(p); responder(e,200,revendas.meus(SegurancaHttp.token(e))); });
        router.registrar("POST","/api/revendas",(e,b,p) -> { exigir(e); semQuery(p); responder(e,201,revendas.anunciar(SegurancaHttp.token(e),payload(b,AnuncioDados.class))); });
        router.registrar("POST","/api/revendas/{anuncioId}/cancelamento",(e,b,p) -> { exigir(e); semQuery(p); vazio(b); responder(e,200,revendas.cancelar(SegurancaHttp.token(e),id(p,"anuncioId"))); });
        router.registrar("POST","/api/revendas/{anuncioId}/compra",(e,b,p) -> { exigir(e); semQuery(p); vazio(b); responder(e,200,revendas.comprar(SegurancaHttp.token(e),id(p,"anuncioId"))); });
    }
    private void exigir(HttpExchange e) { contas.identificar(SegurancaHttp.token(e),Perfil.PARTICIPANTE); seguranca.exigirCsrf(e); SegurancaHttp.exigirJson(e); }
    private void responder(HttpExchange e,int status,Object dados) throws java.io.IOException { respostas.json(e,status,Map.of("dados",dados)); }
    private <T> T payload(byte[] body,Class<T> tipo) {
        JsonNode n=json.ler(body,JsonNode.class); if(!n.isObject()) throw invalido(); numeros(n); return json.ler(body,tipo);
    }
    private void numeros(JsonNode n) {
        if(n.isObject()) n.properties().forEach(p -> {
            var v=p.getValue(); if(v.isNull()) throw invalido();
            if(Set.of("eventoId","tipoIngressoId","quantidade").contains(p.getKey()) && (!v.isIntegralNumber() || !v.canConvertToInt())) throw invalido();
            if(p.getKey().equals("precoUnitario") && !v.isNumber()) throw invalido();
            if(p.getKey().equals("ingressoIds")) { if(!v.isArray()) throw invalido(); for(var id:v) if(!id.isIntegralNumber() || !id.canConvertToInt()) throw invalido(); }
            numeros(v);
        }); else if(n.isArray()) for(var v:n) { if(v.isNull()) throw invalido(); numeros(v); }
    }
    private void vazio(byte[] body) { JsonNode n=json.ler(body,JsonNode.class); if(!n.isObject() || !n.isEmpty()) throw invalido(); }
    private static void semQuery(RouteParameters p) { if(!p.query().isEmpty()) throw invalido(); }
    private static long id(RouteParameters p,String nome) { return RouteParameters.id(p.caminho().get(nome)); }
    private static HttpException invalido() { return new HttpException(400,"REQUISICAO_INVALIDA","Payload ou parâmetros inválidos"); }
}
