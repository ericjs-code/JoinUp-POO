package br.com.joinup.shared.db;

import org.junit.jupiter.api.Test;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

/** Valida somente a fixture SQL lida do classpath. Não equivale a teste de MySQL/login. */
class SeedDemoPasswordTest {
    @Test void todosOsNoveHashesDoSeedConferemComPbkdf2DoJdk() throws Exception {
        String seed;
        try (var recurso = getClass().getResourceAsStream("/db/seed_demo_v0.2.sql")) {
            assertNotNull(recurso);
            seed = new String(recurso.readAllBytes(), StandardCharsets.UTF_8);
        }
        var hashes = Pattern.compile("'pbkdf2_sha256\\$(\\d+)\\$([^$']+)\\$([^']+)'").matcher(seed);
        int quantidade = 0;
        while (hashes.find()) {
            int iteracoes = Integer.parseInt(hashes.group(1));
            byte[] salt = Base64.getDecoder().decode(hashes.group(2));
            byte[] esperado = Base64.getDecoder().decode(hashes.group(3));
            assertTrue(MessageDigest.isEqual(esperado, derivar("JoinUpDemo#2026", salt, iteracoes, esperado.length)));
            assertFalse(MessageDigest.isEqual(esperado, derivar("senha-incorreta", salt, iteracoes, esperado.length)));
            quantidade++;
        }
        assertEquals(9, quantidade);
    }

    private byte[] derivar(String senha, byte[] salt, int iteracoes, int bytes) throws Exception {
        var especificacao = new PBEKeySpec(senha.toCharArray(), salt, iteracoes, bytes * 8);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(especificacao).getEncoded();
        } finally {
            especificacao.clearPassword();
        }
    }
}
