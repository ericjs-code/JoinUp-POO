package br.com.joinup.usuarios.domain;

/** Suporte não possui ciclo de desativação no schema aprovado. */
public final class UsuarioSuporte extends Usuario {
    public UsuarioSuporte(long id, String nome, String email, String hash) { super(id, nome, email, hash, true); }
    @Override public Perfil perfil() { return Perfil.SUPORTE; }
}
