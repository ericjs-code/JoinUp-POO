package br.com.joinup.auth;

import br.com.joinup.usuarios.domain.Usuario;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** Resultado interno: o handler envia o token exclusivamente no Set-Cookie. */
public record SessaoEmitida(@JsonIgnore String token, Usuario usuario, long duracaoSegundos) {
    @Override public String toString() { return "SessaoEmitida[conteúdo omitido]"; }
}
