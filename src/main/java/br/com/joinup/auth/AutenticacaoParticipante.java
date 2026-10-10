package br.com.joinup.auth;

import br.com.joinup.shared.db.ExecutorTransacional;
import br.com.joinup.usuarios.domain.*;
import br.com.joinup.usuarios.repository.ParticipanteRepositoryJdbc;
import java.sql.*;

public final class AutenticacaoParticipante extends AutenticacaoTemplate {
    private final ParticipanteRepositoryJdbc usuarios;
    public AutenticacaoParticipante(ExecutorTransacional t, VerificadorSenha v, GerenciadorSessao s, ParticipanteRepositoryJdbc usuarios) {
        super(t, v, s); this.usuarios = usuarios;
    }
    @Override protected Usuario buscarUsuario(Connection c, String email) throws SQLException { return usuarios.buscarEmail(c, email, true); }
    @Override protected void validarRegraDoPerfil(Usuario usuario) {
        if (!(usuario instanceof Participante)) throw new IllegalStateException("Identidade de participante inconsistente");
    }
}
