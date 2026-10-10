package br.com.joinup.compras.dto;

import java.util.List;

public record CompraDados(Long eventoId, List<Item> itens) {
    public record Item(Long tipoIngressoId, Integer quantidade) {}
}
