package br.com.joinup.http;

import java.net.URI;

public final class RequestPath {
    private RequestPath() { }

    public static String validar(URI uri) {
        String raw = uri.getRawPath();
        if (raw != null && raw.toLowerCase(java.util.Locale.ROOT).matches(".*%(?:2f|5c).*")) throw invalido();
        String caminho = uri.getPath();
        if (caminho == null || !caminho.startsWith("/") || caminho.contains("\\")
                || caminho.contains("%") || caminho.contains("//") || caminho.contains("\uFFFD")
                || caminho.chars().anyMatch(c -> c < 32 || c == 127)) throw invalido();
        for (String segmento : caminho.split("/")) {
            if (segmento.equals(".") || segmento.equals("..")) throw invalido();
        }
        return caminho;
    }

    private static HttpException invalido() {
        return new HttpException(400, "REQUISICAO_INVALIDA", "Caminho de requisição inválido");
    }
}
