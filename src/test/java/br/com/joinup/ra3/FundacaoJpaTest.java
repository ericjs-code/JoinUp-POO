package br.com.joinup.ra3;

import br.com.joinup.ra3.modelo.GeneroMusical;
import br.com.joinup.ra3.persistencia.ConfiguracaoRa3;
import jakarta.persistence.Entity;
import jakarta.persistence.Persistence;
import java.util.Collections;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FundacaoJpaTest {
    @Test void descriptorExplicitoSemGeracaoDeSchemaOuCredenciais() throws Exception {
        try (var xml = getClass().getClassLoader().getResourceAsStream("META-INF/persistence.xml")) {
            assertNotNull(xml);
            var parser = DocumentBuilderFactory.newInstance();
            parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            var doc = parser.newDocumentBuilder().parse(xml);
            assertEquals("joinup-ra3", doc.getElementsByTagName("persistence-unit").item(0).getAttributes().getNamedItem("name").getNodeValue());
            assertEquals("org.eclipse.persistence.jpa.PersistenceProvider", doc.getElementsByTagName("provider").item(0).getTextContent());
            assertEquals("META-INF/orm-ra3.xml", doc.getElementsByTagName("mapping-file").item(0).getTextContent());
            assertEquals(7, doc.getElementsByTagName("class").getLength());
            var classes = new java.util.HashSet<String>();
            for (int i = 0; i < 7; i++) classes.add(doc.getElementsByTagName("class").item(i).getTextContent());
            assertEquals(java.util.Set.of("GeneroMusical", "Organizador", "Participante", "Evento", "TipoIngresso", "Comunidade", "Postagem")
                    .stream().map(n -> "br.com.joinup.ra3.modelo." + n).collect(java.util.stream.Collectors.toSet()), classes);
            assertEquals("true", doc.getElementsByTagName("exclude-unlisted-classes").item(0).getTextContent());
            var propriedades = doc.getElementsByTagName("property");
            var esperadas = Map.of("jakarta.persistence.schema-generation.database.action", "none",
                    "jakarta.persistence.schema-generation.scripts.action", "none", "eclipselink.ddl-generation", "none",
                    "eclipselink.logging.level", "OFF", "eclipselink.logging.parameters", "false");
            var encontradas = new java.util.HashMap<String, String>();
            for (int i = 0; i < propriedades.getLength(); i++) {
                var a = propriedades.item(i).getAttributes();
                encontradas.put(a.getNamedItem("name").getNodeValue(), a.getNamedItem("value").getNodeValue());
            }
            esperadas.forEach((nome, valor) -> assertEquals(valor, encontradas.get(nome), nome));
            assertFalse(encontradas.containsKey("jakarta.persistence.jdbc.password"));
            assertFalse(encontradas.containsKey("jakarta.persistence.jdbc.url"));
        }
    }

    @Test void providerCarregaDescriptorSemExigirLoginMySql() throws Exception {
        var configuracao = ConfiguracaoRa3.doAmbiente(Map.of(
                "JOINUP_RA3_JDBC_URL", "jdbc:mysql://127.0.0.1:1/joinup_poo_ra3",
                "JOINUP_RA3_DB_USER", "nao-conecta", "JOINUP_RA3_DB_PASSWORD", "fixture"));
        // EclipseLink posterga login até createEntityManager. Isto não atesta persistência real.
        try (var fabrica = Persistence.createEntityManagerFactory("joinup-ra3", configuracao.propriedadesJpa())) {
            assertTrue(fabrica.isOpen());
            assertTrue(fabrica.getClass().getName().startsWith("org.eclipse.persistence."));
        }
        var api = Collections.list(Entity.class.getClassLoader().getResources("jakarta/persistence/Entity.class"));
        assertEquals(1, api.size(), "Uma única API Jakarta Persistence no classpath");
        assertTrue(api.getFirst().toString().contains("jakarta.persistence-api-3.1.0.jar"));
        assertTrue(org.eclipse.persistence.Version.getVersion().startsWith("4.0.4"));
    }

    @Test void nomeRespeitaLimiteSqlEMutacaoControlada() {
        var genero = new GeneroMusical("Rock");
        assertNull(genero.getId());
        genero.renomear("MPB");
        assertEquals("MPB", genero.getNome());
        assertThrows(IllegalArgumentException.class, () -> genero.renomear(null));
        assertThrows(IllegalArgumentException.class, () -> genero.renomear(" "));
        assertThrows(IllegalArgumentException.class, () -> genero.renomear("a".repeat(46)));
        assertEquals("MPB", genero.getNome());
        assertDoesNotThrow(() -> new GeneroMusical("🎵".repeat(45)));
    }
}
