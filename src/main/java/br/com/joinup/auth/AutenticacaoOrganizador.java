package br.com.joinup.auth;

import br.com.joinup.shared.db.ExecutorTransacional;
import br.com.joinup.usuarios.domain.*;
import br.com.joinup.usuarios.repository.OrganizadorRepositoryJdbc;
import java.sql.*;

public final class AutenticacaoOrganizador extends AutenticacaoTemplate {
    private final OrganizadorRepositoryJdbc usuarios;
    public AutenticacaoOrganizador(ExecutorTransacional t, VerificadorSenha v, GerenciadorSessao s, OrganizadorRepositoryJdbc usuarios) {
        super(t, v, s); this.usuarios = usuarios;
    }
    @Override protected Usuario buscarUsuario(Connection c, String email) throws SQLException { return usuarios.buscarEmail(c, email, true); }
    @Override protected void validarRegraDoPerfil(Usuario usuario) {
        if (!(usuario instanceof Organizador)) throw new IllegalStateException("Identidade de organizador inconsistente");
    }
}
