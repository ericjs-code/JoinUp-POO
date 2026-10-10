package br.com.joinup.eventos.repository;

import br.com.joinup.eventos.domain.TipoIngresso;
import br.com.joinup.eventos.dto.TipoIngressoDados;
import java.sql.*;
import java.util.*;

public class TipoIngressoRepositoryJdbc {
    private static final String SELECT="SELECT t.*, (t.quantidade_vendida>0 OR EXISTS(SELECT 1 FROM compra_itens ci WHERE ci.id_tipo_ingresso=t.id_tipos_ingressos) OR EXISTS(SELECT 1 FROM ingressos i WHERE i.id_tipo_ingresso=t.id_tipos_ingressos)) AS tem_vendas FROM tipos_ingressos t";
    public List<TipoIngresso> listar(Connection c,long evento) throws SQLException {
        try(var ps=c.prepareStatement(SELECT + " WHERE t.id_evento=? ORDER BY t.id_tipos_ingressos")) {
            ps.setLong(1,evento); try(var rs=ps.executeQuery()) { var r=new ArrayList<TipoIngresso>(); while(rs.next()) r.add(mapear(rs)); return List.copyOf(r); }
        }
    }
    public TipoIngresso bloquear(Connection c,long evento,long tipo) throws SQLException {
        try(var ps=c.prepareStatement(SELECT + " WHERE t.id_evento=? AND t.id_tipos_ingressos=? FOR UPDATE")) {
            ps.setLong(1,evento); ps.setLong(2,tipo); try(var rs=ps.executeQuery()) { return rs.next()?mapear(rs):null; }
        }
    }
    public long criar(Connection c,long evento,TipoIngressoDados d) throws SQLException {
        return JdbcEventos.inserir(c,"INSERT INTO tipos_ingressos(nome_tipo,valor,quantidade_disponivel,id_evento) VALUES(?,?,?,?)",d.nome(),d.preco(),d.quantidade(),evento);
    }
    public void atualizar(Connection c,long tipo,TipoIngressoDados d) throws SQLException {
        JdbcEventos.executar(c,"UPDATE tipos_ingressos SET nome_tipo=?,valor=?,quantidade_disponivel=? WHERE id_tipos_ingressos=?",d.nome(),d.preco(),d.quantidade(),tipo);
    }
    private static TipoIngresso mapear(ResultSet rs) throws SQLException {
        return new TipoIngresso(rs.getLong("id_tipos_ingressos"),rs.getString("nome_tipo"),rs.getBigDecimal("valor"),rs.getInt("quantidade_disponivel"),rs.getInt("quantidade_vendida"),rs.getBoolean("tem_vendas"));
    }
}
