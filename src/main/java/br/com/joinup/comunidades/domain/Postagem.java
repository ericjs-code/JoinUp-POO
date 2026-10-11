package br.com.joinup.comunidades.domain;

import java.time.Instant;

public record Postagem(
        long id,
        long comunidadeId,
        long participanteId,
        String autorNome,
        String descricao,
        String imagem,
        int curtidas,
        int totalComentarios,
        Instant dataPostagem,
        boolean curtidoPeloUsuario
) {}
