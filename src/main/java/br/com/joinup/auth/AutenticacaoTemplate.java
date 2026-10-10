package br.com.joinup.auth;

import br.com.joinup.http.HttpException;
import br.com.joinup.shared.db.ExecutorTransacional;
import br.com.joinup.usuarios.domain.Usuario;
import br.com.joinup.usuarios.dto.Credenciais;
import br.com.joinup.usuarios.service.ValidacaoUsuario;
import java.sql.*;

/** Template Method real: todas as rotas de login executam esta sequência final. */
public abstract class AutenticacaoTemplate {
    private final ExecutorTransacional transacoes;
    private final VerificadorSenha senhas;
    private final GerenciadorSessao sessoes;
    protected AutenticacaoTemplate(ExecutorTransacional transacoes, VerificadorSenha senhas, GerenciadorSessao sessoes) {
        this.transacoes = transacoes; this.senhas = senhas; this.sessoes = sessoes;
    }
    public final SessaoEmitida autenticar(Credenciais credenciais) {
        String email = ValidacaoUsuario.email(credenciais.email(), 45);
        String senha = ValidacaoUsuario.senha(credenciais.senha());
        return transacoes.executar(c -> {
            Usuario usuario = buscarUsuario(c, email);
            boolean correta = senhas.verificar(senha, usuario == null ? null : usuario.hashSenha());
            if (usuario == null || !correta) throw new HttpException(401, "CREDENCIAIS_INVALIDAS", "E-mail ou senha incorretos");
            if (!usuario.ativo()) throw new HttpException(403, "CONTA_INATIVA", "Conta desativada");
            validarRegraDoPerfil(usuario);
            return sessoes.criar(c, usuario);
        });
    }
    protected abstract Usuario buscarUsuario(Connection c, String email) throws SQLException;
    protected abstract void validarRegraDoPerfil(Usuario usuario);
}
