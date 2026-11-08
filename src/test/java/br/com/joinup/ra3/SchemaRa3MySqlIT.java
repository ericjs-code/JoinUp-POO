package br.com.joinup.ra3;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SchemaRa3MySqlIT {
    static final Set<String> TABELAS = Set.of("organizadores", "generos_musicais", "participantes", "eventos",
            "tipos_ingressos", "comunidades", "postagens");

    @Test void schemaInstaladoCorrespondeAoContratoOriginalSemTabelasExtras() throws Exception {
        try (var c = ConexoesRa3IT.abrir()) { verificar(c); }
        System.out.println("RA3_SCHEMA_OK tabelas=7 fks=6 checks=5 colunas_indices_defaults=contrato-RA2");
    }

    /** Compara metadata real com os blocos originais da RA2, lidos somente como referência. */
    static void verificar(Connection c) throws Exception {
        var tabelas = new HashSet<String>();
        try (var s = c.createStatement(); var rs = s.executeQuery(
                "SELECT TABLE_NAME FROM information_schema.tables WHERE TABLE_SCHEMA='joinup_poo_ra3'")) {
            while (rs.next()) tabelas.add(rs.getString(1));
        }
        assertEquals(TABELAS, tabelas, "Schema incompleto ou com tabelas fora do escopo; não executar reset automático");
        String original;
        try (var recurso = SchemaRa3MySqlIT.class.getResourceAsStream("/db/schema_v0.2.sql")) {
            assertNotNull(recurso);
            original = new String(recurso.readAllBytes(), StandardCharsets.UTF_8);
        }
        for (String tabela : TABELAS) {
            var bloco = Pattern.compile("(?ms)^CREATE TABLE IF NOT EXISTS `" + tabela + "` \\(.*?^\\) ENGINE = InnoDB;").matcher(original);
            assertTrue(bloco.find(), tabela);
            verificarColunas(c, tabela, bloco.group());
            verificarIndices(c, tabela, bloco.group());
        }
        var fks = new HashSet<String>();
        try (var s = c.createStatement(); var rs = s.executeQuery("""
                SELECT k.TABLE_NAME,k.COLUMN_NAME,k.REFERENCED_TABLE_NAME,k.REFERENCED_COLUMN_NAME,
                       r.DELETE_RULE,r.UPDATE_RULE
                FROM information_schema.key_column_usage k
                JOIN information_schema.referential_constraints r
                  ON r.CONSTRAINT_SCHEMA=k.CONSTRAINT_SCHEMA AND r.CONSTRAINT_NAME=k.CONSTRAINT_NAME
                WHERE k.CONSTRAINT_SCHEMA='joinup_poo_ra3' AND k.REFERENCED_TABLE_NAME IS NOT NULL
                """)) {
            while (rs.next()) fks.add(rs.getString(1) + "." + rs.getString(2) + "->" + rs.getString(3) + "."
                    + rs.getString(4) + ":" + rs.getString(5) + ":" + rs.getString(6));
        }
        assertEquals(Set.of(
                "eventos.id_organizador->organizadores.id_organizador:NO ACTION:NO ACTION",
                "eventos.id_genero_musical->generos_musicais.id_genero_musical:NO ACTION:NO ACTION",
                "tipos_ingressos.id_evento->eventos.id_evento:NO ACTION:NO ACTION",
                "comunidades.id_evento->eventos.id_evento:CASCADE:CASCADE",
                "postagens.id_participante->participantes.id_participante:NO ACTION:NO ACTION",
                "postagens.id_comunidade->comunidades.id_comunidade:CASCADE:CASCADE"), fks);
        var checks = new HashMap<String, String>();
        try (var s = c.createStatement(); var rs = s.executeQuery("""
                SELECT CONSTRAINT_NAME,CHECK_CLAUSE FROM information_schema.check_constraints
                WHERE CONSTRAINT_SCHEMA='joinup_poo_ra3'
                """)) {
            while (rs.next()) checks.put(rs.getString(1), normalizarCheck(rs.getString(2)));
        }
        assertEquals(Map.of("chk_organizadores_ativo", "ativoin0,1", "chk_participantes_ativo", "ativoin0,1",
                "chk_tipos_valor", "valor>=0", "chk_tipos_estoque", "quantidade_disponivel>=0",
                "chk_tipos_vendido", "quantidade_vendida>=0"), checks);
    }

    private static void verificarColunas(Connection c, String tabela, String ddl) throws Exception {
        var esperadas = new HashMap<String, String>();
        var colunas = Pattern.compile("(?m)^  `([^`]+)` (INT|TINYINT\\(1\\)|VARCHAR\\(\\d+\\)|CHAR\\(\\d+\\)|DECIMAL\\(\\d+,\\d+\\)|DATETIME|ENUM\\([^\\r\\n]+?\\))([^\\r\\n]*)").matcher(ddl);
        while (colunas.find()) {
            String declarado = colunas.group(2).replace(" ", "");
            String tipo = declarado.startsWith("ENUM") ? "enum" + declarado.substring(4)
                    : declarado.toLowerCase(java.util.Locale.ROOT);
            String resto = colunas.group(3);
            var padrao = Pattern.compile("DEFAULT (CURRENT_TIMESTAMP|NULL|[0-9]+)").matcher(resto);
            String valor = padrao.find() ? padrao.group(1).toLowerCase(java.util.Locale.ROOT) : "null";
            esperadas.put(colunas.group(1), tipo + ":" + (resto.contains("NOT NULL") ? "NO" : "YES")
                    + ":" + valor + ":" + resto.contains("AUTO_INCREMENT"));
        }
        var atuais = new HashMap<String, String>();
        try (var ps = c.prepareStatement("""
                SELECT COLUMN_NAME,COLUMN_TYPE,IS_NULLABLE,COLUMN_DEFAULT,EXTRA
                FROM information_schema.columns WHERE TABLE_SCHEMA='joinup_poo_ra3' AND TABLE_NAME=?
                """)) {
            ps.setString(1, tabela);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) atuais.put(rs.getString(1), rs.getString(2) + ":" + rs.getString(3) + ":"
                        + String.valueOf(rs.getString(4)).toLowerCase(java.util.Locale.ROOT).replace("()", "") + ":"
                        + rs.getString(5).contains("auto_increment"));
            }
        }
        assertFalse(esperadas.isEmpty());
        assertEquals(esperadas, atuais, "Contrato de colunas divergente: " + tabela);
    }

    private static void verificarIndices(Connection c, String tabela, String ddl) throws Exception {
        var esperados = new HashSet<String>();
        var indices = Pattern.compile("(?m)^  (PRIMARY KEY|UNIQUE INDEX|INDEX)(?: `([^`]+)`)? \\(`([^`]+)`(?: ASC)?\\)").matcher(ddl);
        while (indices.find()) {
            String nome = indices.group(1).equals("PRIMARY KEY") ? "PRIMARY"
                    : indices.group(2) == null ? indices.group(3) : indices.group(2);
            esperados.add(nome + ":" + indices.group(3) + ":" + !indices.group(1).equals("INDEX"));
        }
        var atuais = new HashSet<String>();
        try (var ps = c.prepareStatement("""
                SELECT INDEX_NAME,COLUMN_NAME,NON_UNIQUE FROM information_schema.statistics
                WHERE TABLE_SCHEMA='joinup_poo_ra3' AND TABLE_NAME=?
                """)) {
            ps.setString(1, tabela);
            try (var rs = ps.executeQuery()) { while (rs.next()) atuais.add(rs.getString(1) + ":" + rs.getString(2) + ":" + !rs.getBoolean(3)); }
        }
        assertEquals(esperados, atuais, "Contrato de índices divergente: " + tabela);
    }

    private static String normalizarCheck(String texto) { return texto.toLowerCase(java.util.Locale.ROOT).replaceAll("[\\s`()]+", ""); }
}
