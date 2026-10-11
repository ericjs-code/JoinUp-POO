package br.com.joinup.comunidades.dto;

import java.time.Instant;

public record ComunidadeDetalheView(
        long id,
        String nome,
        String descricao,
        String imagem,
        long eventoId,
        String eventoNome,
        Instant dataCriacao,
        boolean membroAtivo,
        boolean elegivelParaEntrar
) {}
