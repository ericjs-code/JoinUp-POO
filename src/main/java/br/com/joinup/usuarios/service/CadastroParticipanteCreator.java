package br.com.joinup.usuarios.service;

import br.com.joinup.auth.SenhasPbkdf2;
import br.com.joinup.shared.db.ExecutorTransacional;
import br.com.joinup.usuarios.domain.*;
import br.com.joinup.usuarios.dto.CadastroDados;
import br.com.joinup.usuarios.repository.ParticipanteRepositoryJdbc;

public final class CadastroParticipanteCreator extends CadastroUsuarioCreator {
    public CadastroParticipanteCreator(ExecutorTransacional t, SenhasPbkdf2 s, ParticipanteRepositoryJdbc r) { super(t, s, r); }
    @Override protected Participante criarUsuario(CadastroDados d, String hash) {
        if (d.cnpj() != null) throw ValidacaoUsuario.invalido("cnpj");
        return new Participante(0, ValidacaoUsuario.texto(d.nome(), 60, "nome"), ValidacaoUsuario.email(d.email(), 45),
                hash, true, ValidacaoUsuario.cpf(d.cpf()), ValidacaoUsuario.nascimento(d.data_nascimento()));
    }
}
