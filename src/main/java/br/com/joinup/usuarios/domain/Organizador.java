package br.com.joinup.usuarios.domain;

public final class Organizador extends Usuario {
    private final String cnpj;
    public Organizador(long id, String nome, String email, String hash, boolean ativo, String cnpj) {
        super(id, nome, email, hash, ativo); this.cnpj = cnpj;
    }
    @Override public Perfil perfil() { return Perfil.ORGANIZADOR; }
    public String cnpj() { return cnpj; }
}
