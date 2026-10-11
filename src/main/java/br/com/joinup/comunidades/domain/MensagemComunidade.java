package br.com.joinup.comunidades.domain;

import java.time.Instant;

public record MensagemComunidade(
        long id,
        long comunidadeId,
        String autorTipo,
        Long participanteId,
        Long organizadorId,
        String autorNome,
        String mensagem,
        String imagem,
        Long idRespostaA,
        Instant dataEnvio
) {}
