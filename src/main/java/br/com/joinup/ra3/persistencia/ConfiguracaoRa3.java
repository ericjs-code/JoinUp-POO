package br.com.joinup.ra3.persistencia;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Pattern;

/** Configuração exclusiva do RA3. Não usa fallbacks para o ambiente da aplicação web. */
public final class ConfiguracaoRa3 {
    public static final String CATALOGO = "joinup_poo_ra3";
    private static final Pattern URL = Pattern.compile(
            "jdbc:mysql://([a-zA-Z0-9](?:[a-zA-Z0-9.-]*[a-zA-Z0-9])?)(?::([0-9]{1,5}))?/joinup_poo_ra3(?:\\?(.+))?");
    private static final Map<String, Pattern> PARAMETROS = Map.of(
            "connectTimeout", Pattern.compile("[1-9][0-9]{0,4}"),
            "socketTimeout", Pattern.compile("[1-9][0-9]{0,4}"),
            "connectionTimeZone", Pattern.compile("UTC"),
            "forceConnectionTimeZoneToSession", Pattern.compile("true"),
            "characterEncoding", Pattern.compile("UTF-8|utf8"),
            "useSSL", Pattern.compile("true|false"),
            "allowPublicKeyRetrieval", Pattern.compile("true|false"));
    private final String url;
    private final String usuario;
    private final String senha;

    private ConfiguracaoRa3(String url, String usuario, String senha) {
        validarUrl(url);
        this.url = url;
        this.usuario = usuario;
        this.senha = senha;
    }

    public static ConfiguracaoRa3 doAmbiente(Map<String, String> ambiente) {
        return new ConfiguracaoRa3(obrigatorio(ambiente, "JOINUP_RA3_JDBC_URL"),
                obrigatorio(ambiente, "JOINUP_RA3_DB_USER"),
                obrigatorio(ambiente, "JOINUP_RA3_DB_PASSWORD"));
    }

    private static String obrigatorio(Map<String, String> ambiente, String chave) {
        String valor = ambiente.get(chave);
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException("Configure " + chave + ".");
        return valor;
    }

    public static void validarUrl(String url) {
        var m = URL.matcher(url == null ? "" : url);
        if (!m.matches() || (m.group(2) != null && (Integer.parseInt(m.group(2)) < 1
                || Integer.parseInt(m.group(2)) > 65535))) throw urlInvalida();
        if (m.group(3) != null) {
            var vistos = new HashSet<String>();
            for (String parametro : m.group(3).split("&", -1)) {
                String[] partes = parametro.split("=", -1);
                if (partes.length != 2 || !vistos.add(partes[0]) || !PARAMETROS.containsKey(partes[0])
                        || !PARAMETROS.get(partes[0]).matcher(partes[1]).matches()) throw urlInvalida();
            }
        }
    }

    private static IllegalArgumentException urlInvalida() {
        return new IllegalArgumentException("Destino RA3 recusado: use URL MySQL simples para joinup_poo_ra3 e parâmetros permitidos.");
    }

    public Map<String, Object> propriedadesJpa() {
        var p = new HashMap<String, Object>();
        // Limites também quando a URL não os fornece; a URL permitida tem precedência no driver.
        p.put("jakarta.persistence.jdbc.url", url);
        p.put("jakarta.persistence.jdbc.user", usuario);
        p.put("jakarta.persistence.jdbc.password", senha);
        p.put("eclipselink.jdbc.property.connectTimeout", "5000");
        p.put("eclipselink.jdbc.property.socketTimeout", "10000");
        return p;
    }

    @Override public String toString() { return "ConfiguracaoRa3[catalogo=joinup_poo_ra3, credenciais omitidas]"; }
}
