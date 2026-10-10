package br.com.joinup.usuarios.dto;

import br.com.joinup.usuarios.domain.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UsuarioView(long id, Perfil perfil, String nome, String email, boolean ativo,
                          String cpf, String cnpj, LocalDate data_nascimento) {
    public static UsuarioView de(Usuario usuario) {
        return new UsuarioView(usuario.id(), usuario.perfil(), usuario.nome(), usuario.email(), usuario.ativo(),
                usuario instanceof Participante p ? p.cpf() : null,
                usuario instanceof Organizador o ? o.cnpj() : null,
                usuario instanceof Participante p ? p.dataNascimento() : null);
    }
}
