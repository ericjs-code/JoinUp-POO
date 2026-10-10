package br.com.joinup.auth;

import br.com.joinup.shared.db.ExecutorTransacional;
import br.com.joinup.usuarios.domain.*;
import br.com.joinup.usuarios.repository.SuporteRepositoryJdbc;
import java.sql.*;

public final class AutenticacaoSuporte extends AutenticacaoTemplate {
    private final SuporteRepositoryJdbc usuarios;
    public AutenticacaoSuporte(ExecutorTransacional t, VerificadorSenha v, GerenciadorSessao s, SuporteRepositoryJdbc usuarios) {
        super(t, v, s); this.usuarios = usuarios;
    }
    @Override protected Usuario buscarUsuario(Connection c, String email) throws SQLException { return usuarios.buscarEmail(c, email, true); }
    @Override protected void validarRegraDoPerfil(Usuario usuario) {
        if (!(usuario instanceof UsuarioSuporte)) throw new IllegalStateException("Identidade de suporte inconsistente");
    }
}
