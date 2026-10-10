package br.com.joinup.usuarios.repository;

import br.com.joinup.usuarios.domain.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public abstract class UsuarioRepositoryJdbc implements UsuarioRepository {
    private final Perfil perfil;
    protected UsuarioRepositoryJdbc(Perfil perfil) { this.perfil = perfil; }
    @Override public final Perfil perfil() { return perfil; }
    protected abstract Usuario mapear(ResultSet rs) throws SQLException;
    @Override public Usuario buscarEmail(Connection c, String email, boolean bloquear) throws SQLException {
        return buscar(c, "email", email, bloquear);
    }
    @Override public Usuario buscarId(Connection c, long id, boolean bloquear) throws SQLException {
        return buscar(c, perfil.colunaId(), id, bloquear);
    }
    private Usuario buscar(Connection c, String coluna, Object valor, boolean bloquear) throws SQLException {
        String sql = "SELECT * FROM " + perfil.tabela() + " WHERE " + coluna + "=?" + (bloquear ? " FOR UPDATE" : "");
        try (var ps = c.prepareStatement(sql)) {
            ps.setObject(1, valor);
            try (var rs = ps.executeQuery()) { return rs.next() ? mapear(rs) : null; }
        }
    }
    @Override public List<Usuario> listarInativos(Connection c, int limite, int offset) throws SQLException {
        if (perfil == Perfil.SUPORTE) throw new IllegalArgumentException("Suporte não possui ativo");
        try (var ps = c.prepareStatement("SELECT * FROM " + perfil.tabela() + " WHERE ativo=0 ORDER BY " + perfil.colunaId() + " LIMIT ? OFFSET ?")) {
            ps.setInt(1, limite); ps.setInt(2, offset);
            try (var rs = ps.executeQuery()) {
                var lista = new ArrayList<Usuario>();
                while (rs.next()) lista.add(mapear(rs));
                return List.copyOf(lista);
            }
        }
    }
    @Override public long contarInativos(Connection c) throws SQLException {
        if (perfil == Perfil.SUPORTE) throw new IllegalArgumentException("Suporte não possui ativo");
        try (var ps = c.prepareStatement("SELECT COUNT(*) FROM " + perfil.tabela() + " WHERE ativo=0"); var rs = ps.executeQuery()) {
            rs.next(); return rs.getLong(1);
        }
    }
    protected long inserir(Connection c, String sql, Object... valores) throws SQLException {
        try (var ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < valores.length; i++) ps.setObject(i + 1, valores[i]);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Identidade ausente");
                return keys.getLong(1);
            }
        }
    }
}
