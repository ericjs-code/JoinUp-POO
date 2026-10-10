package br.com.joinup.eventos.dto;

import br.com.joinup.http.*;
import java.util.Map;
import java.util.Set;

public record FiltroEventos(String busca, Long generoId, String cidade, int pagina, int tamanho) {
    public FiltroEventos {
        if (pagina < 1 || pagina > 100_000 || tamanho < 1 || tamanho > 100
                || busca == null || busca.length() > 80 || cidade == null || cidade.length() > 60
                || (generoId != null && (generoId <= 0 || generoId > Integer.MAX_VALUE)))
            throw new HttpException(400, "REQUISICAO_INVALIDA", "Filtros ou paginação inválidos");
    }
    public static FiltroEventos de(Map<String,String> q) {
        if (!Set.of("busca", "generoId", "cidade", "pagina", "tamanho").containsAll(q.keySet()))
            throw new HttpException(400, "REQUISICAO_INVALIDA", "Filtro desconhecido");
        String genero = q.getOrDefault("generoId", "");
        return new FiltroEventos(q.getOrDefault("busca", "").trim(), genero.isEmpty() ? null : RouteParameters.id(genero),
                q.getOrDefault("cidade", "").trim(), (int)RouteParameters.id(q.getOrDefault("pagina", "1")),
                (int)RouteParameters.id(q.getOrDefault("tamanho", "20")));
    }
}
