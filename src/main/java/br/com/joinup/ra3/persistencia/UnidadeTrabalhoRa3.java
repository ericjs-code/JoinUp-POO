package br.com.joinup.ra3.persistencia;

import br.com.joinup.ra3.servico.FalhaRa3;
import jakarta.persistence.*;
import java.sql.SQLException;
import java.util.function.Function;
import static br.com.joinup.ra3.servico.FalhaRa3.Motivo.*;

/** Fábrica pertence à execução; cada chamada tem seu próprio contexto, sempre fechado. */
public final class UnidadeTrabalhoRa3 {
    private final EntityManagerFactory fabrica;
    public UnidadeTrabalhoRa3(EntityManagerFactory fabrica) { this.fabrica = fabrica; }

    public <T> T ler(Function<EntityManager, T> operacao) { return executar(false, operacao); }
    public <T> T escrever(Function<EntityManager, T> operacao) { return executar(true, operacao); }

    private <T> T executar(boolean escrita, Function<EntityManager, T> operacao) {
        try (var em = fabrica.createEntityManager()) {
            PersistenciaRa3.verificarCatalogo(em);
            var tx = em.getTransaction();
            if (escrita) tx.begin();
            try {
                T resultado = operacao.apply(em);
                if (escrita) tx.commit();
                return resultado;
            } catch (RuntimeException | Error falha) {
                if (tx.isActive()) {
                    try { tx.rollback(); } catch (RuntimeException ignored) { /* Não publicar detalhes do driver. */ }
                }
                throw falha;
            }
        } catch (FalhaRa3 e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Campos inválidos. Verifique valores e referências.");
        } catch (RuntimeException e) {
            throw traduzir(e);
        }
    }

    public static FalhaRa3 traduzir(RuntimeException erro) {
        for (Throwable causa = erro; causa != null; causa = causa.getCause()) {
            if (causa instanceof SQLException sql) {
                return switch (sql.getErrorCode()) {
                    case 1062 -> new FalhaRa3(DUPLICIDADE, "Registro duplicado: verifique e-mail, documento ou evento exclusivo da comunidade.");
                    case 1451, 1452, 1048 -> new FalhaRa3(VINCULO, "Vínculo inválido ou registro ainda associado. Nenhuma alteração foi confirmada.");
                    case 1213, 1205 -> new FalhaRa3(CONCORRENCIA, "Operação concorrente. Atualize a consulta e tente novamente.");
                    default -> new FalhaRa3(BANCO, "Não foi possível concluir a operação. Verifique os campos e a disponibilidade do banco.");
                };
            }
        }
        if (erro instanceof PessimisticLockException || erro instanceof LockTimeoutException) {
            return new FalhaRa3(CONCORRENCIA, "Operação concorrente. Atualize a consulta e tente novamente.");
        }
        return new FalhaRa3(BANCO, "Não foi possível concluir a operação no banco RA3.");
    }
}
