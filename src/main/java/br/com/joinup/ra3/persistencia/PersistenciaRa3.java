package br.com.joinup.ra3.persistencia;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public final class PersistenciaRa3 {
    private PersistenciaRa3() { }

    /** Força login e verifica o catálogo real sem DDL ou escrita. Chamador fecha a fábrica. */
    public static EntityManagerFactory abrir(ConfiguracaoRa3 configuracao) {
        EntityManagerFactory fabrica = null;
        try {
            fabrica = Persistence.createEntityManagerFactory("joinup-ra3", configuracao.propriedadesJpa());
            try (var em = fabrica.createEntityManager()) { verificarCatalogo(em); }
            return fabrica;
        } catch (RuntimeException e) {
            if (fabrica != null && fabrica.isOpen()) {
                try { fabrica.close(); } catch (RuntimeException ignored) { /* Erro já sanitizado abaixo. */ }
            }
            // Não encadear exceções do driver/provider: podem conter URL e segredo.
            throw new IllegalStateException("RA3_CONEXAO_FALHOU: confira MySQL, credenciais, permissões e schema joinup_poo_ra3.");
        }
    }

    /** Executar na mesma unidade de trabalho antes de qualquer escrita. */
    public static void verificarCatalogo(EntityManager em) {
        Object catalogo = em.createNativeQuery("SELECT DATABASE()").getSingleResult();
        if (!ConfiguracaoRa3.CATALOGO.equals(catalogo)) {
            throw new IllegalStateException("RA3_CATALOGO_RECUSADO: conexão fora do schema exclusivo.");
        }
    }
}
