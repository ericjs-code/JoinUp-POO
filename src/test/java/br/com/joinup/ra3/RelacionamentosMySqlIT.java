package br.com.joinup.ra3;

import br.com.joinup.ra3.modelo.*;
import br.com.joinup.ra3.persistencia.PersistenciaRa3;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class RelacionamentosMySqlIT {
    private static final LocalDateTime UTC = LocalDateTime.of(2030, 11, 12, 18, 30, 15);
    private static EntityManagerFactory fabrica;
    private final String token = UUID.randomUUID().toString().replace("-", "");
    private final String marca = "r1-" + token;
    private final String cpf = new java.math.BigInteger(token, 16).toString().substring(0, 11);
    private final String cnpj = new java.math.BigInteger(token, 16).toString().substring(0, 18);
    private GeneroMusical genero;
    private Organizador organizador;
    private Participante participante;
    private Evento evento;
    private TipoIngresso tipo;
    private Comunidade comunidade;
    private Postagem postagem;

    @BeforeAll static void abrir() throws Exception {
        try (var c = ConexoesRa3IT.abrir()) { SchemaRa3MySqlIT.verificar(c); }
        fabrica = PersistenciaRa3.abrir(ConexoesRa3IT.configuracao());
    }

    @AfterAll static void fechar() { if (fabrica != null) fabrica.close(); }

    @BeforeEach void persistirGrafo() {
        genero = new GeneroMusical(marca + "g");
        organizador = new Organizador(marca + "o", token + "o@t.test", "HASH_FICTICIO_SEM_AUTENTICACAO", cnpj);
        participante = participante(cpf, token + "p@t.test");
        evento = evento("e");
        tipo = new TipoIngresso(marca + "t", new BigDecimal("125.50"), 20, evento);
        comunidade = new Comunidade(marca + "c", evento, UTC);
        postagem = new Postagem(marca + "p", null, participante, comunidade, UTC);
        transacao(em -> {
            em.persist(genero); em.persist(organizador); em.persist(participante);
            em.persist(evento); em.persist(tipo); em.persist(comunidade); em.persist(postagem);
        });
    }

    @Test void recuperaSeteEntidadesAssociacoesMultiplicidadesEDatasEmOutraConexao() throws Exception {
        var vazio = evento("vazio");
        var segundoTipo = new TipoIngresso(marca + "t2", BigDecimal.ZERO, 0, evento);
        var segundaPostagem = new Postagem(marca + "p2", null, participante, comunidade, UTC);
        transacao(em -> { em.persist(vazio); em.persist(segundoTipo); em.persist(segundaPostagem); });
        Postagem recuperada;
        try (var em = fabrica.createEntityManager()) {
            recuperada = em.find(Postagem.class, postagem.getId());
            assertNotSame(postagem, recuperada);
            assertEquals(participante.getId(), recuperada.getParticipante().getId());
            assertEquals(comunidade.getId(), recuperada.getComunidade().getId());
            var eventoLido = recuperada.getComunidade().getEvento();
            assertEquals(organizador.getId(), eventoLido.getOrganizador().getId());
            assertEquals(genero.getId(), eventoLido.getGeneroMusical().getId());
            assertEquals(UTC, eventoLido.getData());
            assertEquals(UTC, recuperada.getComunidade().getDataCriacao());
            assertEquals(UTC, recuperada.getDataPostagem());
            assertEquals(LocalDate.of(2000, 1, 2), recuperada.getParticipante().getDataNascimento());
            assertEquals(2, eventoLido.getTiposIngressos().size());
            eventoLido.getTiposIngressos().forEach(t -> assertSame(eventoLido, t.getEvento()));
            assertTrue(Persistence.getPersistenceUtil().isLoaded(eventoLido, "tiposIngressos"));
            assertEquals(0, em.find(Evento.class, vazio.getId()).getTiposIngressos().size());
            assertEquals(0L, em.createQuery("SELECT COUNT(c) FROM Comunidade c WHERE c.evento.id=:id", Long.class).setParameter("id", vazio.getId()).getSingleResult());
            assertEquals(2L, em.createQuery("SELECT COUNT(p) FROM Postagem p JOIN p.comunidade c JOIN p.participante a WHERE c.id=:c AND a.id=:a", Long.class)
                    .setParameter("c", comunidade.getId()).setParameter("a", participante.getId()).getSingleResult());
            assertEquals(Set.of(GeneroMusical.class, Organizador.class, Participante.class, Evento.class, TipoIngresso.class, Comunidade.class, Postagem.class),
                    fabrica.getMetamodel().getEntities().stream().map(jakarta.persistence.metamodel.EntityType::getJavaType).collect(java.util.stream.Collectors.toSet()));
            assertEquals("+00:00", em.createNativeQuery("SELECT @@session.time_zone").getSingleResult());
        }
        // EAGER comprovado também após fechar o segundo EntityManager.
        assertEquals(marca + "g", recuperada.getComunidade().getEvento().getGeneroMusical().getNome());
        assertEquals(2, recuperada.getComunidade().getEvento().getTiposIngressos().size());
        try (var c = ConexoesRa3IT.abrir(); var ps = c.prepareStatement("""
                SELECT o.id_organizador,g.id_genero_musical,a.id_participante,e.id_evento,t.id_tipos_ingressos,
                       c.id_comunidade,p.id_postagem,t.valor,e.data,a.data_nascimento,p.data_postagem
                FROM joinup_poo_ra3.postagens p
                JOIN joinup_poo_ra3.participantes a ON a.id_participante=p.id_participante
                JOIN joinup_poo_ra3.comunidades c ON c.id_comunidade=p.id_comunidade
                JOIN joinup_poo_ra3.eventos e ON e.id_evento=c.id_evento
                JOIN joinup_poo_ra3.organizadores o ON o.id_organizador=e.id_organizador
                JOIN joinup_poo_ra3.generos_musicais g ON g.id_genero_musical=e.id_genero_musical
                JOIN joinup_poo_ra3.tipos_ingressos t ON t.id_evento=e.id_evento
                WHERE p.id_postagem=? AND t.id_tipos_ingressos=?
                """)) {
            ps.setInt(1, postagem.getId()); ps.setInt(2, tipo.getId());
            try (var rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(organizador.getId(), rs.getInt(1)); assertEquals(genero.getId(), rs.getInt(2));
                assertEquals(participante.getId(), rs.getInt(3)); assertEquals(evento.getId(), rs.getInt(4));
                assertEquals(tipo.getId(), rs.getInt(5)); assertEquals(comunidade.getId(), rs.getInt(6));
                assertEquals(postagem.getId(), rs.getInt(7)); assertEquals(new BigDecimal("125.50"), rs.getBigDecimal(8));
                assertEquals(UTC, rs.getObject(9, LocalDateTime.class));
                assertEquals("2000-01-02", rs.getString(10).stripTrailing());
                assertEquals(UTC, rs.getObject(11, LocalDateTime.class)); assertFalse(rs.next());
            }
        }
        System.out.println("RA3_GRAFO_OK entidades=7 leitura=outro-EntityManager+JDBC_JOIN EAGER=destacado UTC=sem-conversao evento-sem-comunidade=aceito");
        System.out.println("RA3_GRAFO_IDS genero=" + genero.getId() + " organizador=" + organizador.getId()
                + " participante=" + participante.getId() + " evento=" + evento.getId() + " tipo=" + tipo.getId()
                + " comunidade=" + comunidade.getId() + " postagem=" + postagem.getId());
    }

    @Test void comunidadeUnicaPorEventoEFalhaReverteInsertAnterior() throws Exception {
        falhaSql(1062, "uk_comunidade_evento", em -> {
            em.persist(new GeneroMusical(marca + "rollback"));
            em.flush();
            em.persist(new Comunidade(marca + "duplicada", em.find(Evento.class, evento.getId()), UTC));
            em.flush();
        });
        assertEquals(0, contarJdbc("generos_musicais", "nome", marca + "rollback"));
        assertEquals(1, contarJdbc("comunidades", "id_evento", evento.getId()));
        System.out.println("RA3_ROLLBACK_OK UNIQUE_comunidade_evento=1062 insert_anterior=ausente JDBC=confirmado");
    }

    @ParameterizedTest @ValueSource(strings = {"cpf", "email"})
    void participanteRecusaCpfOuEmailRepetidos(String atributo) {
        falhaSql(1062, atributo.equals("cpf") ? "cpf_UNIQUE" : "email", em -> em.persist(participante(atributo.equals("cpf") ? cpf : "99999999999",
                atributo.equals("email") ? participante.getEmail() : token + "x@t.test")));
        System.out.println("RA3_CONSTRAINT_OK participante_" + atributo + "=UNIQUE codigo=1062");
    }

    @ParameterizedTest @ValueSource(strings = {"email", "cnpj"})
    void organizadorRecusaEmailOuCnpjRepetidos(String atributo) {
        falhaSql(1062, atributo, em -> em.persist(new Organizador(marca + "duplicado",
                atributo.equals("email") ? organizador.getEmail() : token + "x@t.test", "HASH_FICTICIO_SEM_AUTENTICACAO",
                atributo.equals("cnpj") ? cnpj : "999999999999999999")));
        System.out.println("RA3_CONSTRAINT_OK organizador_" + atributo + "=UNIQUE codigo=1062");
    }

    @Test void fkObrigatoriaRecusaNullNoBanco() throws Exception {
        falhaSql(1048, "id_evento", em -> em.createNativeQuery("UPDATE joinup_poo_ra3.tipos_ingressos SET id_evento=NULL WHERE id_tipos_ingressos=?")
                .setParameter(1, tipo.getId()).executeUpdate());
        assertEquals(1, contarJdbc("tipos_ingressos", "id_evento", evento.getId()));
        System.out.println("RA3_CONSTRAINT_OK FK_NOT_NULL=1048 rollback=confirmado");
    }

    @Test void fkRecusaReferenciaInexistente() throws Exception {
        falhaSql(1452, "fk_tipos_ingressos_eventos1", em -> em.createNativeQuery("UPDATE joinup_poo_ra3.tipos_ingressos SET id_evento=-1 WHERE id_tipos_ingressos=?")
                .setParameter(1, tipo.getId()).executeUpdate());
        assertEquals(1, contarJdbc("tipos_ingressos", "id_evento", evento.getId()));
        System.out.println("RA3_CONSTRAINT_OK FK_INEXISTENTE=1452 rollback=confirmado");
    }

    @ParameterizedTest @ValueSource(strings = {"valor", "quantidade_disponivel", "quantidade_vendida"})
    void checksNaoNegativosSaoAplicadosPeloMySql(String coluna) {
        String constraint = java.util.Map.of("valor", "chk_tipos_valor", "quantidade_disponivel", "chk_tipos_estoque", "quantidade_vendida", "chk_tipos_vendido").get(coluna);
        falhaSql(3819, constraint, em -> em.createNativeQuery("UPDATE joinup_poo_ra3.tipos_ingressos SET " + coluna + "=-1 WHERE id_tipos_ingressos=?")
                .setParameter(1, tipo.getId()).executeUpdate());
        System.out.println("RA3_CONSTRAINT_OK CHECK_" + coluna + "=3819");
    }

    private Participante participante(String documento, String email) {
        return new Participante(marca + "a", documento, LocalDate.of(2000, 1, 2), email, "HASH_FICTICIO_SEM_AUTENTICACAO");
    }
    private Evento evento(String sufixo) { return new Evento(marca + sufixo, UTC, marca, "Local", "Cidade", "SP", "12345678", organizador, genero); }

    private static void transacao(java.util.function.Consumer<EntityManager> operacao) {
        try (var em = fabrica.createEntityManager()) {
            var tx = em.getTransaction();
            tx.begin();
            try {
                PersistenciaRa3.verificarCatalogo(em);
                operacao.accept(em);
                tx.commit();
            } catch (RuntimeException | Error e) {
                if (tx.isActive()) {
                    try { tx.rollback(); } catch (RuntimeException rollback) { e.addSuppressed(rollback); }
                }
                throw e;
            }
        }
    }

    @Test void limpezaPreservaRegistroComMarcadorDeOutraExecucao() throws Exception {
        String controle = "controle-" + UUID.randomUUID().toString().replace("-", "");
        var outro = new GeneroMusical(controle);
        transacao(em -> em.persist(outro));
        try {
            limparSomenteFixturesProprias();
            assertEquals(1, contarJdbc("generos_musicais", "nome", controle));
            System.out.println("RA3_LIMPEZA_RESTRITA_OK registro_fora_do_marcador=preservado");
        } finally {
            transacao(em -> em.createQuery("DELETE FROM GeneroMusical g WHERE g.id=:id AND g.nome=:nome")
                    .setParameter("id", outro.getId()).setParameter("nome", controle).executeUpdate());
            assertEquals(0, contarJdbc("generos_musicais", "nome", controle));
        }
    }

    private static void falhaSql(int codigo, String constraint, java.util.function.Consumer<EntityManager> operacao) {
        var erro = assertThrows(PersistenceException.class, () -> transacao(operacao));
        Throwable causa = erro;
        while (causa != null && !(causa instanceof SQLException)) causa = causa.getCause();
        assertInstanceOf(SQLException.class, causa, "Exige violação SQL real, não qualquer exceção");
        assertEquals(codigo, ((SQLException) causa).getErrorCode());
        assertTrue(causa.getMessage().contains(constraint), "Exige a constraint esperada, não outra violação SQL");
    }

    @AfterEach void limparSomenteFixturesProprias() throws Exception {
        transacao(em -> {
            em.createQuery("DELETE FROM Postagem p WHERE p.descricao LIKE :m").setParameter("m", marca + "%").executeUpdate();
            em.createQuery("DELETE FROM Comunidade c WHERE c.nome LIKE :m").setParameter("m", marca + "%").executeUpdate();
            em.createQuery("DELETE FROM TipoIngresso t WHERE t.nome LIKE :m").setParameter("m", marca + "%").executeUpdate();
            em.createQuery("DELETE FROM Evento e WHERE e.nome LIKE :m").setParameter("m", marca + "%").executeUpdate();
            em.createQuery("DELETE FROM Participante p WHERE p.email LIKE :m").setParameter("m", token + "%").executeUpdate();
            em.createQuery("DELETE FROM Organizador o WHERE o.email LIKE :m").setParameter("m", token + "%").executeUpdate();
            em.createQuery("DELETE FROM GeneroMusical g WHERE g.nome LIKE :m").setParameter("m", marca + "%").executeUpdate();
        });
        try (var c = ConexoesRa3IT.abrir()) {
            for (String tabela : SchemaRa3MySqlIT.TABELAS) {
                boolean conta = tabela.equals("participantes") || tabela.equals("organizadores");
                String coluna = conta ? "email" : tabela.equals("postagens") ? "descricao" : tabela.equals("tipos_ingressos") ? "nome_tipo" : "nome";
                try (var ps = c.prepareStatement("SELECT COUNT(*) FROM joinup_poo_ra3." + tabela + " WHERE " + coluna + " LIKE ?")) {
                    ps.setString(1, (conta ? token : marca) + "%");
                    try (var rs = ps.executeQuery()) { assertTrue(rs.next()); assertEquals(0, rs.getInt(1), tabela); }
                }
            }
        }
        System.out.println("RA3_GRAFO_LIMPO tabelas=7 registros_proprios=0 verificacao=JDBC-independente");
    }

    private static int contarJdbc(String tabela, String coluna, Object valor) throws Exception {
        // Identificadores são constantes internas dos testes, não entrada externa.
        try (var c = ConexoesRa3IT.abrir(); var ps = c.prepareStatement("SELECT COUNT(*) FROM joinup_poo_ra3." + tabela + " WHERE " + coluna + "=?")) {
            ps.setObject(1, valor);
            try (var rs = ps.executeQuery()) { assertTrue(rs.next()); return rs.getInt(1); }
        }
    }
}
