package br.com.joinup.revendas.domain;

import java.math.BigDecimal;

public record Anuncio(long id, long ingressoId, long vendedorId, BigDecimal preco, String status) {}
