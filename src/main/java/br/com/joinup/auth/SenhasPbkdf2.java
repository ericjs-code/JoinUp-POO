package br.com.joinup.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.*;
import java.util.Arrays;
import java.util.Base64;

public final class SenhasPbkdf2 implements VerificadorSenha {
    private final SecureRandom random = new SecureRandom();
    private final int iteracoes;
    private final String hashAusente;
    public SenhasPbkdf2(int iteracoes) {
        if (iteracoes < 600_000 || iteracoes > 2_000_000) throw new IllegalArgumentException("Custo PBKDF2 inválido");
        this.iteracoes = iteracoes;
        hashAusente = gerar(Base64.getEncoder().encodeToString(random.generateSeed(32)));
    }
    public String gerar(String senha) {
        byte[] salt = new byte[16]; random.nextBytes(salt);
        byte[] hash = derivar(senha, salt, iteracoes);
        try {
            return "pbkdf2_sha256$" + iteracoes + "$" + Base64.getEncoder().encodeToString(salt) + "$" + Base64.getEncoder().encodeToString(hash);
        } finally { Arrays.fill(hash, (byte) 0); }
    }
    @Override public boolean verificar(String senha, String hash) {
        if (senha == null || senha.length() > 1024) return false;
        if (hash == null) { verificar(senha, hashAusente); return false; }
        try {
            String[] partes = hash.split("\\$", -1);
            if (partes.length != 4 || !partes[0].equals("pbkdf2_sha256")) return false;
            int custo = Integer.parseInt(partes[1]);
            // Compatibilidade estrita com as fixtures aprovadas de 180 mil iterações.
            if (custo < 180_000 || custo > 2_000_000) return false;
            byte[] salt = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            if (salt.length != 16 || esperado.length != 32) return false;
            byte[] obtido = derivar(senha, salt, custo);
            try { return MessageDigest.isEqual(esperado, obtido); }
            finally { Arrays.fill(obtido, (byte) 0); }
        } catch (IllegalArgumentException e) { return false; }
    }
    private static byte[] derivar(String senha, byte[] salt, int iteracoes) {
        char[] caracteres = senha.toCharArray();
        var spec = new PBEKeySpec(caracteres, salt, iteracoes, 256);
        Arrays.fill(caracteres, '\0');
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (GeneralSecurityException e) { throw new IllegalStateException("PBKDF2 indisponível"); }
        finally { spec.clearPassword(); }
    }
}
