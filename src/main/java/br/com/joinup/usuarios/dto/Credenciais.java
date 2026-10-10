package br.com.joinup.usuarios.dto;

public record Credenciais(String email, String senha) {
    @Override public String toString() { return "Credenciais[conteúdo omitido]"; }
}
