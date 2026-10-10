package br.com.joinup.compras.domain;

public record Ingresso(long id, long participanteId, long eventoId, long tipoIngressoId, String status) {}
