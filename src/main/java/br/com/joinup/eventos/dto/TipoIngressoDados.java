package br.com.joinup.eventos.dto;

import java.math.BigDecimal;

public record TipoIngressoDados(String nome, BigDecimal preco, Integer quantidade) {}
