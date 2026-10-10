package br.com.joinup.eventos.repository;

import br.com.joinup.eventos.domain.Evento;
import br.com.joinup.eventos.dto.FiltroEventos;
import java.sql.*;
import java.time.*;
import java.util.*;

public class EventoRepositoryJdbc {
    private static final String SELECT = "SELECT e.*, g.nome AS genero FROM eventos e JOIN generos_musicais g ON g.id_genero_musical=e.id_genero_musical";
    public Evento buscar(Connection c, long id, boolean bloquear) throws SQLException {
        try (var ps=c.prepareStatement(SELECT + " WHERE e.id_evento=?" + (bloquear ? " FOR UPDATE" : " FOR SHARE"))) {
            ps.setLong(1,id);
            try (var rs=ps.executeQuery()) { return rs.next() ? mapear(rs) : null; }
        }
    }
    public long criar(Connection c, Evento e) throws SQLException {
        return JdbcEventos.inserir(c,"INSERT INTO eventos(nome,data,descricao,imagem,localizacao,cidade,estado,cep,id_genero_musical,id_organizador) VALUES(?,?,?,?,?,?,?,?,?,?)",
                e.nome(), utc(e.data()), e.descricao(), e.imagem(), e.localizacao(), e.cidade(), e.estado(), e.cep(), e.generoId(), e.organizadorId());
    }
    public void atualizar(Connection c, Evento e) throws SQLException {
        JdbcEventos.executar(c,"UPDATE eventos SET nome=?,data=?,descricao=?,imagem=?,localizacao=?,cidade=?,estado=?,cep=?,id_genero_musical=? WHERE id_evento=?",
                e.nome(),utc(e.data()),e.descricao(),e.imagem(),e.localizacao(),e.cidade(),e.estado(),e.cep(),e.generoId(),e.id());
    }
    public boolean generoExiste(Connection c, long id) throws SQLException {
        try (var ps=c.prepareStatement("SELECT id_genero_musical FROM generos_musicais WHERE id_genero_musical=? FOR SHARE")) {
            ps.setLong(1,id); try(var rs=ps.executeQuery()) { return rs.next(); }
        }
    }
    private static String where(Long organizador) {
        return " WHERE (?='' OR LOCATE(?,e.nome)>0 OR LOCATE(?,e.descricao)>0) AND (? IS NULL OR e.id_genero_musical=?) AND (?='' OR e.cidade=?)"
                + (organizador == null ? "" : " AND e.id_organizador=?");
    }
    private static List<Object> parametros(FiltroEventos f, Long dono) {
        var v=new ArrayList<Object>(Arrays.asList(f.busca(),f.busca(),f.busca(),f.generoId(),f.generoId(),f.cidade(),f.cidade()));
        if(dono != null) v.add(dono); return v;
    }
    public List<Evento> listar(Connection c, FiltroEventos f, Long dono) throws SQLException {
        var v=parametros(f,dono); v.add(f.tamanho()); v.add((f.pagina()-1)*f.tamanho());
        try(var ps=c.prepareStatement(SELECT + where(dono) + " ORDER BY e.data,e.id_evento LIMIT ? OFFSET ?")) {
            JdbcEventos.preencher(ps,v.toArray());
            try(var rs=ps.executeQuery()) { var r=new ArrayList<Evento>(); while(rs.next()) r.add(mapear(rs)); return List.copyOf(r); }
        }
    }
    public long contar(Connection c, FiltroEventos f, Long dono) throws SQLException {
        try(var ps=c.prepareStatement("SELECT COUNT(*) FROM eventos e" + where(dono))) {
            JdbcEventos.preencher(ps,parametros(f,dono).toArray()); try(var rs=ps.executeQuery()) { rs.next(); return rs.getLong(1); }
        }
    }
    private static LocalDateTime utc(Instant i) { return LocalDateTime.ofInstant(i,ZoneOffset.UTC); }
    private static Evento mapear(ResultSet rs) throws SQLException {
        return new Evento(rs.getLong("id_evento"),rs.getString("nome"),rs.getObject("data",LocalDateTime.class).toInstant(ZoneOffset.UTC),
                rs.getString("descricao"),rs.getString("imagem"),rs.getString("localizacao"),rs.getString("cidade"),rs.getString("estado"),
                rs.getString("cep"),rs.getLong("id_organizador"),rs.getLong("id_genero_musical"),rs.getString("genero"));
    }
}
