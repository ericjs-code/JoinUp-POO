package br.com.joinup.auth;

import br.com.joinup.config.ConfiguracaoSistema;
import br.com.joinup.http.*;
import br.com.joinup.usuarios.dto.*;
import br.com.joinup.usuarios.service.ValidacaoUsuario;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class AuthenticationSecurityTest {
    private static final SenhasPbkdf2 SENHAS = new SenhasPbkdf2(600_000);
    @Test void hashIndividualFormatoEVerificacao() {
        String a = SENHAS.gerar("uma senha "), b = SENHAS.gerar("uma senha ");
        assertNotEquals(a, b); assertTrue(a.startsWith("pbkdf2_sha256$600000$"));
        assertTrue(SENHAS.verificar("uma senha ", a)); assertFalse(SENHAS.verificar("uma senha", a));
        assertFalse(SENHAS.verificar("uma senha ", null));
        assertEquals(64, GerenciadorSessao.hashToken("abc").length());
    }
    @ParameterizedTest @ValueSource(strings = {"", "bcrypt$algo", "pbkdf2_sha256$-1$abc$abc", "pbkdf2_sha256$999999999$abc$abc",
            "pbkdf2_sha256$600000$abc$abc", "pbkdf2_sha256$x$a$b", "pbkdf2_sha256$600000$?$?"})
    void hashMalformadoNaoAutentica(String hash) { assertFalse(SENHAS.verificar("senha", hash)); }
    @Test void datasIsoSemCoercaoOuCamposDeTitularidade() throws Exception {
        var json = new JsonCodec();
        var dados = new CadastroDados("Nome", "n@joinup.test", "senha", "12345678901", null, LocalDate.of(1990, 2, 10));
        byte[] bytes = json.escrever(dados);
        assertTrue(new String(bytes, StandardCharsets.UTF_8).contains("\"data_nascimento\":\"1990-02-10\""));
        assertEquals(dados, json.ler(bytes, CadastroDados.class));
        for (String entrada : new String[]{"{\"id\":2}", "{\"perfil\":\"SUPORTE\"}", "{\"email\":123}", "{\"nome\":true}"})
            assertEquals(400, assertThrows(HttpException.class, () -> json.ler(entrada.getBytes(StandardCharsets.UTF_8), PerfilDados.class)).status());
        assertFalse(dados.toString().contains("senha"));
        assertFalse(new Credenciais("e", "segredo").toString().contains("segredo"));
    }
    @ParameterizedTest @ValueSource(strings = {"http://example.com", "http://localhost:8080", "https://example.com/", "https://user:pass@example.com",
            "https://example.com?x=1", "https://example.com#x", "https://example.com:0", "ftp://example.com"})
    void origemInvalidaRejeitada(String origem) { assertThrows(IllegalArgumentException.class, () -> ConfiguracaoSistema.validarOrigemPublica(origem)); }
    @Test void origemSeguraEValidacaoSemNovasRegrasFuncionais() {
        ConfiguracaoSistema.validarOrigemPublica("http://127.0.0.1:8080");
        ConfiguracaoSistema.validarOrigemPublica("https://joinup.example");
        assertEquals("00000000001", ValidacaoUsuario.cpf("000.000.000-01"));
        assertEquals("00000000000001", ValidacaoUsuario.cnpj("00.000.000/0000-01"));
        assertEquals("x", ValidacaoUsuario.senha("x"));
        assertThrows(HttpException.class, () -> ValidacaoUsuario.cpf("xxx00000000001"));
        assertThrows(HttpException.class, () -> ValidacaoUsuario.cnpj("0000000000000"));
        assertThrows(HttpException.class, () -> ValidacaoUsuario.texto("a".repeat(61), 60, "nome"));
        assertThrows(HttpException.class, () -> ValidacaoUsuario.senha("a".repeat(1025)));
    }
}
