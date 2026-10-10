package br.com.joinup.eventos.dto;

import br.com.joinup.eventos.domain.*;
import java.time.Instant;
import java.util.List;

/** DTO explícito: nunca serializar conta do organizador ou conteúdo comunitário. */
public record EventoView(long id, String nome, Instant data, String descricao, String imagem,
                         String localizacao, String cidade, String estado, String cep,
                         ItemCatalogo genero, List<ItemCatalogo> artistas,
                         List<TipoIngresso> tiposIngresso, Comunidade comunidade, boolean editavel) {
    public static EventoView de(Evento e, List<ItemCatalogo> a, List<TipoIngresso> t, Comunidade c, Instant agora) {
        return new EventoView(e.id(), e.nome(), e.data(), e.descricao(), e.imagem(), e.localizacao(),
                e.cidade(), e.estado(), e.cep(), new ItemCatalogo(e.generoId(), e.genero()), a, t, c, e.data().isAfter(agora));
    }
}
