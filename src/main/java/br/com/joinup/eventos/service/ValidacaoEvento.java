package br.com.joinup.eventos.service;

import br.com.joinup.http.HttpException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Set;

public final class ValidacaoEvento {
    private ValidacaoEvento() {}
    private static final Set<String> UFS = Set.of("AC","AL","AP","AM","BA","CE","DF","ES","GO","MA","MT","MS","MG","PA","PB","PR","PE","PI","RJ","RN","RS","RO","RR","SC","SP","SE","TO");
    public static HttpException invalido(String campo) { return new HttpException(422, "REQUISICAO_INVALIDA", "Campo inválido: " + campo); }
    public static HttpException naoEncontrado() { return new HttpException(404,"RECURSO_NAO_ENCONTRADO","Recurso não encontrado"); }
    public static String texto(String s, int limite, String campo) {
        if (s == null || s.isBlank() || s.codePointCount(0,s.length()) > limite || s.chars().anyMatch(c -> c < 32 && c != '\n' && c != '\r' && c != '\t')) throw invalido(campo);
        return s.trim();
    }
    public static long id(Long id, String campo) {
        if (id == null || id <= 0 || id > Integer.MAX_VALUE) throw invalido(campo);
        return id;
    }
    public static String uf(String uf) { if (!UFS.contains(uf == null ? "" : uf)) throw invalido("estado"); return uf; }
    public static String cep(String cep) { if (cep == null || !cep.matches("[0-9]{8}")) throw invalido("cep"); return cep; }
    public static String imagem(String imagem) {
        if (imagem == null || imagem.isEmpty()) return null;
        // Allowlist exata de recurso empacotado: sem filesystem, URL remota ou fetch no servidor.
        if (!imagem.equals("/assets/evento-placeholder.svg")) throw invalido("imagem");
        return imagem;
    }
    public static Instant dataFutura(OffsetDateTime data, Instant agora) {
        if (data == null) throw invalido("data");
        Instant normalizado = data.toInstant().truncatedTo(ChronoUnit.SECONDS);
        int ano = normalizado.atOffset(ZoneOffset.UTC).getYear();
        if (ano < 1000 || ano > 9999 || !normalizado.isAfter(agora))
            throw new HttpException(422,"DATA_EVENTO_INVALIDA","A data do evento deve ser estritamente futura");
        return normalizado;
    }
    public static void antesInicio(Instant inicio, Instant agora) {
        if (!inicio.isAfter(agora)) throw new HttpException(409,"EVENTO_JA_INICIADO","O evento já começou; edição e categorias estão bloqueadas");
    }
}
