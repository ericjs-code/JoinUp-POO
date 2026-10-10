package br.com.joinup.usuarios.dto;

import java.time.LocalDate;

public record CadastroDados(String nome, String email, String senha, String cpf, String cnpj, LocalDate data_nascimento) {
    @Override public String toString() { return "CadastroDados[conteúdo omitido]"; }
}
