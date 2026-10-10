package br.com.joinup.usuarios.dto;

public record PerfilDados(String nome, String email, String nova_senha, String cnpj) {
    @Override public String toString() { return "PerfilDados[conteúdo omitido]"; }
}
