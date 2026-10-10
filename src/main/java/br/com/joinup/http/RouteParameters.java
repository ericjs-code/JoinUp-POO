package br.com.joinup.http;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Valores imutáveis por requisição, sem HttpContext.attributes compartilhados. */
public record RouteParameters(Map<String, String> caminho, Map<String, String> query) {
    public RouteParameters { caminho = Map.copyOf(caminho); query = Map.copyOf(query); }
    public static Map<String, String> lerQuery(URI uri) {
        var valores = new LinkedHashMap<String, String>();
        String raw = uri.getRawQuery();
        if (raw == null || raw.isEmpty()) return Map.of();
        for (String par : raw.split("&", -1)) {
            String[] partes = par.split("=", 2);
            try {
                String chave = decodificar(partes[0]);
                String valor = decodificar(partes.length == 2 ? partes[1] : "");
                if (chave.isEmpty() || valores.putIfAbsent(chave, valor) != null) throw invalido();
            } catch (IllegalArgumentException e) { throw invalido(); }
        }
        return Map.copyOf(valores);
    }
    private static String decodificar(String raw) {
        var bytes = new java.io.ByteArrayOutputStream();
        for (int i = 0; i < raw.length();) {
            char ch = raw.charAt(i);
            if (ch == '%') {
                if (i + 2 >= raw.length()) throw invalido();
                int a = Character.digit(raw.charAt(i + 1), 16), b = Character.digit(raw.charAt(i + 2), 16);
                if (a < 0 || b < 0) throw invalido();
                bytes.write(a * 16 + b); i += 3;
            } else {
                int cp = raw.codePointAt(i);
                bytes.writeBytes(new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8));
                i += Character.charCount(cp);
            }
        }
        try { StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes.toByteArray())); }
        catch (java.nio.charset.CharacterCodingException e) { throw invalido(); }
        String valor = URLDecoder.decode(raw, StandardCharsets.UTF_8);
        if (valor.chars().anyMatch(c -> c < 32 || c == 127)) throw invalido();
        return valor;
    }
    public static long id(String valor) {
        try {
            if (valor == null || !valor.matches("[1-9][0-9]{0,9}")) throw invalido();
            long id = Long.parseLong(valor);
            if (id > Integer.MAX_VALUE) throw invalido();
            return id;
        } catch (NumberFormatException e) { throw invalido(); }
    }
    private static HttpException invalido() { return new HttpException(400, "REQUISICAO_INVALIDA", "Parâmetros de requisição inválidos"); }
}
