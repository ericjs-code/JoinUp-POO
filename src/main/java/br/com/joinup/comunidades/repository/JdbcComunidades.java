package br.com.joinup.comunidades.repository;

import java.sql.*;
import java.time.*;

public final class JdbcComunidades {
    private JdbcComunidades() {}

    public static void preencher(PreparedStatement ps, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
    }

    public static int executar(Connection c, String sql, Object... args) throws SQLException {
        try (var ps = c.prepareStatement(sql)) {
            preencher(ps, args);
            return ps.executeUpdate();
        }
    }

    public static long inserir(Connection c, String sql, Object... args) throws SQLException {
        try (var ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preencher(ps, args);
            ps.executeUpdate();
            try (var rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("Identidade não retornada");
                return rs.getLong(1);
            }
        }
    }

    public static Instant instante(ResultSet rs, String campo) throws SQLException {
        var data = rs.getObject(campo, LocalDateTime.class);
        return data == null ? null : data.toInstant(ZoneOffset.UTC);
    }
}
