package br.com.joinup.arquivos;

import br.com.joinup.http.HttpException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/** Subconjunto multipart explícito: três partes, sem preâmbulo, nesting ou headers extras. */
public record MultipartImagem(String contexto, long alvoId, String nome, String mime, byte[] bytes) {
    public static final int MAX_IMAGEM = 1048576;
    public static final int MAX_CORPO = MAX_IMAGEM + 16384;
    private static final Pattern TIPO = Pattern.compile("multipart/form-data;\\s*boundary=(?:\"([A-Za-z0-9'()+_,./:=?-]{1,70})\"|([A-Za-z0-9'()+_,./:=?-]{1,70}))");
    private static final Pattern DISPOSICAO = Pattern.compile("form-data; name=\"(contexto|alvoId|arquivo)\"(?:; filename=\"([A-Za-z0-9 _.-]{1,100})\")?");
    public static MultipartImagem ler(String tipo, byte[] corpo) {
        var m = TIPO.matcher(tipo == null ? "" : tipo);
        if (!m.matches()) throw invalido();
        String boundary = m.group(1) == null ? m.group(2) : m.group(1);
        byte[] inicio = ascii("--" + boundary + "\r\n"), delimitador = ascii("\r\n--" + boundary);
        if (!igual(corpo, 0, inicio)) throw invalido();
        int pos = inicio.length;
        Map<String, byte[]> campos = new HashMap<>();
        String nome = null, mime = null;
        while (true) {
            int fimHeader = encontrar(corpo, ascii("\r\n\r\n"), pos);
            if (fimHeader < pos || fimHeader - pos > 4096) throw invalido();
            for (int i = pos; i < fimHeader; i++) if (corpo[i] < 32 && corpo[i] != 13 && corpo[i] != 10) throw invalido();
            Map<String, String> headers = new HashMap<>();
            for (String linha : new String(corpo, pos, fimHeader-pos, StandardCharsets.US_ASCII).split("\r\n")) {
                int colon = linha.indexOf(':');
                if (colon < 1) throw invalido();
                String key = linha.substring(0, colon).toLowerCase(Locale.ROOT);
                if (!Set.of("content-disposition", "content-type").contains(key)
                        || headers.putIfAbsent(key, linha.substring(colon+1).trim()) != null) throw invalido();
            }
            var d = DISPOSICAO.matcher(headers.getOrDefault("content-disposition", ""));
            if (!d.matches()) throw invalido();
            String key = d.group(1);
            if (campos.containsKey(key)) throw invalido();
            if (key.equals("arquivo")) {
                nome = d.group(2); mime = headers.get("content-type");
                if (nome == null || nome.contains("..") || !nome.toLowerCase(Locale.ROOT).matches(".+\\.(png|jpg|jpeg|webp)")) throw invalido();
            } else if (d.group(2) != null || headers.containsKey("content-type")) throw invalido();
            pos = fimHeader + 4;
            int fim = encontrar(corpo, delimitador, pos);
            if (fim < pos) throw invalido();
            int limite = key.equals("arquivo") ? MAX_IMAGEM : 32;
            if (fim-pos > limite) throw new HttpException(413, "REQUISICAO_MUITO_GRANDE", "Imagem excede 1 MiB");
            campos.put(key, Arrays.copyOfRange(corpo, pos, fim));
            pos = fim + delimitador.length;
            if (igual(corpo, pos, ascii("--\r\n")) && pos+4 == corpo.length) break;
            if (!igual(corpo, pos, ascii("\r\n")) || campos.size() >= 3) throw invalido();
            pos += 2;
        }
        if (!campos.keySet().equals(Set.of("contexto", "alvoId", "arquivo"))) throw invalido();
        String contexto = new String(campos.get("contexto"), StandardCharsets.US_ASCII);
        String alvo = new String(campos.get("alvoId"), StandardCharsets.US_ASCII);
        if (!Set.of("evento", "postagem", "mensagem").contains(contexto) || !alvo.matches("[1-9][0-9]{0,9}")) throw invalido();
        long id = Long.parseLong(alvo);
        if (id > Integer.MAX_VALUE) throw invalido();
        return new MultipartImagem(contexto, id, nome, mime, campos.get("arquivo"));
    }
    private static byte[] ascii(String s) { return s.getBytes(StandardCharsets.US_ASCII); }
    private static boolean igual(byte[] b, int p, byte[] v) {
        if (p < 0 || p+v.length > b.length) return false;
        for (int i=0; i<v.length; i++) if (b[p+i]!=v[i]) return false;
        return true;
    }
    private static int encontrar(byte[] b, byte[] v, int p) {
        for (int i=p; i<=b.length-v.length; i++) if (igual(b,i,v)) return i;
        return -1;
    }
    private static HttpException invalido() { return new HttpException(400, "MULTIPART_INVALIDO", "Use contexto, alvoId e um arquivo PNG/JPEG/WebP válido"); }
}
