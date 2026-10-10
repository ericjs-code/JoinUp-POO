package br.com.joinup.compras.domain;

import java.time.Instant;

public record EventoComercial(long id, long organizadorId, String nome, Instant data, String local) {}
