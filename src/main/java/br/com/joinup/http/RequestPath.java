package br.com.joinup.http;

import java.net.URI;

public final class RequestPath {
    private RequestPath() { }

    public static String validar(URI uri) {
        String caminho = uri.getPath();
        if (caminho == null || !caminho.startsWith("/") || caminho.contains("\\")
                || caminho.contains("%") || caminho.contains("//")
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
