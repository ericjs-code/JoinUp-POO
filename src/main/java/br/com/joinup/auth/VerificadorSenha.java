package br.com.joinup.auth;

public interface VerificadorSenha {
    boolean verificar(String senha, String hash);
}
