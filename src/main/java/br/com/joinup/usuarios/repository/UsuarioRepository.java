package br.com.joinup.usuarios.repository;

import br.com.joinup.usuarios.domain.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface UsuarioRepository {
    Perfil perfil();
    long salvar(Connection c, Usuario usuario) throws SQLException;
    Usuario buscarEmail(Connection c, String email, boolean bloquear) throws SQLException;
    Usuario buscarId(Connection c, long id, boolean bloquear) throws SQLException;
    List<Usuario> listarInativos(Connection c, int limite, int offset) throws SQLException;
    long contarInativos(Connection c) throws SQLException;
}
