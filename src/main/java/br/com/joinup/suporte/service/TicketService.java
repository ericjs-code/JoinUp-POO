package br.com.joinup.suporte.service;

import br.com.joinup.auth.GerenciadorSessao;
import br.com.joinup.http.HttpException;
import br.com.joinup.shared.db.ExecutorTransacional;
import br.com.joinup.suporte.domain.*;
import br.com.joinup.suporte.dto.*;
import br.com.joinup.suporte.repository.TicketRepositoryJdbc;
import br.com.joinup.usuarios.domain.*;
import java.sql.*;
import java.util.Objects;

public final class TicketService {
    private final ExecutorTransacional transacoes;
    private final GerenciadorSessao sessoes;
    private final TicketRepositoryJdbc repo;
    public TicketService(ExecutorTransacional transacoes, GerenciadorSessao sessoes, TicketRepositoryJdbc repo) {
        this.transacoes = transacoes; this.sessoes = sessoes; this.repo = repo;
    }
    public Ticket criar(String token, AbrirTicketDados dados) {
        if (dados == null) throw invalido();
        String titulo = texto(dados.titulo(), 100), descricao = texto(dados.descricao(), 500);
        return transacoes.executar(c -> {
            var u = sessoes.exigir(c, token, Perfil.PARTICIPANTE, Perfil.ORGANIZADOR);
            long id = repo.criar(c, u, titulo, descricao);
            repo.mensagem(c, id, u, descricao);
            return repo.buscar(c, id, false);
        });
    }
    public PaginaTickets listar(String token, boolean suporte, StatusTicket status, int pagina, int tamanho) {
        validarPagina(pagina, tamanho);
        return transacoes.executar(c -> repo.listar(c, exigir(c, token, suporte), status, pagina, tamanho));
    }
    public TicketDetalhe detalhe(String token, boolean suporte, long id, int pagina, int tamanho) {
        validarId(id); validarPagina(pagina, tamanho);
        return transacoes.executar(c -> {
            var u = exigir(c, token, suporte);
            // Também serializa detalhe/histórico com mutações para não devolver estado e mensagens divergentes.
            return repo.detalhe(c, autorizado(c, id, u), pagina, tamanho);
        });
    }
    public Ticket associar(String token, long id) {
        validarId(id);
        return transacoes.executar(c -> {
            var u = exigir(c, token, true);
            autorizado(c, id, u);
            // PHP: autoassociação, inclusive fechado, sem transição nem alteração de autoria.
            repo.associar(c, id, u.id());
            return repo.buscar(c, id, false);
        });
    }
    public Ticket responder(String token, long id, MensagemTicketDados dados) {
        String mensagem = texto(dados == null ? null : dados.mensagem(), 500);
        return alterar(token, true, id, "responder", mensagem);
    }
    public Ticket retornar(String token, long id, MensagemTicketDados dados) {
        String mensagem = texto(dados == null ? null : dados.mensagem(), 500);
        return alterar(token, false, id, "retornar", mensagem);
    }
    public Ticket resolver(String token, long id) { return alterar(token, false, id, "resolver", null); }
    public Ticket encerrar(String token, long id) { return alterar(token, true, id, "encerrar", null); }
    private Ticket alterar(String token, boolean suporte, long id, String acao, String mensagem) {
        validarId(id);
        return transacoes.executar(c -> {
            var u = exigir(c, token, suporte); // Ordem: conta -> sessão -> ticket; não bloquear conta do solicitante por suporte.
            var ticket = autorizado(c, id, u);
            StatusTicket destino = switch (acao) {
                case "responder" -> ticket.status().responder();
                case "retornar" -> ticket.status().retornar();
                case "resolver" -> ticket.status().resolver();
                case "encerrar" -> ticket.status().encerrar();
                default -> throw new IllegalArgumentException("Ação interna inválida");
            };
            // Status e mensagem juntos. Falha na mensagem desfaz também a transição.
            repo.transicionar(c, id, destino, mensagem, suporte);
            if (mensagem != null) repo.mensagem(c, id, u, mensagem);
            return repo.buscar(c, id, false);
        });
    }
    private Usuario exigir(Connection c, String token, boolean suporte) throws SQLException {
        return suporte ? sessoes.exigir(c, token, Perfil.SUPORTE) : sessoes.exigir(c, token, Perfil.PARTICIPANTE, Perfil.ORGANIZADOR);
    }
    private Ticket autorizado(Connection c, long id, Usuario u) throws SQLException {
        var t = repo.buscar(c, id, true);
        if (t == null || (u.perfil() != Perfil.SUPORTE && !(u.perfil() == Perfil.PARTICIPANTE
                ? Objects.equals(t.participanteId(), u.id()) : Objects.equals(t.organizadorId(), u.id()))))
            throw new HttpException(404, "RECURSO_NAO_ENCONTRADO", "Chamado não encontrado");
        return t;
    }
    public static String texto(String s, int max) {
        if (s == null || s.isBlank()) throw invalido();
        s = s.strip();
        if (s.codePointCount(0, s.length()) > max || s.codePoints().anyMatch(ch -> Character.isISOControl(ch) && ch != '\n' && ch != '\r' && ch != '\t')) throw invalido();
        return s;
    }
    public static void validarPagina(int pagina, int tamanho) {
        if (pagina < 1 || pagina > 10000 || tamanho < 1 || tamanho > 100) throw invalido();
    }
    private static void validarId(long id) { if (id < 1 || id > Integer.MAX_VALUE) throw invalido(); }
    private static HttpException invalido() { return new HttpException(400, "REQUISICAO_INVALIDA", "Verifique os campos e limites do chamado"); }
}
