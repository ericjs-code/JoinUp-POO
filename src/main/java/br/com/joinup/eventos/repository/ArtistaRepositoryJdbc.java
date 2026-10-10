package br.com.joinup.eventos.repository;

import br.com.joinup.eventos.domain.ItemCatalogo;
import java.sql.*;
import java.util.*;

public class ArtistaRepositoryJdbc {
    public List<ItemCatalogo> buscar(Connection c, String busca) throws SQLException {
        return lista(c,"SELECT id_artista,nome FROM artistas WHERE LOCATE(?,nome)>0 ORDER BY nome,id_artista",busca);
    }
    public List<ItemCatalogo> generos(Connection c) throws SQLException {
        return lista(c,"SELECT id_genero_musical,nome FROM generos_musicais ORDER BY id_genero_musical");
    }
    public List<ItemCatalogo> lineup(Connection c, long evento) throws SQLException {
        return lista(c,"SELECT a.id_artista,a.nome FROM artistas a JOIN line_up l ON l.id_artista=a.id_artista WHERE l.id_evento=? ORDER BY a.id_artista",evento);
    }
    public boolean existe(Connection c, long id) throws SQLException {
        try(var ps=c.prepareStatement("SELECT id_artista FROM artistas WHERE id_artista=? FOR SHARE")) {
            ps.setLong(1,id); try(var rs=ps.executeQuery()) { return rs.next(); }
        }
    }
    public void substituirLineup(Connection c, long evento, List<Long> artistas) throws SQLException {
        JdbcEventos.executar(c,"DELETE FROM line_up WHERE id_evento=?",evento);
        try(var ps=c.prepareStatement("INSERT INTO line_up(id_evento,id_artista) VALUES(?,?)")) {
            for(long id:artistas) { ps.setLong(1,evento); ps.setLong(2,id); ps.addBatch(); }
            ps.executeBatch();
        }
    }
    private List<ItemCatalogo> lista(Connection c, String sql, Object... valores) throws SQLException {
        try(var ps=c.prepareStatement(sql)) {
            JdbcEventos.preencher(ps,valores);
            try(var rs=ps.executeQuery()) { var r=new ArrayList<ItemCatalogo>(); while(rs.next()) r.add(new ItemCatalogo(rs.getLong(1),rs.getString(2))); return List.copyOf(r); }
        }
    }
}
