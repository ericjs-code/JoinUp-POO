package br.com.joinup.comunidades.domain;

import java.time.Instant;

public record Comentario(
        long id,
        long postagemId,
        long participanteId,
        String autorNome,
        String texto,
        Instant dataComentario
) {}
