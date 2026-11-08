package br.com.joinup.ra3;

import br.com.joinup.ra3.modelo.*;
import br.com.joinup.ra3.persistencia.DataHoraUtcConverter;
import jakarta.persistence.*;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class EntidadesJpaTest {
    static final Set<Class<?>> ENTIDADES = Set.of(GeneroMusical.class, Organizador.class, Participante.class,
            Evento.class, TipoIngresso.class, Comunidade.class, Postagem.class);
    static final LocalDateTime UTC = LocalDateTime.of(2030, 11, 12, 18, 30, 15);

    static Stream<Class<?>> entidades() { return ENTIDADES.stream(); }

    @ParameterizedTest @MethodSource("entidades")
    void entidadeConcretaComPkIntegerGeradaEConstrutorProtegido(Class<?> classe) throws Exception {
        assertNotNull(classe.getAnnotation(Entity.class));
        assertFalse(classe.isRecord());
        assertFalse(Modifier.isFinal(classe.getModifiers()));
        assertTrue(Modifier.isProtected(classe.getDeclaredConstructor().getModifiers()));
        assertEquals("joinup_poo_ra3", classe.getAnnotation(Table.class).catalog());
        var ids = Arrays.stream(classe.getDeclaredFields()).filter(f -> f.isAnnotationPresent(Id.class)).toList();
        assertEquals(1, ids.size());
        assertEquals(Integer.class, ids.getFirst().getType());
        assertEquals(GenerationType.IDENTITY, ids.getFirst().getAnnotation(GeneratedValue.class).strategy());
        assertTrue(Modifier.isPrivate(ids.getFirst().getModifiers()));
        for (var campo : classe.getDeclaredFields()) {
            if (campo.getType().equals(LocalDateTime.class)) {
                assertEquals(DataHoraUtcConverter.class, campo.getAnnotation(Convert.class).converter());
            }
            var muitos = campo.getAnnotation(ManyToOne.class);
            var um = campo.getAnnotation(OneToOne.class);
            var colecao = campo.getAnnotation(OneToMany.class);
            if (muitos != null) { assertEquals(0, muitos.cascade().length); assertEquals(FetchType.EAGER, muitos.fetch()); }
            if (um != null) { assertEquals(0, um.cascade().length); assertFalse(um.orphanRemoval()); }
            if (colecao != null) { assertEquals(0, colecao.cascade().length); assertFalse(colecao.orphanRemoval()); }
        }
    }

    @Test void joinsObrigatoriosRepresentamObjetosETemNomesExatos() throws Exception {
        var joins = Map.of(
                Evento.class.getDeclaredField("organizador"), "id_organizador",
                Evento.class.getDeclaredField("generoMusical"), "id_genero_musical",
                TipoIngresso.class.getDeclaredField("evento"), "id_evento",
                Comunidade.class.getDeclaredField("evento"), "id_evento",
                Postagem.class.getDeclaredField("participante"), "id_participante",
                Postagem.class.getDeclaredField("comunidade"), "id_comunidade");
        joins.forEach((campo, nome) -> {
            assertTrue(ENTIDADES.contains(campo.getType()));
            assertEquals(nome, campo.getAnnotation(JoinColumn.class).name());
            assertFalse(campo.getAnnotation(JoinColumn.class).nullable());
            if (campo.isAnnotationPresent(ManyToOne.class)) assertFalse(campo.getAnnotation(ManyToOne.class).optional());
            else assertFalse(campo.getAnnotation(OneToOne.class).optional());
        });
        assertTrue(Comunidade.class.getDeclaredField("evento").getAnnotation(JoinColumn.class).unique());
        var inverso = Evento.class.getDeclaredField("tiposIngressos").getAnnotation(OneToMany.class);
        assertEquals("evento", inverso.mappedBy());
        assertEquals(FetchType.EAGER, inverso.fetch());
        assertEquals(8, TipoIngresso.class.getDeclaredField("valor").getAnnotation(Column.class).precision());
        assertEquals(2, TipoIngresso.class.getDeclaredField("valor").getAnnotation(Column.class).scale());
        assertEquals("CHAR(14)", Participante.class.getDeclaredField("nascimentoIso").getAnnotation(Column.class).columnDefinition());
    }

    @Test void colecaoInversaConsistenteProtegidaESemPersistenciaAutomatica() {
        var evento = evento();
        var tipo = new TipoIngresso("Pista", new BigDecimal("80.00"), 50, evento);
        assertSame(evento, tipo.getEvento());
        assertEquals(java.util.List.of(tipo), evento.getTiposIngressos());
        assertThrows(UnsupportedOperationException.class, () -> evento.getTiposIngressos().clear());
        assertThrows(IllegalArgumentException.class, () -> new TipoIngresso("Inválido", new BigDecimal("-1"), 1, evento));
        assertEquals(1, evento.getTiposIngressos().size(), "Falha não deixa associação parcial");
        assertNull(evento.getId());
        assertNull(tipo.getId());
    }

    @ParameterizedTest @ValueSource(strings = {"-0.01", "1000000.00", "1.001"})
    void valorRecusaNegativoOverflowOuArredondamentoImplicito(String valor) {
        assertThrows(IllegalArgumentException.class, () -> new TipoIngresso("Pista", new BigDecimal(valor), 0, evento()));
    }

    @Test void valorEQuantidadesMantemLimitesSemImplementarVenda() {
        var tipo = new TipoIngresso("Pista", BigDecimal.ZERO, 0, evento());
        assertEquals(new BigDecimal("0.00"), tipo.getValor());
        tipo.definirValor(new BigDecimal("999999.99"));
        assertThrows(IllegalArgumentException.class, () -> tipo.definirValor(null));
        assertThrows(IllegalArgumentException.class, () -> tipo.definirQuantidades(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> tipo.definirQuantidades(0, -1));
        assertEquals(0, tipo.getQuantidadeDisponivel());
        assertEquals(0, tipo.getQuantidadeVendida());
    }

    @Test void referenciasObrigatoriasSaoRecusadasSemCriarEntidadesImplicitas() {
        var evento = evento();
        assertThrows(IllegalArgumentException.class, () -> new Evento("E", UTC, "D", "L", "C", "SP", "12345678", null, new GeneroMusical("G")));
        assertThrows(IllegalArgumentException.class, () -> new Evento("E", UTC, "D", "L", "C", "SP", "12345678", organizador(), null));
        assertThrows(IllegalArgumentException.class, () -> new TipoIngresso("T", BigDecimal.ONE, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new Comunidade("C", null, UTC));
        assertThrows(IllegalArgumentException.class, () -> new Postagem(null, null, null, new Comunidade("C", evento, UTC), UTC));
        assertThrows(IllegalArgumentException.class, () -> new Postagem(null, null, participante(), null, UTC));
    }

    @Test void tamanhosNulosUfENascimentoRespeitamContratoDoSchema() {
        assertThrows(IllegalArgumentException.class, () -> new Organizador("a".repeat(81), "e", "HASH_FIXTURE", "cnpj"));
        assertThrows(IllegalArgumentException.class, () -> new Organizador("O", "a".repeat(41), "HASH_FIXTURE", "cnpj"));
        assertThrows(IllegalArgumentException.class, () -> new Participante("a".repeat(61), "cpf", LocalDate.of(2000, 1, 2), "e", "HASH_FIXTURE"));
        assertThrows(IllegalArgumentException.class, () -> new Participante("P", "a".repeat(12), LocalDate.of(2000, 1, 2), "e", "HASH_FIXTURE"));
        assertThrows(IllegalArgumentException.class, () -> new Participante("P", "cpf", null, "e", "HASH_FIXTURE"));
        assertThrows(IllegalArgumentException.class, () -> new Evento("E", UTC, "D", "L", "C", "XX", "12345678", organizador(), new GeneroMusical("G")));
        var comunidade = new Comunidade("C", evento(), UTC.plusNanos(999));
        assertEquals(UTC, comunidade.getDataCriacao());
        assertThrows(IllegalArgumentException.class, () -> comunidade.definirDescricao("a".repeat(301)));
        assertThrows(IllegalArgumentException.class, () -> comunidade.definirImagem("a".repeat(256)));
        var postagem = new Postagem(null, null, participante(), comunidade, UTC);
        assertNull(postagem.getDescricao()); // O SQL não impõe texto ou imagem nesta entrega.
        assertThrows(IllegalArgumentException.class, () -> postagem.alterarConteudo("a".repeat(501), null));
        assertEquals(LocalDate.of(2000, 1, 2), participante().getDataNascimento());
    }

    @Test void conversaoTemporalUsaInstanteUtcSemDependerDoFusoDaJvm() {
        var conversor = new DataHoraUtcConverter();
        var jdbc = conversor.convertToDatabaseColumn(UTC.plusNanos(999));
        assertEquals(java.time.Instant.parse("2030-11-12T18:30:15Z"), jdbc.toInstant());
        assertEquals(UTC, conversor.convertToEntityAttribute(jdbc));
        assertNull(conversor.convertToDatabaseColumn(null));
        assertNull(conversor.convertToEntityAttribute(null));
    }

    static Organizador organizador() { return new Organizador("Organizador", "o@fixture.test", "HASH_FICTICIO_SEM_AUTENTICACAO", "00.000.000/0000-00"); }
    static Participante participante() { return new Participante("Participante", "00000000000", LocalDate.of(2000, 1, 2), "p@fixture.test", "HASH_FICTICIO_SEM_AUTENTICACAO"); }
    static Evento evento() { return new Evento("Evento", UTC, "Descrição", "Local", "Cidade", "SP", "12345678", organizador(), new GeneroMusical("Gênero")); }
}
