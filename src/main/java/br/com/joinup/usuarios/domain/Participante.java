package br.com.joinup.usuarios.domain;

import java.time.LocalDate;

public final class Participante extends Usuario {
    private final String cpf;
    private final LocalDate dataNascimento;
    public Participante(long id, String nome, String email, String hash, boolean ativo, String cpf, LocalDate nascimento) {
        super(id, nome, email, hash, ativo); this.cpf = cpf; this.dataNascimento = nascimento;
    }
    @Override public Perfil perfil() { return Perfil.PARTICIPANTE; }
    public String cpf() { return cpf; }
    public LocalDate dataNascimento() { return dataNascimento; }
}
