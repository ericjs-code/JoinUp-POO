package br.com.joinup.usuarios.service;

import br.com.joinup.auth.*;
import br.com.joinup.http.HttpException;
import br.com.joinup.shared.db.ExecutorTransacional;
import br.com.joinup.usuarios.domain.*;
import br.com.joinup.usuarios.dto.*;
import br.com.joinup.usuarios.repository.*;
import java.util.Map;

public final class ContaService {
    private final ExecutorTransacional transacoes;
    private final GerenciadorSessao sessoes;
    private final RepositoriosUsuarios usuarios;
    private final ContaRepositoryJdbc contas;
    private final SenhasPbkdf2 senhas;
    public ContaService(ExecutorTransacional t, GerenciadorSessao s, RepositoriosUsuarios u, ContaRepositoryJdbc c, SenhasPbkdf2 senhas) {
        transacoes = t; sessoes = s; usuarios = u; contas = c; this.senhas = senhas;
    }
    public Usuario identificar(String token, Perfil... perfis) { return transacoes.executar(c -> sessoes.exigir(c, token, perfis)); }
    public Usuario atualizar(String token, PerfilDados dados) {
        return transacoes.executar(c -> {
            Usuario u = sessoes.exigir(c, token, Perfil.PARTICIPANTE, Perfil.ORGANIZADOR);
            if (u.perfil() == Perfil.PARTICIPANTE && dados.cnpj() != null) throw ValidacaoUsuario.invalido("cnpj");
            if (dados.nome() == null && dados.email() == null && dados.nova_senha() == null && dados.cnpj() == null)
                throw ValidacaoUsuario.invalido("perfil vazio");
            String nome = dados.nome() == null ? u.nome() : ValidacaoUsuario.texto(dados.nome(), u.perfil() == Perfil.PARTICIPANTE ? 60 : 80, "nome");
            String email = dados.email() == null ? u.email() : ValidacaoUsuario.email(dados.email(), u.perfil() == Perfil.ORGANIZADOR ? 40 : 45);
            String hash = dados.nova_senha() == null || dados.nova_senha().isEmpty() ? u.hashSenha() : senhas.gerar(ValidacaoUsuario.senha(dados.nova_senha()));
            String cnpj = u instanceof Organizador o ? (dados.cnpj() == null ? o.cnpj() : ValidacaoUsuario.cnpj(dados.cnpj())) : null;
            contas.atualizar(c, u, nome, email, hash, cnpj);
            return usuarios.de(u.perfil()).buscarId(c, u.id(), false);
        });
    }
    public void desativar(String token) {
        transacoes.executar(c -> {
            Usuario u = sessoes.exigir(c, token, Perfil.PARTICIPANTE, Perfil.ORGANIZADOR);
            contas.desativar(c, u);
            if (u.perfil() == Perfil.PARTICIPANTE) contas.cancelarAnuncios(c, u.id());
            sessoes.revogarTodas(c, u);
            return null;
        });
    }
    public void logout(String token) {
        transacoes.executar(c -> { sessoes.exigir(c, token); sessoes.revogar(c, token); return null; });
    }
    public Map<String, Object> listarInativas(String token, Perfil perfil, int pagina, int tamanho) {
        exigirTipoConta(perfil);
        return transacoes.executar(c -> {
            sessoes.exigir(c, token, Perfil.SUPORTE);
            var repo = usuarios.de(perfil);
            var lista = repo.listarInativos(c, tamanho, (pagina - 1) * tamanho).stream()
                    .map(u -> Map.of("id", u.id(), "perfil", u.perfil(), "nome", u.nome(), "email", u.email(), "ativo", false)).toList();
            return Map.of("dados", lista, "pagina", pagina, "tamanho", tamanho, "total", repo.contarInativos(c));
        });
    }
    public Usuario reativar(String token, Perfil perfil, long id) {
        exigirTipoConta(perfil);
        return transacoes.executar(c -> {
            Usuario operador = sessoes.exigir(c, token, Perfil.SUPORTE);
            Usuario alvo = usuarios.de(perfil).buscarId(c, id, true);
            if (alvo == null) throw new HttpException(404, "RECURSO_NAO_ENCONTRADO", "Conta não encontrada");
            if (alvo.ativo()) throw new HttpException(409, "CONTA_JA_ATIVA", "Conta já está ativa");
            contas.reativar(c, alvo, operador.id());
            return usuarios.de(perfil).buscarId(c, id, false);
        });
    }
    private static void exigirTipoConta(Perfil perfil) {
        if (perfil != Perfil.PARTICIPANTE && perfil != Perfil.ORGANIZADOR) throw ValidacaoUsuario.invalido("tipo");
    }
}
