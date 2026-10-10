package br.com.joinup.usuarios.service;

import br.com.joinup.http.HttpException;
import java.time.LocalDate;
import java.util.Locale;

public final class ValidacaoUsuario {
    private ValidacaoUsuario() { }
    public static String texto(String valor, int maximo, String campo) {
        if (valor == null || valor.strip().isEmpty()) throw invalido(campo);
        String texto = valor.strip();
        if (texto.codePointCount(0, texto.length()) > maximo || texto.chars().anyMatch(c -> c < 32 || c == 127))
            throw invalido(campo);
        return texto;
    }
    public static String email(String valor, int maximo) {
        String email = texto(valor, maximo, "email").toLowerCase(Locale.ROOT);
        if (!email.matches("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+"))
            throw invalido("email");
        return email;
    }
    public static String senha(String senha) {
        // Limite técnico contra abuso de CPU/memória; nenhuma regra adicional de complexidade.
        if (senha == null || senha.isEmpty() || senha.length() > 1024) throw invalido("senha");
        return senha;
    }
    public static String cpf(String valor) {
        if (valor == null || !valor.matches("(?:[0-9]{11}|[0-9]{3}\\.[0-9]{3}\\.[0-9]{3}-[0-9]{2})")) throw invalido("cpf");
        return valor.replaceAll("[^0-9]", "");
    }
    public static String cnpj(String valor) {
        String cnpj = texto(valor, 18, "cnpj");
        if (!cnpj.matches("(?:[0-9]{14}|[0-9]{2}\\.[0-9]{3}\\.[0-9]{3}/[0-9]{4}-[0-9]{2})")) throw invalido("cnpj");
        // Um formato canônico de 14 dígitos evita unicidade distinta por máscara.
        return cnpj.replaceAll("[^0-9]", "");
    }
    public static LocalDate nascimento(LocalDate valor) {
        if (valor == null || valor.getYear() < 1 || valor.getYear() > 9999) throw invalido("data_nascimento");
        return valor;
    }
    public static HttpException invalido(String campo) {
        return new HttpException(422, "REQUISICAO_INVALIDA", "Campo inválido: " + campo);
    }
}
