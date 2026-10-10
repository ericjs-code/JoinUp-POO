package br.com.joinup.eventos.repository;

import java.sql.*;

/** Utilitário JDBC; todos os recursos usam a conexão recebida da unidade de trabalho. */
final class JdbcEventos {
    private JdbcEventos() {}
    static void preencher(PreparedStatement ps, Object... valores) throws SQLException {
        for (int i=0; i<valores.length; i++) ps.setObject(i+1, valores[i]);
    }
    static long inserir(Connection c, String sql, Object... valores) throws SQLException {
        try (var ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preencher(ps, valores); ps.executeUpdate();
            try (var rs=ps.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("Identidade ausente");
                return rs.getLong(1);
            }
        }
    }
    static void executar(Connection c, String sql, Object... valores) throws SQLException {
        try (var ps=c.prepareStatement(sql)) { preencher(ps,valores); ps.executeUpdate(); }
    }
}
