package br.com.joinup.eventos.domain;

import java.math.BigDecimal;

public record TipoIngresso(long id, String nome, BigDecimal preco, int quantidade,
                           int quantidadeVendida, boolean temVendas) {}
