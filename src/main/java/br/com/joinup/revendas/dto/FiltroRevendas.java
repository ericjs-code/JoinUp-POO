package br.com.joinup.revendas.dto;

import br.com.joinup.http.*;
import java.util.*;

public record FiltroRevendas(Long eventoId,int pagina,int tamanho) {
    public FiltroRevendas {
        if(eventoId!=null && (eventoId<=0 || eventoId>Integer.MAX_VALUE) || pagina<=0 || tamanho<=0 || tamanho>100 || (long)(pagina-1)*tamanho>Integer.MAX_VALUE)
            throw invalido();
    }
    public static FiltroRevendas de(Map<String,String> q) {
        if(!Set.of("eventoId","pagina","tamanho").containsAll(q.keySet())) throw invalido();
        return new FiltroRevendas(q.getOrDefault("eventoId","").isEmpty()?null:RouteParameters.id(q.get("eventoId")),
                inteiro(q,"pagina",1),inteiro(q,"tamanho",20));
    }
    private static int inteiro(Map<String,String> q,String nome,int padrao) { return q.getOrDefault(nome,"").isEmpty()?padrao:(int)RouteParameters.id(q.get(nome)); }
    private static HttpException invalido() { return new HttpException(400,"REQUISICAO_INVALIDA","Filtros de revenda inválidos"); }
}
