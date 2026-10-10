package br.com.joinup.usuarios.repository;

import br.com.joinup.usuarios.domain.*;
import java.sql.*;

public final class ContaRepositoryJdbc {
    public void atualizar(Connection c, Usuario u, String nome, String email, String hashSenha, String cnpj) throws SQLException {
        String sql = "UPDATE " + u.perfil().tabela() + " SET nome=?,email=?,senha=?"
                + (u.perfil() == Perfil.ORGANIZADOR ? ",cnpj=?" : "") + " WHERE " + u.perfil().colunaId() + "=?";
        try (var ps = c.prepareStatement(sql)) {
            int i = 1; ps.setString(i++, nome); ps.setString(i++, email); ps.setString(i++, hashSenha);
            if (u.perfil() == Perfil.ORGANIZADOR) ps.setString(i++, cnpj);
            ps.setLong(i, u.id()); ps.executeUpdate();
        }
    }
    public void desativar(Connection c, Usuario u) throws SQLException {
        try (var ps = c.prepareStatement("UPDATE " + u.perfil().tabela() + " SET ativo=0,data_desativacao=UTC_TIMESTAMP() WHERE " + u.perfil().colunaId() + "=?")) {
            ps.setLong(1, u.id()); ps.executeUpdate();
        }
    }
    public void cancelarAnuncios(Connection c, long idParticipante) throws SQLException {
        try (var ps = c.prepareStatement("UPDATE revenda_anuncios SET status='cancelado',data_cancelamento=UTC_TIMESTAMP() WHERE id_participante=? AND status='disponivel'")) {
            ps.setLong(1, idParticipante); ps.executeUpdate();
        }
    }
    public void reativar(Connection c, Usuario u, long idSuporte) throws SQLException {
        try (var ps = c.prepareStatement("UPDATE " + u.perfil().tabela() + " SET ativo=1,data_desativacao=NULL WHERE " + u.perfil().colunaId() + "=?")) {
            ps.setLong(1, u.id()); ps.executeUpdate();
        }
        try (var ps = c.prepareStatement("INSERT INTO auditoria_reativacoes(id_usuario_suporte," + u.perfil().colunaId() + ",data_reativacao) VALUES(?,?,UTC_TIMESTAMP())")) {
            ps.setLong(1, idSuporte); ps.setLong(2, u.id()); ps.executeUpdate();
        }
    }
}
