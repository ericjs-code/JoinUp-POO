package br.com.joinup.ra3;

import br.com.joinup.ra3.modelo.GeneroMusical;
import br.com.joinup.ra3.persistencia.ConfiguracaoRa3;
import br.com.joinup.ra3.persistencia.PersistenciaRa3;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in; schema manual em instância descartável. Nunca cria/apaga tabelas. */
class GeneroMusicalMySqlIT {
    @Test void persisteLeEmOutroEntityManagerReverteFalhaELimpaSomenteFixture() {
        assertEquals("instancia-descartavel-ra3", System.getenv("JOINUP_RA3_IT_DATABASE"),
                "Confirme explicitamente a instância descartável com JOINUP_RA3_IT_DATABASE.");
        var configuracao = ConfiguracaoRa3.doAmbiente(System.getenv());
        String nome = "ra3-it-" + UUID.randomUUID(); // 43 caracteres, posse exclusiva desta execução.
        String revertido = "ra3-rb-" + UUID.randomUUID();
        try (var fabrica = PersistenciaRa3.abrir(configuracao)) {
            try {
                var genero = new GeneroMusical(nome);
                try (var escrita = fabrica.createEntityManager()) {
                    transacao(escrita, () -> escrita.persist(genero));
                }
                assertNotNull(genero.getId());
                assertTrue(genero.getId() > 0);
                try (var leitura = fabrica.createEntityManager()) {
                    PersistenciaRa3.verificarCatalogo(leitura);
                    var recuperado = leitura.find(GeneroMusical.class, genero.getId());
                    assertNotNull(recuperado);
                    assertNotSame(genero, recuperado);
                    assertEquals(nome, recuperado.getNome());
                    // Consulta SQL real adicional; cache compartilhado desabilitado no descriptor.
                    assertEquals(nome, leitura.createNativeQuery(
                            "SELECT nome FROM joinup_poo_ra3.generos_musicais WHERE id_genero_musical = ?")
                            .setParameter(1, genero.getId()).getSingleResult());
                    assertEquals(7, fabrica.getMetamodel().getEntities().size());
                    System.out.println("RA3_SMOKE_OK provider=" + org.eclipse.persistence.Version.getVersion()
                            + " catalogo=joinup_poo_ra3 id=" + genero.getId() + " leitura=outro-EntityManager+SQL");
                }
                try (var em = fabrica.createEntityManager()) {
                    assertThrows(FalhaFixture.class, () -> transacao(em, () -> {
                        em.persist(new GeneroMusical(revertido));
                        em.flush();
                        throw new FalhaFixture();
                    }));
                }
                try (var em = fabrica.createEntityManager()) {
                    assertEquals(0L, em.createQuery("SELECT COUNT(g) FROM GeneroMusical g WHERE g.nome = :nome", Long.class)
                            .setParameter("nome", revertido).getSingleResult());
                }
            } finally {
                limparFixture(fabrica, nome);
                limparFixture(fabrica, revertido);
            }
        }
    }

    private static void transacao(EntityManager em, Runnable operacao) {
        var tx = em.getTransaction();
        tx.begin();
        try {
            PersistenciaRa3.verificarCatalogo(em);
            operacao.run();
            tx.commit();
        } catch (RuntimeException | Error falha) {
            if (tx.isActive()) {
                try { tx.rollback(); } catch (RuntimeException rollback) { falha.addSuppressed(rollback); }
            }
            throw falha;
        }
    }

    private static void limparFixture(EntityManagerFactory fabrica, String nome) {
        try (var em = fabrica.createEntityManager()) {
            transacao(em, () -> em.createQuery("DELETE FROM GeneroMusical g WHERE g.nome = :nome")
                    .setParameter("nome", nome).executeUpdate());
        }
        try (var em = fabrica.createEntityManager()) {
            assertEquals(0L, em.createQuery("SELECT COUNT(g) FROM GeneroMusical g WHERE g.nome = :nome", Long.class)
                    .setParameter("nome", nome).getSingleResult());
        }
        System.out.println("RA3_FIXTURE_LIMPA: registro próprio ausente em nova consulta.");
    }

    private static final class FalhaFixture extends RuntimeException { }
}
