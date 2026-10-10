package br.com.joinup.eventos.repository;

import br.com.joinup.eventos.domain.*;
import java.sql.*;

public class ComunidadeRepositoryJdbc {
    public long criar(Connection c,Evento e) throws SQLException {
        return JdbcEventos.inserir(c,"INSERT INTO comunidades(nome,descricao,imagem,id_evento,data_criacao) VALUES(?,?,?,?,UTC_TIMESTAMP())",
                "Comunidade: " + e.nome(),"Comunidade oficial do evento " + e.nome() + " em " + e.cidade() + "/" + e.estado() + ".",e.imagem(),e.id());
    }
    public Comunidade buscar(Connection c,long evento) throws SQLException {
        try(var ps=c.prepareStatement("SELECT id_comunidade,nome,imagem,id_evento FROM comunidades WHERE id_evento=?")) {
            ps.setLong(1,evento); try(var rs=ps.executeQuery()) { return rs.next()?new Comunidade(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getLong(4)):null; }
        }
    }
}
