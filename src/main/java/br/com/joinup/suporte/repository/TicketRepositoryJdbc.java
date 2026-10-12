package br.com.joinup.suporte.repository;

import br.com.joinup.suporte.domain.*;
import br.com.joinup.suporte.dto.*;
import br.com.joinup.usuarios.domain.*;
import br.com.joinup.compras.repository.JdbcComercio;
import java.sql.*;
import java.util.*;

/** SQL explícito, sempre na Connection da unidade de trabalho. */
public final class TicketRepositoryJdbc {
    public long criar(Connection c, Usuario u, String titulo, String descricao) throws SQLException {
        return JdbcComercio.inserir(c, """
            INSERT INTO ticket(titulo,descricao,status_ticket,id_participante,id_organizador,data_criacao,data_atualizacao)
            VALUES(?,?,'aberto',?,?,UTC_TIMESTAMP(),UTC_TIMESTAMP())
            """, titulo, descricao, u.perfil() == Perfil.PARTICIPANTE ? u.id() : null,
                u.perfil() == Perfil.ORGANIZADOR ? u.id() : null);
    }
    public Ticket buscar(Connection c, long id, boolean bloquear) throws SQLException {
        try (var p = c.prepareStatement("SELECT * FROM ticket WHERE id_ticket=?" + (bloquear ? " FOR UPDATE" : ""))) {
            p.setLong(1, id);
            try (var r = p.executeQuery()) { return r.next() ? ticket(r) : null; }
        }
    }
    public void mensagem(Connection c, long id, Usuario u, String texto) throws SQLException {
        JdbcComercio.inserir(c, """
            INSERT INTO ticket_mensagens(id_ticket,autor_tipo,id_participante,id_organizador,id_usuario_suporte,mensagem,data_envio)
            VALUES(?,?,?,?,?,?,UTC_TIMESTAMP())
            """, id, u.perfil().name().toLowerCase(Locale.ROOT),
                u.perfil() == Perfil.PARTICIPANTE ? u.id() : null,
                u.perfil() == Perfil.ORGANIZADOR ? u.id() : null,
                u.perfil() == Perfil.SUPORTE ? u.id() : null, texto);
    }
    public void transicionar(Connection c, long id, StatusTicket destino, String texto, boolean resposta) throws SQLException {
        // Snapshots legados acompanham a última mensagem; nunca substituem o histórico.
        JdbcComercio.executar(c, """
            UPDATE ticket SET status_ticket=?, data_atualizacao=UTC_TIMESTAMP(),
              data_fechamento=CASE WHEN ?='fechado' THEN UTC_TIMESTAMP() ELSE NULL END,
              atendido=CASE WHEN ?='fechado' THEN 1 ELSE 0 END,
              resposta=CASE WHEN ? THEN ? ELSE resposta END,
              retorno_usuario=CASE WHEN ? THEN ? ELSE retorno_usuario END WHERE id_ticket=?
            """, destino.name(), destino.name(), destino.name(), resposta && texto != null, texto,
                !resposta && texto != null, texto, id);
    }
    public void associar(Connection c, long id, long suporteId) throws SQLException {
        JdbcComercio.executar(c, "UPDATE ticket SET id_usuario_suporte=?,data_atualizacao=UTC_TIMESTAMP() WHERE id_ticket=?", suporteId, id);
    }
    private static String escopo(Usuario u) {
        return switch (u.perfil()) {
            case PARTICIPANTE -> "id_participante=?";
            case ORGANIZADOR -> "id_organizador=?";
            case SUPORTE -> "1=1";
        };
    }
    public PaginaTickets listar(Connection c, Usuario u, StatusTicket status, int pagina, int tamanho) throws SQLException {
        String where = escopo(u) + (status == null ? "" : " AND status_ticket=?");
        var args = new ArrayList<Object>();
        if (u.perfil() != Perfil.SUPORTE) args.add(u.id());
        if (status != null) args.add(status.name());
        long total = contar(c, "SELECT COUNT(*) FROM ticket WHERE " + where, args.toArray());
        args.add(tamanho); args.add((pagina - 1) * tamanho);
        var dados = new ArrayList<Ticket>();
        try (var p = c.prepareStatement("SELECT * FROM ticket WHERE " + where + " ORDER BY id_ticket DESC LIMIT ? OFFSET ?")) {
            JdbcComercio.preencher(p, args.toArray());
            try (var r = p.executeQuery()) { while (r.next()) dados.add(ticket(r)); }
        }
        return new PaginaTickets(List.copyOf(dados), pagina, tamanho, total);
    }
    public TicketDetalhe detalhe(Connection c, Ticket ticket, int pagina, int tamanho) throws SQLException {
        var mensagens = new ArrayList<MensagemTicket>();
        try (var p = c.prepareStatement("""
            SELECT m.id_ticket_mensagem,m.autor_tipo,m.mensagem,m.data_envio,
                   COALESCE(m.id_participante,m.id_organizador,m.id_usuario_suporte) autor_id,
                   COALESCE(p.nome,o.nome,s.nome) autor_nome
            FROM ticket_mensagens m
            LEFT JOIN participantes p ON p.id_participante=m.id_participante
            LEFT JOIN organizadores o ON o.id_organizador=m.id_organizador
            LEFT JOIN usuarios_suporte s ON s.id_usuario_suporte=m.id_usuario_suporte
            WHERE m.id_ticket=? ORDER BY m.id_ticket_mensagem LIMIT ? OFFSET ?
            """)) {
            JdbcComercio.preencher(p, ticket.id(), tamanho, (pagina - 1) * tamanho);
            try (var r = p.executeQuery()) {
                while (r.next()) mensagens.add(new MensagemTicket(r.getLong(1), r.getString(2), r.getLong("autor_id"),
                        r.getString("autor_nome"), r.getString("mensagem"), JdbcComercio.instante(r, "data_envio")));
            }
        }
        return new TicketDetalhe(ticket, List.copyOf(mensagens), pagina, tamanho,
                contar(c, "SELECT COUNT(*) FROM ticket_mensagens WHERE id_ticket=?", ticket.id()));
    }
    private static long contar(Connection c, String sql, Object... args) throws SQLException {
        try (var p = c.prepareStatement(sql)) {
            JdbcComercio.preencher(p, args);
            try (var r = p.executeQuery()) { r.next(); return r.getLong(1); }
        }
    }
    private static Ticket ticket(ResultSet r) throws SQLException {
        return new Ticket(r.getLong("id_ticket"), r.getString("titulo"), r.getString("descricao"),
                StatusTicket.valueOf(r.getString("status_ticket")), r.getObject("id_participante", Long.class),
                r.getObject("id_organizador", Long.class), r.getObject("id_usuario_suporte", Long.class),
                JdbcComercio.instante(r, "data_criacao"), JdbcComercio.instante(r, "data_atualizacao"), JdbcComercio.instante(r, "data_fechamento"));
    }
}
