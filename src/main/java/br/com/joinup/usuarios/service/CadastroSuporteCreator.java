package br.com.joinup.usuarios.service;

import br.com.joinup.auth.*;
import br.com.joinup.shared.db.ExecutorTransacional;
import br.com.joinup.usuarios.domain.*;
import br.com.joinup.usuarios.dto.CadastroDados;
import br.com.joinup.usuarios.repository.SuporteRepositoryJdbc;
import java.sql.*;

public final class CadastroSuporteCreator extends CadastroUsuarioCreator {
    private final GerenciadorSessao sessoes;
    public CadastroSuporteCreator(ExecutorTransacional t, SenhasPbkdf2 s, SuporteRepositoryJdbc r, GerenciadorSessao sessoes) {
        super(t, s, r); this.sessoes = sessoes;
    }
    @Override protected void autorizar(Connection c, String token) throws SQLException { sessoes.exigir(c, token, Perfil.SUPORTE); }
    @Override protected UsuarioSuporte criarUsuario(CadastroDados d, String hash) {
        if (d.cpf() != null || d.cnpj() != null || d.data_nascimento() != null) throw ValidacaoUsuario.invalido("perfil");
        return new UsuarioSuporte(0, ValidacaoUsuario.texto(d.nome(), 80, "nome"), ValidacaoUsuario.email(d.email(), 45), hash);
    }
}
