package br.com.joinup.usuarios.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** Product do Factory Method. A API usa UsuarioView, nunca serializa esta entidade. */
public abstract class Usuario {
    private long id;
    private final String nome;
    private final String email;
    private final String hashSenha;
    private final boolean ativo;

    protected Usuario(long id, String nome, String email, String hashSenha, boolean ativo) {
        this.id = id; this.nome = nome; this.email = email; this.hashSenha = hashSenha; this.ativo = ativo;
    }
    public long id() { return id; }
    public String nome() { return nome; }
    public String email() { return email; }
    @JsonIgnore public String hashSenha() { return hashSenha; }
    public boolean ativo() { return ativo; }
    public abstract Perfil perfil();
    public void atribuirId(long id) {
        if (this.id != 0 || id <= 0) throw new IllegalStateException("Identidade já atribuída ou inválida");
        this.id = id;
    }
}
