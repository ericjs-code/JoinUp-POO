package br.com.joinup.eventos.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record CriarEventoDados(String nome, String descricao, OffsetDateTime data, String imagem,
                               String localizacao, String cidade, String estado, String cep,
                               Long generoId, List<Long> artistaIds, List<TipoIngressoDados> tiposIngresso) {}
