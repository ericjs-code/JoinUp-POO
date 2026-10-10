package br.com.joinup.eventos.domain;

import java.time.Instant;

/** DATETIME do banco representa UTC, nunca a zona padrão da JVM. */
public record Evento(long id, String nome, Instant data, String descricao, String imagem,
                     String localizacao, String cidade, String estado, String cep,
                     long organizadorId, long generoId, String genero) {}
