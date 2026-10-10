package br.com.joinup.usuarios.repository;

import br.com.joinup.usuarios.domain.*;
import java.sql.*;

public final class SuporteRepositoryJdbc extends UsuarioRepositoryJdbc {
    public SuporteRepositoryJdbc() { super(Perfil.SUPORTE); }
    @Override protected UsuarioSuporte mapear(ResultSet rs) throws SQLException {
        return new UsuarioSuporte(rs.getLong("id_usuario_suporte"), rs.getString("nome"), rs.getString("email"), rs.getString("senha"));
    }
    @Override public long salvar(Connection c, Usuario usuario) throws SQLException {
        if (!(usuario instanceof UsuarioSuporte)) throw new IllegalArgumentException("Produto incorreto");
        return inserir(c, "INSERT INTO usuarios_suporte(nome,email,senha) VALUES(?,?,?)", usuario.nome(), usuario.email(), usuario.hashSenha());
    }
}
