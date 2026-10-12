package br.com.joinup.suporte.domain;

import java.time.Instant;

public record Ticket(long id, String titulo, String descricao, StatusTicket status,
                     Long participanteId, Long organizadorId, Long suporteId,
                     Instant criadoEm, Instant atualizadoEm, Instant fechadoEm) {}
