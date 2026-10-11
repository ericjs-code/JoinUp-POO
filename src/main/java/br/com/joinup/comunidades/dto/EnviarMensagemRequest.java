package br.com.joinup.comunidades.dto;

public record EnviarMensagemRequest(
        String mensagem,
        String imagem,
        Long idRespostaA
) {}
