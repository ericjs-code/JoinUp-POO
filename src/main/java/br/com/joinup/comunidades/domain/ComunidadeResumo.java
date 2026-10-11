package br.com.joinup.comunidades.domain;

import java.time.Instant;

public record ComunidadeResumo(
        long id,
        String nome,
        String descricao,
        String imagem,
        long eventoId,
        String eventoNome,
        Instant dataCriacao
) {}
