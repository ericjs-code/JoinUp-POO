package br.com.joinup.comunidades.repository;

import br.com.joinup.comunidades.domain.*;
import java.sql.*;
import java.util.*;
import static br.com.joinup.comunidades.repository.JdbcComunidades.*;

public class ComunidadeSocialRepositoryJdbc {

    public List<ComunidadeResumo> listarComunidades(Connection c, String busca) throws SQLException {
        return listarComunidades(c, busca, 50, 0);
    }

    public List<ComunidadeResumo> listarComunidades(Connection c, String busca, int tamanho, int deslocamento) throws SQLException {
        String sql = """
            SELECT cm.id_comunidade, cm.nome, cm.descricao, e.imagem AS imagem, cm.id_evento, e.nome AS evento_nome, cm.data_criacao
            FROM comunidades cm
            JOIN eventos e ON e.id_evento = cm.id_evento
            WHERE (? IS NULL OR ? = '' OR cm.nome LIKE ? OR e.nome LIKE ?)
            ORDER BY cm.id_comunidade ASC LIMIT ? OFFSET ?
        """;
        String padrao = (busca != null && !busca.isBlank()) ? "%" + busca.trim() + "%" : null;
        var lista = new ArrayList<ComunidadeResumo>();
        try (var ps = c.prepareStatement(sql)) {
            ps.setString(1, padrao);
            ps.setString(2, padrao);
            ps.setString(3, padrao);
            ps.setString(4, padrao);
            ps.setInt(5, tamanho);
            ps.setInt(6, deslocamento);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new ComunidadeResumo(
                            rs.getLong("id_comunidade"),
                            rs.getString("nome"),
                            rs.getString("descricao"),
                            rs.getString("imagem"),
                            rs.getLong("id_evento"),
                            rs.getString("evento_nome"),
                            instante(rs, "data_criacao")
                    ));
                }
            }
        }
        return List.copyOf(lista);
    }

    public ComunidadeResumo buscarComunidade(Connection c, long id) throws SQLException {
        String sql = """
            SELECT cm.id_comunidade, cm.nome, cm.descricao, e.imagem AS imagem, cm.id_evento, e.nome AS evento_nome, cm.data_criacao
            FROM comunidades cm
            JOIN eventos e ON e.id_evento = cm.id_evento
            WHERE cm.id_comunidade = ?
        """;
        try (var ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ComunidadeResumo(
                            rs.getLong("id_comunidade"),
                            rs.getString("nome"),
                            rs.getString("descricao"),
                            rs.getString("imagem"),
                            rs.getLong("id_evento"),
                            rs.getString("evento_nome"),
                            instante(rs, "data_criacao")
                    );
                }
            }
        }
        return null;
    }

    public boolean verificarMembroAtivo(Connection c, long comunidadeId, long participanteId) throws SQLException {
        String sql = "SELECT 1 FROM participante_comunidades WHERE id_comunidade = ? AND id_participante = ? AND ativo = 1";
        try (var ps = c.prepareStatement(sql)) {
            ps.setLong(1, comunidadeId);
            ps.setLong(2, participanteId);
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean verificarIngressoElegivel(Connection c, long comunidadeId, long participanteId) throws SQLException {
        String sql = """
            SELECT 1 FROM ingressos i
            JOIN comunidades cm ON cm.id_evento = i.id_evento
            WHERE cm.id_comunidade = ? AND i.id_participante = ? AND i.status IN ('ativo', 'utilizado')
            LIMIT 1
        """;
        try (var ps = c.prepareStatement(sql)) {
            ps.setLong(1, comunidadeId);
            ps.setLong(2, participanteId);
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean verificarOrganizadorDono(Connection c, long comunidadeId, long organizadorId) throws SQLException {
        String sql = """
            SELECT 1 FROM eventos e
            JOIN comunidades cm ON cm.id_evento = e.id_evento
            WHERE cm.id_comunidade = ? AND e.id_organizador = ?
            LIMIT 1
        """;
        try (var ps = c.prepareStatement(sql)) {
            ps.setLong(1, comunidadeId);
            ps.setLong(2, organizadorId);
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void entrarComunidade(Connection c, long comunidadeId, long participanteId) throws SQLException {
        String sql = """
            INSERT INTO participante_comunidades (id_participante, id_comunidade, data_entrada, ativo, data_saida)
            VALUES (?, ?, UTC_TIMESTAMP(), 1, NULL)
            ON DUPLICATE KEY UPDATE ativo = 1, data_saida = NULL, data_entrada = UTC_TIMESTAMP()
        """;
        executar(c, sql, participanteId, comunidadeId);
    }

    public void sairComunidade(Connection c, long comunidadeId, long participanteId) throws SQLException {
        String sql = "UPDATE participante_comunidades SET ativo = 0, data_saida = UTC_TIMESTAMP() WHERE id_comunidade = ? AND id_participante = ? AND ativo = 1";
        executar(c, sql, comunidadeId, participanteId);
    }

    public List<MensagemComunidade> listarMensagens(Connection c, long comunidadeId) throws SQLException {
        return listarMensagens(c, comunidadeId, 50, 0);
    }

    public List<MensagemComunidade> listarMensagens(Connection c, long comunidadeId, int tamanho, int deslocamento) throws SQLException {
        return listarMensagens(c, comunidadeId, tamanho, deslocamento, null);
    }

    public List<MensagemComunidade> listarMensagens(Connection c, long comunidadeId, int tamanho, int deslocamento, Long id) throws SQLException {
        String sql = """
            SELECT m.id_mensagem, m.id_comunidade, m.autor_tipo, m.id_participante, m.id_organizador,
                   COALESCE(p.nome, o.nome) AS autor_nome, m.mensagem, m.imagem, m.id_resposta_a, m.data_envio
            FROM comunidade_mensagens m
            LEFT JOIN participantes p ON p.id_participante = m.id_participante
            LEFT JOIN organizadores o ON o.id_organizador = m.id_organizador
            WHERE m.id_comunidade = ?
            AND (? IS NULL OR m.id_mensagem=?)
            ORDER BY m.id_mensagem ASC LIMIT ? OFFSET ?
        """;
        var lista = new ArrayList<MensagemComunidade>();
        try (var ps = c.prepareStatement(sql)) {
            ps.setLong(1, comunidadeId);
            ps.setObject(2, id);
            ps.setObject(3, id);
            ps.setInt(4, tamanho);
            ps.setInt(5, deslocamento);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    Long partId = rs.getObject("id_participante") != null ? rs.getLong("id_participante") : null;
                    Long orgId = rs.getObject("id_organizador") != null ? rs.getLong("id_organizador") : null;
                    Long respId = rs.getObject("id_resposta_a") != null ? rs.getLong("id_resposta_a") : null;
                    lista.add(new MensagemComunidade(
                            rs.getLong("id_mensagem"),
                            rs.getLong("id_comunidade"),
                            rs.getString("autor_tipo"),
                            partId,
                            orgId,
                            rs.getString("autor_nome"),
                            rs.getString("mensagem"),
                            rs.getString("imagem"),
                            respId,
                            instante(rs, "data_envio")
                    ));
                }
            }
        }
        return List.copyOf(lista);
    }

    public long enviarMensagem(Connection c, long comunidadeId, String autorTipo, Long participanteId, Long organizadorId, String texto, String imagem, Long idRespostaA) throws SQLException {
        String sql = """
            INSERT INTO comunidade_mensagens (id_comunidade, autor_tipo, id_participante, id_organizador, mensagem, imagem, id_resposta_a, data_envio)
            VALUES (?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP())
        """;
        return inserir(c, sql, comunidadeId, autorTipo, participanteId, organizadorId, texto, imagem, idRespostaA);
    }

    public List<Postagem> listarPostagens(Connection c, long comunidadeId, Long participanteIdLogado) throws SQLException {
        return listarPostagens(c, comunidadeId, participanteIdLogado, 50, 0);
    }

    public List<Postagem> listarPostagens(Connection c, long comunidadeId, Long participanteIdLogado, int tamanho, int deslocamento) throws SQLException {
        String sql = """
            SELECT post.id_postagem, post.id_comunidade, post.id_participante, p.nome AS autor_nome,
                   post.descricao, post.imagem, post.curtidas,
                   (SELECT COUNT(*) FROM comentarios com WHERE com.id_postagem = post.id_postagem) AS total_comentarios,
                   post.data_postagem,
                   CASE WHEN ? IS NOT NULL AND EXISTS(
                       SELECT 1 FROM postagem_curtidas pc WHERE pc.id_postagem = post.id_postagem AND pc.id_participante = ?
                   ) THEN 1 ELSE 0 END AS curtido_pelo_usuario
            FROM postagens post
            JOIN participantes p ON p.id_participante = post.id_participante
            WHERE post.id_comunidade = ?
            ORDER BY post.id_postagem DESC LIMIT ? OFFSET ?
        """;
        var lista = new ArrayList<Postagem>();
        try (var ps = c.prepareStatement(sql)) {
            ps.setObject(1, participanteIdLogado);
            ps.setObject(2, participanteIdLogado);
            ps.setLong(3, comunidadeId);
            ps.setInt(4, tamanho);
            ps.setInt(5, deslocamento);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Postagem(
                            rs.getLong("id_postagem"),
                            rs.getLong("id_comunidade"),
                            rs.getLong("id_participante"),
                            rs.getString("autor_nome"),
                            rs.getString("descricao"),
                            rs.getString("imagem"),
                            rs.getInt("curtidas"),
                            rs.getInt("total_comentarios"),
                            instante(rs, "data_postagem"),
                            rs.getInt("curtido_pelo_usuario") == 1
                    ));
                }
            }
        }
        return List.copyOf(lista);
    }

    public Postagem buscarPostagem(Connection c, long postagemId, Long participanteIdLogado) throws SQLException {
        String sql = """
            SELECT post.id_postagem, post.id_comunidade, post.id_participante, p.nome AS autor_nome,
                   post.descricao, post.imagem, post.curtidas,
                   (SELECT COUNT(*) FROM comentarios com WHERE com.id_postagem = post.id_postagem) AS total_comentarios,
                   post.data_postagem,
                   CASE WHEN ? IS NOT NULL AND EXISTS(
                       SELECT 1 FROM postagem_curtidas pc WHERE pc.id_postagem = post.id_postagem AND pc.id_participante = ?
                   ) THEN 1 ELSE 0 END AS curtido_pelo_usuario
            FROM postagens post
            JOIN participantes p ON p.id_participante = post.id_participante
            WHERE post.id_postagem = ?
        """;
        try (var ps = c.prepareStatement(sql)) {
            ps.setObject(1, participanteIdLogado);
            ps.setObject(2, participanteIdLogado);
            ps.setLong(3, postagemId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Postagem(
                            rs.getLong("id_postagem"),
                            rs.getLong("id_comunidade"),
                            rs.getLong("id_participante"),
                            rs.getString("autor_nome"),
                            rs.getString("descricao"),
                            rs.getString("imagem"),
                            rs.getInt("curtidas"),
                            rs.getInt("total_comentarios"),
                            instante(rs, "data_postagem"),
                            rs.getInt("curtido_pelo_usuario") == 1
                    );
                }
            }
        }
        return null;
    }

    public long criarPostagem(Connection c, long comunidadeId, long participanteId, String descricao, String imagem) throws SQLException {
        String sql = "INSERT INTO postagens (descricao, imagem, curtidas, id_participante, id_comunidade, data_postagem) VALUES (?, ?, 0, ?, ?, UTC_TIMESTAMP())";
        return inserir(c, sql, descricao, imagem, participanteId, comunidadeId);
    }

    public void curtirPostagem(Connection c, long postagemId, long participanteId) throws SQLException {
        try (var ps = c.prepareStatement("SELECT id_postagem FROM postagens WHERE id_postagem=? FOR UPDATE")) {
            ps.setLong(1, postagemId); try (var rs = ps.executeQuery()) { rs.next(); }
        }
        String sqlInsert = "INSERT IGNORE INTO postagem_curtidas (id_participante, id_postagem, data_curtida) VALUES (?, ?, UTC_TIMESTAMP())";
        int linhas = executar(c, sqlInsert, participanteId, postagemId);
        if (linhas > 0) {
            executar(c, "UPDATE postagens SET curtidas = curtidas + 1 WHERE id_postagem = ?", postagemId);
        }
    }

    public void descurtirPostagem(Connection c, long postagemId, long participanteId) throws SQLException {
        try (var ps = c.prepareStatement("SELECT id_postagem FROM postagens WHERE id_postagem=? FOR UPDATE")) {
            ps.setLong(1, postagemId); try (var rs = ps.executeQuery()) { rs.next(); }
        }
        String sqlDelete = "DELETE FROM postagem_curtidas WHERE id_participante = ? AND id_postagem = ?";
        int linhas = executar(c, sqlDelete, participanteId, postagemId);
        if (linhas > 0) {
            executar(c, "UPDATE postagens SET curtidas = GREATEST(0, curtidas - 1) WHERE id_postagem = ?", postagemId);
        }
    }

    public List<Comentario> listarComentarios(Connection c, long postagemId) throws SQLException {
        return listarComentarios(c, postagemId, 50, 0);
    }

    public List<Comentario> listarComentarios(Connection c, long postagemId, int tamanho, int deslocamento) throws SQLException {
        return listarComentarios(c, postagemId, tamanho, deslocamento, null);
    }

    public List<Comentario> listarComentarios(Connection c, long postagemId, int tamanho, int deslocamento, Long id) throws SQLException {
        String sql = """
            SELECT com.id_comentario, com.id_postagem, com.id_participante, p.nome AS autor_nome,
                   com.texto, com.data_comentario
            FROM comentarios com
            JOIN participantes p ON p.id_participante = com.id_participante
            WHERE com.id_postagem = ?
            AND (? IS NULL OR com.id_comentario=?)
            ORDER BY com.id_comentario ASC LIMIT ? OFFSET ?
        """;
        var lista = new ArrayList<Comentario>();
        try (var ps = c.prepareStatement(sql)) {
            ps.setLong(1, postagemId);
            ps.setObject(2, id);
            ps.setObject(3, id);
            ps.setInt(4, tamanho);
            ps.setInt(5, deslocamento);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Comentario(
                            rs.getLong("id_comentario"),
                            rs.getLong("id_postagem"),
                            rs.getLong("id_participante"),
                            rs.getString("autor_nome"),
                            rs.getString("texto"),
                            instante(rs, "data_comentario")
                    ));
                }
            }
        }
        return List.copyOf(lista);
    }

    public long adicionarComentario(Connection c, long postagemId, long participanteId, String texto) throws SQLException {
        String sql = "INSERT INTO comentarios (texto, id_postagem, id_participante, data_comentario) VALUES (?, ?, ?, UTC_TIMESTAMP())";
        return inserir(c, sql, texto, postagemId, participanteId);
    }

    public Long buscarComunidadeIdDaPostagem(Connection c, long postagemId) throws SQLException {
        String sql = "SELECT id_comunidade FROM postagens WHERE id_postagem = ?";
        try (var ps = c.prepareStatement(sql)) {
            ps.setLong(1, postagemId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id_comunidade");
                }
            }
        }
        return null;
    }
    /** Conta -> evento -> ingressos -> filiação; nenhum lock em JOIN.
     * A conta do ator já é bloqueada por GerenciadorSessao no caminho HTTP.
     * Repetimos a validação para os métodos que recebem Connection/Usuario.
     * Não exigimos organizador ativo nem evento futuro para RF-COM-05.
     */
    public void bloquearAcesso(Connection c, long comunidadeId, br.com.joinup.usuarios.domain.Usuario u) throws SQLException {
        boolean participante = u.perfil() == br.com.joinup.usuarios.domain.Perfil.PARTICIPANTE;
        if (!participante && u.perfil() != br.com.joinup.usuarios.domain.Perfil.ORGANIZADOR)
            throw new br.com.joinup.http.HttpException(403, "PERMISSAO_NEGADA", "Perfil sem acesso à comunidade");
        String tabela = participante ? "participantes" : "organizadores";
        String pk = participante ? "id_participante" : "id_organizador";
        try (var ps = c.prepareStatement("SELECT ativo FROM " + tabela + " WHERE " + pk + "=? FOR UPDATE")) {
            ps.setLong(1, u.id());
            try (var rs = ps.executeQuery()) {
                if (!rs.next() || !rs.getBoolean(1))
                    throw new br.com.joinup.http.HttpException(403, "CONTA_INATIVA", "Conta desativada");
            }
        }
        var pista = buscarComunidade(c, comunidadeId);
        if (pista == null) throw new br.com.joinup.http.HttpException(404, "RECURSO_NAO_ENCONTRADO", "Comunidade não encontrada");
        try (var ps = c.prepareStatement("SELECT id_evento FROM eventos WHERE id_evento=? FOR UPDATE")) {
            ps.setLong(1, pista.eventoId());
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) throw new br.com.joinup.http.HttpException(404, "RECURSO_NAO_ENCONTRADO", "Comunidade não encontrada");
            }
        }
        if (participante) {
            try (var ps = c.prepareStatement("SELECT id_ingresso FROM ingressos WHERE id_evento=? AND id_participante=? AND status IN ('ativo','utilizado') ORDER BY id_ingresso FOR UPDATE")) {
                ps.setLong(1, pista.eventoId()); ps.setLong(2, u.id());
                try (var rs = ps.executeQuery()) { while (rs.next()) { /* manter locks até commit */ } }
            }
            try (var ps = c.prepareStatement("SELECT ativo FROM participante_comunidades WHERE id_comunidade=? AND id_participante=? FOR UPDATE")) {
                ps.setLong(1, comunidadeId); ps.setLong(2, u.id());
                try (var rs = ps.executeQuery()) { while (rs.next()) { /* inclusive filiação inativa */ } }
            }
        }
    }

    public boolean respostaNaComunidade(Connection c, long comunidadeId, long mensagemId) throws SQLException {
        try (var ps = c.prepareStatement("SELECT id_comunidade FROM comunidade_mensagens WHERE id_mensagem=? FOR SHARE")) {
            ps.setLong(1, mensagemId);
            try (var rs = ps.executeQuery()) { return rs.next() && rs.getLong(1) == comunidadeId; }
        }
    }

}
