package br.com.joinup.auth;

import br.com.joinup.usuarios.domain.*;
import java.sql.*;

public final class SessaoRepositoryJdbc {
    public record Titular(long id, Perfil perfil) { }
    public void criar(Connection c, String hash, Usuario usuario, long segundos) throws SQLException {
        try (var ps = c.prepareStatement("INSERT INTO sessoes(hash_token," + usuario.perfil().colunaId()
                + ",criada_em,expira_em) VALUES(?,?,UTC_TIMESTAMP(),TIMESTAMPADD(SECOND,?,UTC_TIMESTAMP()))")) {
            ps.setString(1, hash); ps.setLong(2, usuario.id()); ps.setLong(3, segundos); ps.executeUpdate();
        }
    }
    public Titular localizar(Connection c, String hash) throws SQLException {
        try (var ps = c.prepareStatement("SELECT id_participante,id_organizador,id_usuario_suporte FROM sessoes WHERE hash_token=?")) {
            ps.setString(1, hash);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                for (Perfil perfil : Perfil.values()) {
                    long id = rs.getLong(perfil.colunaId());
                    if (!rs.wasNull()) return new Titular(id, perfil);
                }
                return null;
            }
        }
    }
    public boolean validarEBloquear(Connection c, String hash) throws SQLException {
        try (var ps = c.prepareStatement("SELECT revogada_em IS NULL AND expira_em>UTC_TIMESTAMP() AS valida FROM sessoes WHERE hash_token=? FOR UPDATE")) {
            ps.setString(1, hash);
            try (var rs = ps.executeQuery()) { return rs.next() && rs.getBoolean("valida"); }
        }
    }
    public void revogar(Connection c, String hash) throws SQLException {
        try (var ps = c.prepareStatement("UPDATE sessoes SET revogada_em=UTC_TIMESTAMP() WHERE hash_token=? AND revogada_em IS NULL")) {
            ps.setString(1, hash); ps.executeUpdate();
        }
    }
    public void revogarTodas(Connection c, Usuario usuario) throws SQLException {
        try (var ps = c.prepareStatement("UPDATE sessoes SET revogada_em=UTC_TIMESTAMP() WHERE "
                + usuario.perfil().colunaId() + "=? AND revogada_em IS NULL")) {
            ps.setLong(1, usuario.id()); ps.executeUpdate();
        }
    }
}
