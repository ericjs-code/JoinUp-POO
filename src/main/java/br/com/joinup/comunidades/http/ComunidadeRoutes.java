package br.com.joinup.comunidades.http;

import br.com.joinup.auth.SegurancaHttp;
import br.com.joinup.comunidades.dto.*;
import br.com.joinup.comunidades.service.ComunidadeSocialService;
import br.com.joinup.http.*;
import br.com.joinup.usuarios.domain.Perfil;
import br.com.joinup.usuarios.service.ContaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.*;

public final class ComunidadeRoutes {
    private final JsonCodec json;
    private final HttpResponses respostas;
    private final SegurancaHttp seguranca;
    private final ContaService contas;
    private final ComunidadeSocialService service;

    public ComunidadeRoutes(JsonCodec json, HttpResponses respostas, SegurancaHttp seguranca, ContaService contas, ComunidadeSocialService service) {
        this.json = json;
        this.respostas = respostas;
        this.seguranca = seguranca;
        this.contas = contas;
        this.service = service;
    }

    public void registrar(Router router) {
        // Descoberta pública (RF-COM-06)
        router.registrar("GET", "/api/comunidades", (e, b, p) -> {
            String busca = p.query().get("busca");
            respostas.json(e, 200, Map.of("dados", service.listar(busca, pagina(p), tamanho(p))));
        });

        router.registrar("GET", "/api/comunidades/{comunidadeId}", (e, b, p) -> {
            respostas.json(e, 200, Map.of("dados", service.detalhe(SegurancaHttp.token(e), id(p, "comunidadeId"))));
        });

        // Participação / Adesão e Saída (RF-COM-01, RF-COM-02, RF-COM-01A)
        router.registrar("POST", "/api/comunidades/{comunidadeId}/participacao", (e, b, p) -> {
            contas.identificar(SegurancaHttp.token(e), Perfil.PARTICIPANTE);
            seguranca.exigirCsrf(e);
            service.entrar(SegurancaHttp.token(e), id(p, "comunidadeId"));
            respostas.json(e, 200, Map.of("dados", Map.of("sucesso", true, "mensagem", "Inscrição na comunidade realizada")));
        });

        router.registrar("DELETE", "/api/comunidades/{comunidadeId}/participacao", (e, b, p) -> {
            contas.identificar(SegurancaHttp.token(e), Perfil.PARTICIPANTE);
            seguranca.exigirCsrf(e);
            service.sair(SegurancaHttp.token(e), id(p, "comunidadeId"));
            respostas.json(e, 200, Map.of("dados", Map.of("sucesso", true, "mensagem", "Saída da comunidade realizada")));
        });

        // Mensagens / Chat da Comunidade (RF-COM-H02, RF-COM-01, RF-COM-05)
        router.registrar("GET", "/api/comunidades/{comunidadeId}/mensagens", (e, b, p) -> {
            respostas.json(e, 200, Map.of("dados", service.listarMensagens(SegurancaHttp.token(e), id(p, "comunidadeId"), pagina(p), tamanho(p))));
        });

        router.registrar("POST", "/api/comunidades/{comunidadeId}/mensagens", (e, b, p) -> {
            contas.identificar(SegurancaHttp.token(e), Perfil.PARTICIPANTE, Perfil.ORGANIZADOR);
            seguranca.exigirCsrf(e);
            SegurancaHttp.exigirJson(e);
            var req = payload(b, EnviarMensagemRequest.class);
            respostas.json(e, 201, Map.of("dados", service.enviarMensagem(SegurancaHttp.token(e), id(p, "comunidadeId"), req)));
        });

        // Feed Social / Postagens (RF-SOC-H01 a H04, RF-SOC-01, RF-COM-01B)
        router.registrar("GET", "/api/comunidades/{comunidadeId}/postagens", (e, b, p) -> {
            respostas.json(e, 200, Map.of("dados", service.listarPostagens(SegurancaHttp.token(e), id(p, "comunidadeId"), pagina(p), tamanho(p))));
        });

        router.registrar("POST", "/api/comunidades/{comunidadeId}/postagens", (e, b, p) -> {
            contas.identificar(SegurancaHttp.token(e), Perfil.PARTICIPANTE);
            seguranca.exigirCsrf(e);
            SegurancaHttp.exigirJson(e);
            var req = payload(b, CriarPostagemRequest.class);
            respostas.json(e, 201, Map.of("dados", service.criarPostagem(SegurancaHttp.token(e), id(p, "comunidadeId"), req)));
        });

        // Detalhe de Postagem
        router.registrar("GET", "/api/postagens/{postagemId}", (e, b, p) -> {
            respostas.json(e, 200, Map.of("dados", service.detalhePostagem(SegurancaHttp.token(e), id(p, "postagemId"))));
        });

        // Curtidas
        router.registrar("PUT", "/api/postagens/{postagemId}/curtida", (e, b, p) -> {
            contas.identificar(SegurancaHttp.token(e), Perfil.PARTICIPANTE);
            seguranca.exigirCsrf(e);
            respostas.json(e, 200, Map.of("dados", service.curtir(SegurancaHttp.token(e), id(p, "postagemId"))));
        });

        router.registrar("DELETE", "/api/postagens/{postagemId}/curtida", (e, b, p) -> {
            contas.identificar(SegurancaHttp.token(e), Perfil.PARTICIPANTE);
            seguranca.exigirCsrf(e);
            respostas.json(e, 200, Map.of("dados", service.descurtir(SegurancaHttp.token(e), id(p, "postagemId"))));
        });

        // Comentários
        router.registrar("GET", "/api/postagens/{postagemId}/comentarios", (e, b, p) -> {
            respostas.json(e, 200, Map.of("dados", service.listarComentarios(SegurancaHttp.token(e), id(p, "postagemId"), pagina(p), tamanho(p))));
        });

        router.registrar("POST", "/api/postagens/{postagemId}/comentarios", (e, b, p) -> {
            contas.identificar(SegurancaHttp.token(e), Perfil.PARTICIPANTE);
            seguranca.exigirCsrf(e);
            SegurancaHttp.exigirJson(e);
            var req = payload(b, CriarComentarioRequest.class);
            respostas.json(e, 201, Map.of("dados", service.adicionarComentario(SegurancaHttp.token(e), id(p, "postagemId"), req)));
        });
    }

    private <T> T payload(byte[] b, Class<T> tipo) {
        JsonNode n = json.ler(b, JsonNode.class);
        if (!n.isObject()) throw invalido();
        if (tipo == EnviarMensagemRequest.class && n.hasNonNull("idRespostaA")) {
            var id = n.get("idRespostaA");
            if (!id.isIntegralNumber() || !id.canConvertToInt() || id.intValue() <= 0) throw invalido();
        }
        return json.ler(b, tipo);
    }

    private static long id(RouteParameters p, String nome) {
        return RouteParameters.id(p.caminho().get(nome));
    }

    private static int pagina(RouteParameters p) { return numero(p, "pagina", 1, 10000); }
    private static int tamanho(RouteParameters p) { return numero(p, "tamanho", 50, 100); }
    private static int numero(RouteParameters p, String nome, int padrao, int maximo) {
        String valor = p.query().get(nome);
        if (valor == null) return padrao;
        if (!valor.matches("[1-9][0-9]{0,4}")) throw invalido();
        int numero = Integer.parseInt(valor);
        if (numero > maximo) throw invalido();
        return numero;
    }
    private static HttpException invalido() {
        return new HttpException(400, "REQUISICAO_INVALIDA", "Payload ou parâmetros inválidos");
    }
}
