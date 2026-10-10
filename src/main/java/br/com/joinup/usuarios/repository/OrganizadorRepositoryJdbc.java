package br.com.joinup.usuarios.repository;

import br.com.joinup.usuarios.domain.*;
import java.sql.*;

public final class OrganizadorRepositoryJdbc extends UsuarioRepositoryJdbc {
    public OrganizadorRepositoryJdbc() { super(Perfil.ORGANIZADOR); }
    @Override protected Organizador mapear(ResultSet rs) throws SQLException {
        return new Organizador(rs.getLong("id_organizador"), rs.getString("nome"), rs.getString("email"),
                rs.getString("senha"), rs.getBoolean("ativo"), rs.getString("cnpj"));
    }
    @Override public long salvar(Connection c, Usuario usuario) throws SQLException {
        var o = (Organizador) usuario;
        return inserir(c, "INSERT INTO organizadores(nome,email,senha,cnpj) VALUES(?,?,?,?)", o.nome(), o.email(), o.hashSenha(), o.cnpj());
    }
}
