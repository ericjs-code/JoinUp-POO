package br.com.joinup.compras.repository;

import br.com.joinup.compras.domain.EventoComercial;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import static br.com.joinup.compras.repository.JdbcComercio.*;

public final class CompraRepositoryJdbc {
    // Sem JOIN no lock: não introduzir locks implícitos em ordem desconhecida.
    public EventoComercial evento(Connection c,long id,boolean lock) throws SQLException {
        try (var ps=c.prepareStatement("SELECT id_evento,id_organizador,nome,data,localizacao FROM eventos WHERE id_evento=?"+(lock?" FOR UPDATE":""))) {
            ps.setLong(1,id); try (var rs=ps.executeQuery()) { return rs.next()?new EventoComercial(rs.getLong(1),rs.getLong(2),rs.getString(3),instante(rs,"data"),rs.getString(5)):null; }
        }
    }
    public void baixarEstoque(Connection c,long tipo,int quantidade) throws SQLException {
        int mudou=executar(c,"UPDATE tipos_ingressos SET quantidade_disponivel=quantidade_disponivel-?,quantidade_vendida=quantidade_vendida+? WHERE id_tipos_ingressos=? AND quantidade_disponivel>=? AND quantidade_vendida<=2147483647-?",quantidade,quantidade,tipo,quantidade,quantidade);
        if (mudou!=1) throw new br.com.joinup.http.HttpException(409,"ESTOQUE_INSUFICIENTE","Quantidade indisponível");
    }
    public long criar(Connection c,long participante,EventoComercial e,BigDecimal total) throws SQLException {
        return inserir(c,"INSERT INTO compras(id_participante,id_evento,status,valor_total,data_compra,nome_evento_na_compra,data_evento_na_compra,local_evento_na_compra) VALUES(?,?,'confirmada',?,UTC_TIMESTAMP(),?,?,?)",participante,e.id(),total,e.nome(),LocalDateTime.ofInstant(e.data(),ZoneOffset.UTC),e.local());
    }
    public void item(Connection c,long compra,long tipo,int quantidade,BigDecimal preco,BigDecimal subtotal) throws SQLException {
        inserir(c,"INSERT INTO compra_itens(id_compra,id_tipo_ingresso,quantidade,valor_unitario,subtotal) VALUES(?,?,?,?,?)",compra,tipo,quantidade,preco,subtotal);
    }
    public long emitir(Connection c,long compra,long participante,long evento,long tipo) throws SQLException {
        return inserir(c,"INSERT INTO ingressos(data_compra,status,id_participante,id_evento,id_tipo_ingresso,id_compra) VALUES(UTC_DATE(),'ativo',?,?,?,?)",participante,evento,tipo,compra);
    }
    public List<Map<String,Object>> historico(Connection c,long participante) throws SQLException {
        var compras=new LinkedHashMap<Long,Map<String,Object>>();
        try (var ps=c.prepareStatement("SELECT id_compra,id_evento,status,valor_total,data_compra,nome_evento_na_compra,data_evento_na_compra,local_evento_na_compra FROM compras WHERE id_participante=? ORDER BY id_compra DESC")) {
            ps.setLong(1,participante); try (var rs=ps.executeQuery()) { while(rs.next()) {
                var m=new LinkedHashMap<String,Object>(); long id=rs.getLong("id_compra"); m.put("compraId",id); m.put("eventoId",rs.getLong("id_evento"));
                m.put("status",rs.getString("status")); m.put("total",rs.getBigDecimal("valor_total")); m.put("dataCompra",instante(rs,"data_compra"));
                m.put("nomeEventoNaCompra",rs.getString("nome_evento_na_compra")); m.put("dataEventoNaCompra",instante(rs,"data_evento_na_compra")); m.put("localEventoNaCompra",rs.getString("local_evento_na_compra"));
                m.put("pagamento","simulado"); m.put("itens",new ArrayList<Map<String,Object>>()); compras.put(id,m);
            } }
        }
        try (var ps=c.prepareStatement("SELECT ci.* FROM compra_itens ci JOIN compras c ON c.id_compra=ci.id_compra WHERE c.id_participante=? ORDER BY ci.id_compra_item")) {
            ps.setLong(1,participante); try (var rs=ps.executeQuery()) { while(rs.next()) {
                @SuppressWarnings("unchecked") var itens=(List<Map<String,Object>>)compras.get(rs.getLong("id_compra")).get("itens");
                itens.add(Map.of("tipoIngressoId",rs.getLong("id_tipo_ingresso"),"quantidade",rs.getInt("quantidade"),"valorUnitario",rs.getBigDecimal("valor_unitario"),"subtotal",rs.getBigDecimal("subtotal")));
            } }
        }
        return List.copyOf(compras.values());
    }
}
