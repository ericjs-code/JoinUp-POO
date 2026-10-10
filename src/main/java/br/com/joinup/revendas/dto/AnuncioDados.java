package br.com.joinup.revendas.dto;

import java.math.BigDecimal;
import java.util.List;

public record AnuncioDados(List<Long> ingressoIds, BigDecimal precoUnitario) {}
