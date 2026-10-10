package br.com.joinup.eventos.dto;

import java.time.OffsetDateTime;
import java.util.List;

/** Campos omitidos preservados; imagem vazia remove somente a referência do evento. */
public record EditarEventoDados(String nome, String descricao, OffsetDateTime data, String imagem,
                                String localizacao, String cidade, String estado, String cep,
                                Long generoId, List<Long> artistaIds) {
    public boolean vazio() {
        return nome == null && descricao == null && data == null && imagem == null && localizacao == null
                && cidade == null && estado == null && cep == null && generoId == null && artistaIds == null;
    }
}
