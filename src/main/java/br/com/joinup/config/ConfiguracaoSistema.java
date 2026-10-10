package br.com.joinup.config;

import java.net.URI;

/** Configuração imutável do processo; não guarda conexões ou estado de negócio. */
public final class ConfiguracaoSistema {
    private final String hostHttp;
    private final int portaHttp;
    private final int threadsHttp;
    private final int limiteRequisicaoBytes;
    private final boolean modoDemo;
    private final String urlJdbc;
    private final String usuarioBanco;
    private final String senhaBanco;

    private ConfiguracaoSistema() {
        hostHttp = valor("joinup.http.host", "JOINUP_HTTP_HOST", "127.0.0.1");
        if (hostHttp.isBlank()) throw new IllegalArgumentException("Host HTTP obrigatório");
        portaHttp = inteiro("joinup.http.port", "JOINUP_HTTP_PORT", 8080, 1, 65535);
        threadsHttp = inteiro("joinup.http.threads", "JOINUP_HTTP_THREADS", 8, 1, 64);
        limiteRequisicaoBytes = inteiro("joinup.maxRequestBytes", "JOINUP_MAX_REQUEST_BYTES", 65536, 1, 1048576);
        String demo = valor("joinup.demo", "JOINUP_DEMO", "false");
        if (!demo.equalsIgnoreCase("true") && !demo.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException("Modo demo deve ser true ou false");
        }
        modoDemo = Boolean.parseBoolean(demo);
        urlJdbc = valor("joinup.jdbc.url", "JOINUP_JDBC_URL",
                "jdbc:mysql://127.0.0.1:3306/joinup_poo?connectionTimeZone=UTC&connectTimeout=5000&socketTimeout=10000");
        validarUrlJdbc(urlJdbc);
        usuarioBanco = valor("joinup.db.user", "JOINUP_DB_USER", "");
        senhaBanco = valor("joinup.db.password", "JOINUP_DB_PASSWORD", "");
    }

    private static class Holder {
        private static final ConfiguracaoSistema INSTANCIA = new ConfiguracaoSistema();
    }

    public static ConfiguracaoSistema getInstancia() { return Holder.INSTANCIA; }

    private static String valor(String propriedade, String ambiente, String padrao) {
        String valor = System.getProperty(propriedade);
        if (valor == null) valor = System.getenv(ambiente);
        return valor == null ? padrao : valor;
    }

    private static int inteiro(String propriedade, String ambiente, int padrao, int minimo, int maximo) {
        try {
            int resultado = Integer.parseInt(valor(propriedade, ambiente, Integer.toString(padrao)));
            if (resultado >= minimo && resultado <= maximo) return resultado;
        } catch (NumberFormatException ignored) {
            // Mensagem contém apenas a chave, sem revelar o conteúdo da configuração.
        }
        throw new IllegalArgumentException("Configuração inválida: " + propriedade);
    }

    public static void validarUrlJdbc(String url) {
        try {
            if (url == null || !url.startsWith("jdbc:mysql://")) throw new IllegalArgumentException();
            URI destino = URI.create(url.substring(5));
            if (destino.getHost() == null || destino.getUserInfo() != null || destino.getFragment() != null
                    || !"/joinup_poo".equals(destino.getRawPath())) throw new IllegalArgumentException();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("URL JDBC deve apontar exclusivamente para joinup_poo");
        }
    }

    public String getHostHttp() { return hostHttp; }
    public int getPortaHttp() { return portaHttp; }
    public int getThreadsHttp() { return threadsHttp; }
    public int getLimiteRequisicaoBytes() { return limiteRequisicaoBytes; }
    public boolean isModoDemo() { return modoDemo; }
    public String getUrlJdbc() { return urlJdbc; }
    public String getUsuarioBanco() { return usuarioBanco; }
    public String getSenhaBanco() { return senhaBanco; }
}
