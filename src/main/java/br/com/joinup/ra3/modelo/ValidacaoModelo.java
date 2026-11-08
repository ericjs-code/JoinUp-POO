package br.com.joinup.ra3.modelo;

import java.time.LocalDateTime;

/** Limites do SQL compartilhados pelo modelo; não é entidade nem serviço de negócio. */
final class ValidacaoModelo {
    private ValidacaoModelo() { }

    static String texto(String valor, String campo, int limite, boolean obrigatorio) {
        if (valor == null) {
            if (obrigatorio) throw new IllegalArgumentException(campo + " obrigatório.");
            return null;
        }
        if ((obrigatorio && valor.isBlank()) || valor.codePointCount(0, valor.length()) > limite) {
            throw new IllegalArgumentException(campo + " inválido: limite de " + limite + " caracteres.");
        }
        return valor;
    }

    static <T> T obrigatorio(T valor, String campo) {
        if (valor == null) throw new IllegalArgumentException(campo + " obrigatório.");
        return valor;
    }

    /** DATETIME sem fuso: valores recebidos representam UTC, na precisão de segundos do DDL. */
    static LocalDateTime instanteUtc(LocalDateTime valor, String campo) {
        return obrigatorio(valor, campo).withNano(0);
    }
}
