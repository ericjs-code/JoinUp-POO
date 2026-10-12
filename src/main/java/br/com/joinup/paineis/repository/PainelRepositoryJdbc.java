package br.com.joinup.paineis.repository;

import br.com.joinup.compras.repository.JdbcComercio;
import br.com.joinup.suporte.domain.StatusTicket;
import br.com.joinup.usuarios.domain.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/** Número constante de consultas por painel. Agregações independentes evitam multiplicação de vendas por JOIN. */
public final class PainelRepositoryJdbc {
    public Map<String, Object> participante(Connection c, long id, Instant agora) throws SQLException {
        return linha(c, """
            SELECT
              (SELECT COUNT(*) FROM ingressos WHERE id_participante=?) ingressos,
              (SELECT COUNT(*) FROM ingressos i JOIN eventos e ON e.id_evento=i.id_evento
                 WHERE i.id_participante=? AND i.status='ativo' AND e.data>?) proximosIngressos,
              (SELECT COUNT(DISTINCT i.id_evento) FROM ingressos i JOIN eventos e ON e.id_evento=i.id_evento
                 WHERE i.id_participante=? AND i.status='ativo' AND e.data>?) proximosEventos,
              (SELECT COUNT(*) FROM compras WHERE id_participante=? AND status='confirmada') comprasConfirmadas,
              (SELECT COALESCE(SUM(valor_total),0) FROM compras WHERE id_participante=? AND status='confirmada') valorHistoricoCompras,
              (SELECT COUNT(*) FROM revenda_anuncios WHERE id_participante=? AND status='disponivel') anunciosDisponiveis,
              (SELECT COUNT(*) FROM revenda_anuncios WHERE id_participante=? AND status='vendido') anunciosVendidos,
              (SELECT COUNT(*) FROM revenda_anuncios WHERE id_participante=? AND status='cancelado') anunciosCancelados,
              (SELECT COUNT(*) FROM participante_comunidades pc JOIN comunidades cm ON cm.id_comunidade=pc.id_comunidade
                 WHERE pc.id_participante=? AND pc.ativo=1 AND EXISTS(SELECT 1 FROM ingressos i
                   WHERE i.id_participante=pc.id_participante AND i.id_evento=cm.id_evento AND i.status IN('ativo','utilizado'))) comunidadesAcessiveis
            """, id,id,utc(agora),id,utc(agora),id,id,id,id,id,id);
    }
    public List<Map<String, Object>> proximos(Connection c, long id, Instant agora) throws SQLException {
        return linhas(c, """
            SELECT e.id_evento id,e.nome,e.data,COUNT(*) ingressos
            FROM ingressos i JOIN eventos e ON e.id_evento=i.id_evento
            WHERE i.id_participante=? AND i.status='ativo' AND e.data>?
            GROUP BY e.id_evento,e.nome,e.data ORDER BY e.data,e.id_evento LIMIT 20
            """, id,utc(agora));
    }
    public Map<String, Object> organizador(Connection c, long id, Instant agora) throws SQLException {
        return linha(c, """
            SELECT
              (SELECT COUNT(*) FROM eventos WHERE id_organizador=?) eventos,
              (SELECT COUNT(*) FROM eventos WHERE id_organizador=? AND data>?) eventosFuturos,
              (SELECT COUNT(*) FROM eventos WHERE id_organizador=? AND data<=?) eventosIniciados,
              (SELECT COUNT(*) FROM tipos_ingressos t JOIN eventos e ON e.id_evento=t.id_evento WHERE e.id_organizador=?) categorias,
              (SELECT COALESCE(SUM(t.quantidade_disponivel),0) FROM tipos_ingressos t JOIN eventos e ON e.id_evento=t.id_evento WHERE e.id_organizador=?) estoqueDisponivel,
              (SELECT COALESCE(SUM(ci.quantidade),0) FROM compra_itens ci JOIN compras cp ON cp.id_compra=ci.id_compra
                 JOIN eventos e ON e.id_evento=cp.id_evento WHERE e.id_organizador=? AND cp.status='confirmada') ingressosVendidos,
              (SELECT COUNT(*) FROM compras cp JOIN eventos e ON e.id_evento=cp.id_evento WHERE e.id_organizador=? AND cp.status='confirmada') comprasConfirmadas,
              (SELECT COALESCE(SUM(cp.valor_total),0) FROM compras cp JOIN eventos e ON e.id_evento=cp.id_evento
                 WHERE e.id_organizador=? AND cp.status='confirmada') valorHistoricoCompras,
              (SELECT COUNT(*) FROM comunidades cm JOIN eventos e ON e.id_evento=cm.id_evento WHERE e.id_organizador=?) comunidades
            """, id,id,utc(agora),id,utc(agora),id,id,id,id,id,id);
    }
    public List<Map<String, Object>> eventos(Connection c, long id, Instant agora) throws SQLException {
        return linhas(c, """
            SELECT e.id_evento id,e.nome,e.data,CASE WHEN e.data>? THEN 'futuro' ELSE 'iniciado' END situacao,
              (SELECT COUNT(*) FROM tipos_ingressos t WHERE t.id_evento=e.id_evento) categorias,
              (SELECT COALESCE(SUM(ci.quantidade),0) FROM compra_itens ci JOIN compras cp ON cp.id_compra=ci.id_compra
                 WHERE cp.id_evento=e.id_evento AND cp.status='confirmada') ingressosVendidos,
              (SELECT COALESCE(SUM(cp.valor_total),0) FROM compras cp WHERE cp.id_evento=e.id_evento AND cp.status='confirmada') valorHistoricoCompras
            FROM eventos e WHERE e.id_organizador=? ORDER BY e.data DESC,e.id_evento DESC LIMIT 20
            """, utc(agora),id);
    }
    public Map<String, Object> suporte(Connection c, long id) throws SQLException {
        return linha(c, """
            SELECT (SELECT COUNT(*) FROM ticket) chamados,
              (SELECT COUNT(*) FROM ticket WHERE id_usuario_suporte=?) associadosAMim,
              (SELECT COUNT(*) FROM ticket WHERE id_usuario_suporte IS NULL) semAssociacao,
              (SELECT COUNT(*) FROM ticket_mensagens WHERE autor_tipo='suporte') respostasSuporte,
              (SELECT COUNT(*) FROM ticket_mensagens WHERE autor_tipo='suporte' AND id_usuario_suporte=?) minhasRespostas,
              (SELECT COUNT(DISTINCT id_ticket) FROM ticket_mensagens WHERE autor_tipo='suporte' AND id_usuario_suporte=?) chamadosRespondidosPorMim,
              (SELECT COUNT(*) FROM ticket WHERE id_participante IS NOT NULL) chamadosParticipantes,
              (SELECT COUNT(*) FROM ticket WHERE id_organizador IS NOT NULL) chamadosOrganizadores,
              (SELECT COUNT(*) FROM ticket WHERE data_criacao>=UTC_DATE() AND data_criacao<UTC_DATE()+INTERVAL 1 DAY) criadosHojeUtc,
              (SELECT AVG(TIMESTAMPDIFF(SECOND,data_criacao,data_fechamento)) FROM ticket
                 WHERE status_ticket='fechado' AND data_fechamento>=data_criacao) mediaResolucaoSegundos,
              (SELECT ROUND(100.0*SUM(status_ticket='fechado')/NULLIF(COUNT(*),0),2) FROM ticket) percentualFechados
            """, id,id,id);
    }
    public Map<String, Long> chamados(Connection c, Usuario u) throws SQLException {
        var dados = new LinkedHashMap<String, Long>();
        for (var status : StatusTicket.values()) dados.put(status.name(), 0L);
        String where = switch (u.perfil()) {
            case PARTICIPANTE -> " WHERE id_participante=?";
            case ORGANIZADOR -> " WHERE id_organizador=?";
            case SUPORTE -> "";
        };
        try (var p = c.prepareStatement("SELECT status_ticket,COUNT(*) FROM ticket" + where + " GROUP BY status_ticket")) {
            if (u.perfil() != Perfil.SUPORTE) p.setLong(1,u.id());
            try (var r = p.executeQuery()) { while (r.next()) dados.put(r.getString(1),r.getLong(2)); }
        }
        return Collections.unmodifiableMap(dados);
    }
    private static java.time.LocalDateTime utc(Instant instante) { return java.time.LocalDateTime.ofInstant(instante,java.time.ZoneOffset.UTC); }
    private static Map<String, Object> linha(Connection c, String sql, Object... args) throws SQLException {
        return linhas(c,sql,args).getFirst();
    }
    private static List<Map<String, Object>> linhas(Connection c, String sql, Object... args) throws SQLException {
        var dados = new ArrayList<Map<String, Object>>();
        try (var p = c.prepareStatement(sql)) {
            JdbcComercio.preencher(p,args);
            try (var r = p.executeQuery()) {
                var meta = r.getMetaData();
                while (r.next()) {
                    var item = new LinkedHashMap<String, Object>();
                    for (int i=1;i<=meta.getColumnCount();i++) {
                        String nome = meta.getColumnLabel(i);
                        item.put(nome, nome.equals("data") ? JdbcComercio.instante(r,nome) : r.getObject(i));
                    }
                    dados.add(Collections.unmodifiableMap(item));
                }
            }
        }
        return List.copyOf(dados);
    }
}
