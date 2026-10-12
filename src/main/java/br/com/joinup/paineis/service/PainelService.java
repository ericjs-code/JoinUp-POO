package br.com.joinup.paineis.service;

import br.com.joinup.auth.GerenciadorSessao;
import br.com.joinup.paineis.dto.PainelView;
import br.com.joinup.paineis.repository.PainelRepositoryJdbc;
import br.com.joinup.shared.db.ExecutorTransacional;
import br.com.joinup.usuarios.domain.Perfil;
import java.sql.Connection;
import java.time.Clock;
import java.util.List;

public final class PainelService {
    private final ExecutorTransacional transacoes;
    private final GerenciadorSessao sessoes;
    private final PainelRepositoryJdbc repo;
    private final Clock clock;
    public PainelService(ExecutorTransacional transacoes, GerenciadorSessao sessoes, PainelRepositoryJdbc repo, Clock clock) {
        this.transacoes=transacoes; this.sessoes=sessoes; this.repo=repo; this.clock=clock;
    }
    public PainelView consultar(String token, Perfil perfil) {
        return transacoes.executar(c -> {
            // Um snapshot de leitura para os indicadores e listas da mesma resposta.
            c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            var u=sessoes.exigir(c,token,perfil);
            var agora=clock.instant();
            var indicadores=switch(perfil) {
                case PARTICIPANTE -> repo.participante(c,u.id(),agora);
                case ORGANIZADOR -> repo.organizador(c,u.id(),agora);
                case SUPORTE -> repo.suporte(c,u.id());
            };
            var eventos=switch(perfil) {
                case PARTICIPANTE -> repo.proximos(c,u.id(),agora);
                case ORGANIZADOR -> repo.eventos(c,u.id(),agora);
                case SUPORTE -> List.<java.util.Map<String,Object>>of();
            };
            return new PainelView(perfil,agora,indicadores,repo.chamados(c,u),eventos);
        });
    }
}
