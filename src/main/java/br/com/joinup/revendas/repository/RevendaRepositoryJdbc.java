package br.com.joinup.revendas.repository;

import br.com.joinup.revendas.domain.Anuncio;
import br.com.joinup.revendas.dto.FiltroRevendas;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import static br.com.joinup.compras.repository.JdbcComercio.*;

public final class RevendaRepositoryJdbc {
    public Anuncio buscar(Connection c,long id,boolean lock) throws SQLException {
        try(var ps=c.prepareStatement("SELECT id_revenda_anuncios,id_ingresso,id_participante,valor_revenda,status FROM revenda_anuncios WHERE id_revenda_anuncios=?"+(lock?" FOR UPDATE":""))) {
            ps.setLong(1,id); try(var rs=ps.executeQuery()) { return rs.next()?new Anuncio(rs.getLong(1),rs.getLong(2),rs.getLong(3),rs.getBigDecimal(4),rs.getString(5)):null; }
        }
    }
    public boolean disponivel(Connection c,long ingresso) throws SQLException {
        try(var ps=c.prepareStatement("SELECT id_revenda_anuncios FROM revenda_anuncios WHERE id_ingresso_disponivel=? FOR UPDATE")) {
            ps.setLong(1,ingresso); try(var rs=ps.executeQuery()) { return rs.next(); }
        }
    }
    public long criar(Connection c,long ingresso,long vendedor,java.math.BigDecimal preco) throws SQLException {
        try { return inserir(c,"INSERT INTO revenda_anuncios(id_ingresso,id_participante,valor_revenda,status,data_criacao) VALUES(?,?,?,'disponivel',UTC_TIMESTAMP())",ingresso,vendedor,preco); }
        catch(SQLException e) { if(e.getErrorCode()==1062) throw br.com.joinup.compras.service.ValidacaoComercio.indisponivel(); throw e; }
    }
    public void cancelar(Connection c,long id,long vendedor) throws SQLException {
        if(executar(c,"UPDATE revenda_anuncios SET status='cancelado',data_cancelamento=UTC_TIMESTAMP() WHERE id_revenda_anuncios=? AND id_participante=? AND status='disponivel'",id,vendedor)!=1)
            throw br.com.joinup.compras.service.ValidacaoComercio.indisponivel();
    }
    public void vender(Connection c,long id,long comprador) throws SQLException {
        if(executar(c,"UPDATE revenda_anuncios SET status='vendido',id_comprador=?,data_venda=UTC_TIMESTAMP() WHERE id_revenda_anuncios=? AND status='disponivel'",comprador,id)!=1)
            throw br.com.joinup.compras.service.ValidacaoComercio.indisponivel();
    }
    private static final String PROJECAO="SELECT a.id_revenda_anuncios,a.id_ingresso,a.id_participante AS vendedor,a.valor_revenda,a.status,a.data_criacao,a.data_venda,a.data_cancelamento,i.status AS ingresso_status,i.id_participante AS titular,e.id_evento,e.nome,e.data,e.localizacao,t.nome_tipo,o.ativo AS organizador_ativo,p.ativo AS vendedor_ativo FROM revenda_anuncios a JOIN ingressos i ON i.id_ingresso=a.id_ingresso JOIN eventos e ON e.id_evento=i.id_evento JOIN tipos_ingressos t ON t.id_tipos_ingressos=i.id_tipo_ingresso JOIN organizadores o ON o.id_organizador=e.id_organizador JOIN participantes p ON p.id_participante=a.id_participante";
    public List<Map<String,Object>> listar(Connection c,long usuario,FiltroRevendas f,boolean meus,Instant agora) throws SQLException {
        return projetar(c,PROJECAO+(meus?" WHERE a.id_participante=? ORDER BY a.id_revenda_anuncios DESC":" WHERE a.status='disponivel' AND (? IS NULL OR e.id_evento=?) ORDER BY a.id_revenda_anuncios DESC LIMIT ? OFFSET ?"),usuario,agora,
                meus?new Object[]{usuario}:new Object[]{f.eventoId(),f.eventoId(),f.tamanho(),(f.pagina()-1)*f.tamanho()});
    }
    public Map<String,Object> detalhe(Connection c,long usuario,long id,Instant agora) throws SQLException {
        var lista=projetar(c,PROJECAO+" WHERE a.id_revenda_anuncios=?",usuario,agora,id);
        return lista.isEmpty()?null:lista.getFirst();
    }
    public long contar(Connection c,FiltroRevendas f) throws SQLException {
        try(var ps=c.prepareStatement("SELECT COUNT(*) FROM revenda_anuncios a JOIN ingressos i ON i.id_ingresso=a.id_ingresso WHERE a.status='disponivel' AND (? IS NULL OR i.id_evento=?)")) {
            preencher(ps,f.eventoId(),f.eventoId()); try(var rs=ps.executeQuery()) { rs.next(); return rs.getLong(1); }
        }
    }
    public List<Map<String,Object>> recebidas(Connection c,long comprador) throws SQLException {
        var lista=new ArrayList<Map<String,Object>>();
        try(var ps=c.prepareStatement("SELECT id_revenda_anuncios,id_ingresso,valor_revenda,data_venda FROM revenda_anuncios WHERE id_comprador=? AND status='vendido' ORDER BY id_revenda_anuncios DESC")) {
            ps.setLong(1,comprador); try(var rs=ps.executeQuery()) { while(rs.next()) lista.add(Map.of("anuncioId",rs.getLong(1),"ingressoId",rs.getLong(2),"total",rs.getBigDecimal(3),"dataVenda",instante(rs,"data_venda"),"pagamento","simulado")); }
        }
        return List.copyOf(lista);
    }
    private List<Map<String,Object>> projetar(Connection c,String sql,long usuario,Instant agora,Object... args) throws SQLException {
        var lista=new ArrayList<Map<String,Object>>();
        try(var ps=c.prepareStatement(sql)) {
            preencher(ps,args); try(var rs=ps.executeQuery()) { while(rs.next()) {
                boolean proprio=rs.getLong("vendedor")==usuario;
                boolean elegivel=rs.getString("status").equals("disponivel") && rs.getString("ingresso_status").equals("ativo") && rs.getLong("vendedor")==rs.getLong("titular") && rs.getBoolean("vendedor_ativo") && rs.getBoolean("organizador_ativo") && instante(rs,"data").isAfter(agora);
                var m=new LinkedHashMap<String,Object>(); m.put("id",rs.getLong("id_revenda_anuncios")); m.put("ingressoId",rs.getLong("id_ingresso")); m.put("preco",rs.getBigDecimal("valor_revenda")); m.put("status",rs.getString("status"));
                m.put("eventoId",rs.getLong("id_evento")); m.put("nomeEvento",rs.getString("nome")); m.put("dataEvento",instante(rs,"data")); m.put("localEvento",rs.getString("localizacao")); m.put("nomeTipo",rs.getString("nome_tipo"));
                m.put("proprio",proprio); m.put("compravel",elegivel && !proprio); m.put("organizadorAtivo",rs.getBoolean("organizador_ativo"));
                m.put("dataCriacao",instante(rs,"data_criacao")); m.put("dataVenda",instante(rs,"data_venda")); m.put("dataCancelamento",instante(rs,"data_cancelamento")); lista.add(m);
            } }
        }
        return List.copyOf(lista);
    }
}
