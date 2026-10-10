package br.com.joinup.compras.repository;

import br.com.joinup.compras.domain.Ingresso;
import java.sql.*;
import java.util.*;
import static br.com.joinup.compras.repository.JdbcComercio.*;

public final class IngressoRepositoryJdbc {
    public Ingresso buscar(Connection c,long id,boolean lock) throws SQLException {
        try (var ps=c.prepareStatement("SELECT id_ingresso,id_participante,id_evento,id_tipo_ingresso,status FROM ingressos WHERE id_ingresso=?"+(lock?" FOR UPDATE":""))) {
            ps.setLong(1,id); try (var rs=ps.executeQuery()) { return rs.next()?new Ingresso(rs.getLong(1),rs.getLong(2),rs.getLong(3),rs.getLong(4),rs.getString(5)):null; }
        }
    }
    public void transferir(Connection c,Ingresso i,long comprador) throws SQLException {
        if(executar(c,"UPDATE ingressos SET id_participante=? WHERE id_ingresso=? AND id_participante=? AND status='ativo'",comprador,i.id(),i.participanteId())!=1)
            throw new br.com.joinup.http.HttpException(409,"REVENDA_INDISPONIVEL","Ingresso indisponível");
    }
    public void sairSemIngressoElegivel(Connection c,long vendedor,long evento) throws SQLException {
        // Conta do vendedor e evento já bloqueados: nenhuma compra/transferência do mesmo
        // participante pode surgir entre NOT EXISTS e UPDATE. RF-COM-05 inclui utilizado.
        executar(c,"UPDATE participante_comunidades pc JOIN comunidades cm ON cm.id_comunidade=pc.id_comunidade SET pc.ativo=0,pc.data_saida=UTC_TIMESTAMP() WHERE pc.id_participante=? AND cm.id_evento=? AND pc.ativo=1 AND NOT EXISTS(SELECT 1 FROM ingressos i WHERE i.id_participante=? AND i.id_evento=? AND i.status IN('ativo','utilizado'))",vendedor,evento,vendedor,evento);
    }
    public List<Map<String,Object>> listar(Connection c,long titular,Long ingresso) throws SQLException {
        String sql="SELECT i.id_ingresso,i.status,i.id_evento,i.id_tipo_ingresso,i.data_compra,e.nome,e.data,e.localizacao,t.nome_tipo,c.nome_evento_na_compra,c.data_evento_na_compra,c.local_evento_na_compra,a.id_revenda_anuncios, o.ativo AS organizador_ativo FROM ingressos i JOIN eventos e ON e.id_evento=i.id_evento JOIN organizadores o ON o.id_organizador=e.id_organizador JOIN tipos_ingressos t ON t.id_tipos_ingressos=i.id_tipo_ingresso JOIN compras c ON c.id_compra=i.id_compra LEFT JOIN revenda_anuncios a ON a.id_ingresso=i.id_ingresso AND a.status='disponivel' WHERE i.id_participante=?"+(ingresso==null?"":" AND i.id_ingresso=?")+" ORDER BY e.data,i.id_evento,i.id_tipo_ingresso,i.status,i.id_ingresso";
        var lista=new ArrayList<Map<String,Object>>();
        try (var ps=c.prepareStatement(sql)) {
            ps.setLong(1,titular); if(ingresso!=null) ps.setLong(2,ingresso);
            try (var rs=ps.executeQuery()) { while(rs.next()) {
                var m=new LinkedHashMap<String,Object>(); m.put("id",rs.getLong("id_ingresso")); m.put("status",rs.getString("status"));
                m.put("eventoId",rs.getLong("id_evento")); m.put("nomeEvento",rs.getString("nome")); m.put("dataEvento",instante(rs,"data")); m.put("localEvento",rs.getString("localizacao"));
                m.put("tipoIngressoId",rs.getLong("id_tipo_ingresso")); m.put("nomeTipo",rs.getString("nome_tipo")); m.put("dataCompra",rs.getObject("data_compra",java.time.LocalDate.class));
                m.put("nomeEventoNaCompra",rs.getString("nome_evento_na_compra")); m.put("dataEventoNaCompra",instante(rs,"data_evento_na_compra")); m.put("localEventoNaCompra",rs.getString("local_evento_na_compra"));
                m.put("anuncioDisponivelId",rs.getObject("id_revenda_anuncios")); m.put("organizadorAtivo",rs.getBoolean("organizador_ativo")); lista.add(m);
            } }
        }
        return List.copyOf(lista);
    }
}
