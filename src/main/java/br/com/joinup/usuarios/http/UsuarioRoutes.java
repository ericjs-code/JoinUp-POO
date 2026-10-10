package br.com.joinup.usuarios.http;

import br.com.joinup.auth.*;
import br.com.joinup.http.*;
import br.com.joinup.usuarios.domain.*;
import br.com.joinup.usuarios.dto.*;
import br.com.joinup.usuarios.service.*;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/** Adaptação HTTP; SQL e transações ficam nos colaboradores de caso de uso. */
public final class UsuarioRoutes {
    private final JsonCodec json;
    private final HttpResponses respostas;
    private final SegurancaHttp seguranca;
    private final ContaService contas;
    public UsuarioRoutes(JsonCodec json, HttpResponses respostas, SegurancaHttp seguranca, ContaService contas) {
        this.json = json; this.respostas = respostas; this.seguranca = seguranca; this.contas = contas;
    }
    public void registrar(Router router, CadastroParticipanteCreator participante, CadastroOrganizadorCreator organizador,
                          CadastroSuporteCreator suporte, AutenticacaoParticipante loginP, AutenticacaoOrganizador loginO, AutenticacaoSuporte loginS) {
        cadastro(router, "/api/participantes", participante, false);
        cadastro(router, "/api/organizadores", organizador, false);
        cadastro(router, "/api/suporte/usuarios", suporte, true);
        login(router, "/api/auth/participantes/login", loginP);
        login(router, "/api/auth/organizadores/login", loginO);
        login(router, "/api/auth/suporte/login", loginS);
        router.registrar("GET", "/api/auth/me", (e, corpo) -> responderUsuario(e, 200, contas.identificar(SegurancaHttp.token(e))));
        router.registrar("GET", "/api/me/perfil", (e, corpo) -> responderUsuario(e, 200,
                contas.identificar(SegurancaHttp.token(e), Perfil.PARTICIPANTE, Perfil.ORGANIZADOR)));
        router.registrar("PATCH", "/api/me/perfil", (e, corpo) -> {
            exigir(e, Perfil.PARTICIPANTE, Perfil.ORGANIZADOR);
            SegurancaHttp.exigirJson(e);
            responderUsuario(e, 200, contas.atualizar(SegurancaHttp.token(e), json.ler(corpo, PerfilDados.class)));
        });
        router.registrar("POST", "/api/me/desativacao", (e, corpo) -> {
            exigir(e, Perfil.PARTICIPANTE, Perfil.ORGANIZADOR); exigirVazio(corpo);
            contas.desativar(SegurancaHttp.token(e)); seguranca.limparCookie(e);
            respostas.json(e, 200, Map.of("dados", Map.of("ativo", false)));
        });
        router.registrar("POST", "/api/auth/logout", (e, corpo) -> {
            exigir(e); exigirVazio(corpo); contas.logout(SegurancaHttp.token(e)); seguranca.limparCookie(e);
            respostas.bytes(e, 204, "application/json; charset=utf-8", new byte[0]);
        });
        router.registrar("GET", "/api/suporte/contas", (e, corpo, p) -> {
            contas.identificar(SegurancaHttp.token(e), Perfil.SUPORTE);
            if (!Set.of("tipo", "situacao", "pagina", "tamanho").containsAll(p.query().keySet()) || !"inativa".equals(p.query().get("situacao")))
                throw new HttpException(400, "REQUISICAO_INVALIDA", "Informe tipo e situacao=inativa");
            respostas.json(e, 200, contas.listarInativas(SegurancaHttp.token(e), tipoConta(p.query().get("tipo")),
                    inteiroQuery(p.query().getOrDefault("pagina", "1"), 100_000), inteiroQuery(p.query().getOrDefault("tamanho", "20"), 100)));
        });
        router.registrar("POST", "/api/suporte/contas/{tipo}/{id}/reativacao", (e, corpo, p) -> {
            exigir(e, Perfil.SUPORTE); exigirVazio(corpo);
            Usuario u = contas.reativar(SegurancaHttp.token(e), tipoConta(p.caminho().get("tipo")), RouteParameters.id(p.caminho().get("id")));
            // Metadados mínimos também na resposta de reativação.
            respostas.json(e, 200, Map.of("dados", Map.of("id", u.id(), "perfil", u.perfil(), "ativo", u.ativo())));
        });
    }
    private void cadastro(Router router, String caminho, CadastroUsuarioCreator creator, boolean restrito) {
        router.registrar("POST", caminho, (e, corpo) -> {
            if (restrito) exigir(e, Perfil.SUPORTE); else seguranca.exigirCsrf(e);
            SegurancaHttp.exigirJson(e);
            responderUsuario(e, 201, creator.cadastrar(json.ler(corpo, CadastroDados.class), restrito ? SegurancaHttp.token(e) : null));
        });
    }
    private void login(Router router, String caminho, AutenticacaoTemplate autenticacao) {
        router.registrar("POST", caminho, (e, corpo) -> {
            seguranca.exigirCsrf(e); SegurancaHttp.exigirJson(e);
            SessaoEmitida sessao = autenticacao.autenticar(json.ler(corpo, Credenciais.class));
            seguranca.emitirCookie(e, sessao);
            respostas.json(e, 200, Map.of("dados", Map.of("id", sessao.usuario().id(), "perfil", sessao.usuario().perfil(), "nome", sessao.usuario().nome())));
        });
    }
    private void exigir(HttpExchange e, Perfil... perfis) {
        // 401/403 precedem CSRF; a operação revalida sessão sob a mesma transação da mutação.
        contas.identificar(SegurancaHttp.token(e), perfis); seguranca.exigirCsrf(e);
    }
    private void responderUsuario(HttpExchange e, int status, Usuario usuario) throws IOException { respostas.json(e, status, Map.of("dados", UsuarioView.de(usuario))); }
    private void exigirVazio(byte[] corpo) {
        if (corpo.length != 0 && !json.ler(corpo, Map.class).isEmpty()) throw new HttpException(400, "REQUISICAO_INVALIDA", "Esta ação não aceita campos");
    }
    private static Perfil tipoConta(String tipo) {
        if ("participante".equals(tipo)) return Perfil.PARTICIPANTE;
        if ("organizador".equals(tipo)) return Perfil.ORGANIZADOR;
        throw new HttpException(400, "REQUISICAO_INVALIDA", "Tipo deve ser participante ou organizador");
    }
    private static int inteiroQuery(String valor, int maximo) {
        long n = RouteParameters.id(valor);
        if (n > maximo) throw new HttpException(400, "REQUISICAO_INVALIDA", "Paginação inválida");
        return (int) n;
    }
}
