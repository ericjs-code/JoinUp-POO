package br.com.joinup.suporte.domain;

import java.time.Instant;

public record MensagemTicket(long id, String autorTipo, long autorId, String autorNome,
                             String mensagem, Instant enviadoEm) {}
