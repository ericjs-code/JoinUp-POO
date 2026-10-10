package br.com.joinup.usuarios.service;

import br.com.joinup.auth.*;
import br.com.joinup.shared.db.ExecutorTransacional;
import br.com.joinup.usuarios.domain.Usuario;
import br.com.joinup.usuarios.dto.CadastroDados;
import br.com.joinup.usuarios.repository.UsuarioRepository;
import java.sql.*;

/** Creator: o fluxo comum usa o produto polimórfico, sem switch de construtores. */
public abstract class CadastroUsuarioCreator {
    private final ExecutorTransacional transacoes;
    private final SenhasPbkdf2 senhas;
    private final UsuarioRepository usuarios;
    protected CadastroUsuarioCreator(ExecutorTransacional t, SenhasPbkdf2 senhas, UsuarioRepository usuarios) {
        this.transacoes = t; this.senhas = senhas; this.usuarios = usuarios;
    }
    public final Usuario cadastrar(CadastroDados dados, String tokenOperador) {
        return transacoes.executar(c -> {
            autorizar(c, tokenOperador);
            // O concrete creator valida os campos do perfil antes de qualquer INSERT.
            Usuario produto = criarUsuario(dados, senhas.gerar(ValidacaoUsuario.senha(dados.senha())));
            produto.atribuirId(usuarios.salvar(c, produto));
            return produto;
        });
    }
    protected void autorizar(Connection c, String tokenOperador) throws SQLException { }
    protected abstract Usuario criarUsuario(CadastroDados dados, String hashSenha);
}
