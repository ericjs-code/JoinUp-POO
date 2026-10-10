package br.com.joinup.auth;

import br.com.joinup.http.HttpException;
import br.com.joinup.usuarios.domain.*;
import br.com.joinup.usuarios.repository.RepositoriosUsuarios;
import java.sql.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;

public final class GerenciadorSessao {
    private final SecureRandom random = new SecureRandom();
    private final SessaoRepositoryJdbc sessoes;
    private final RepositoriosUsuarios usuarios;
    private final long duracaoSegundos;
    public GerenciadorSessao(SessaoRepositoryJdbc sessoes, RepositoriosUsuarios usuarios, long duracaoSegundos) {
        if (duracaoSegundos < 60 || duracaoSegundos > 86400) throw new IllegalArgumentException("Duração de sessão inválida");
        this.sessoes = sessoes; this.usuarios = usuarios; this.duracaoSegundos = duracaoSegundos;
    }
    public SessaoEmitida criar(Connection c, Usuario usuario) throws SQLException {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessoes.criar(c, hashToken(token), usuario, duracaoSegundos);
        return new SessaoEmitida(token, usuario, duracaoSegundos);
    }
    public Usuario exigir(Connection c, String token, Perfil... permitidos) throws SQLException {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw naoAutenticado();
        String hash = hashToken(token);
        var titular = sessoes.localizar(c, hash);
        if (titular == null) throw naoAutenticado();
        // Ordem global de locks: conta -> sessão. Login usa a mesma conta, impedindo
        // emissão de sessão após commit de desativação/alteração concorrente.
        Usuario usuario = usuarios.de(titular.perfil()).buscarId(c, titular.id(), true);
        if (usuario == null || !sessoes.validarEBloquear(c, hash)) throw naoAutenticado();
        if (!usuario.ativo()) throw new HttpException(403, "CONTA_INATIVA", "Conta desativada");
        if (permitidos.length > 0 && java.util.Arrays.stream(permitidos).noneMatch(p -> p == usuario.perfil()))
            throw new HttpException(403, "PERMISSAO_NEGADA", "Perfil sem permissão para esta operação");
        return usuario;
    }
    public void revogar(Connection c, String token) throws SQLException { sessoes.revogar(c, hashToken(token)); }
    public void revogarTodas(Connection c, Usuario usuario) throws SQLException { sessoes.revogarTodas(c, usuario); }
    public static String hashToken(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 indisponível"); }
    }
    public static HttpException naoAutenticado() { return new HttpException(401, "NAO_AUTENTICADO", "Sessão ausente ou inválida"); }
}
