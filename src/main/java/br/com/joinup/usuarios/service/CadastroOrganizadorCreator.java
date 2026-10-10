package br.com.joinup.usuarios.service;

import br.com.joinup.auth.SenhasPbkdf2;
import br.com.joinup.shared.db.ExecutorTransacional;
import br.com.joinup.usuarios.domain.*;
import br.com.joinup.usuarios.dto.CadastroDados;
import br.com.joinup.usuarios.repository.OrganizadorRepositoryJdbc;

public final class CadastroOrganizadorCreator extends CadastroUsuarioCreator {
    public CadastroOrganizadorCreator(ExecutorTransacional t, SenhasPbkdf2 s, OrganizadorRepositoryJdbc r) { super(t, s, r); }
    @Override protected Organizador criarUsuario(CadastroDados d, String hash) {
        if (d.cpf() != null || d.data_nascimento() != null) throw ValidacaoUsuario.invalido("perfil");
        return new Organizador(0, ValidacaoUsuario.texto(d.nome(), 80, "nome"), ValidacaoUsuario.email(d.email(), 40),
                hash, true, ValidacaoUsuario.cnpj(d.cnpj()));
    }
}
