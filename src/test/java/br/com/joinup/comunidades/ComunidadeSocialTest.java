package br.com.joinup.comunidades;

import br.com.joinup.comunidades.domain.*;
import br.com.joinup.comunidades.dto.*;
import br.com.joinup.comunidades.repository.ComunidadeSocialRepositoryJdbc;
import br.com.joinup.comunidades.service.ComunidadeSocialService;
import br.com.joinup.http.HttpException;
import br.com.joinup.usuarios.domain.Organizador;
import br.com.joinup.usuarios.domain.Participante;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ComunidadeSocialTest {
    private final Connection fakeConnection = (Connection) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[]{Connection.class},
            (proxy, method, args) -> null
    );

    private ComunidadeSocialRepositoryJdbc repo;
    private ComunidadeSocialService service;

    private final Participante participante1 = new Participante(1, "Ana", "ana@test.com", "hash", true, "11111111111", LocalDate.of(2000, 1, 1));
    private final Participante participanteSemIngresso = new Participante(2, "Carlos", "carlos@test.com", "hash", true, "22222222222", LocalDate.of(2001, 1, 1));
    private final Organizador organizadorDono = new Organizador(10, "Produtora X", "prod@test.com", "hash", true, "11222333000199");
    private final Organizador organizadorAlheio = new Organizador(20, "Outra Prod", "outra@test.com", "hash", true, "99888777000100");

    @BeforeEach
    void setup() {
        repo = new ComunidadeSocialRepositoryJdbc() {
            private boolean p1Membro = true;

            @Override
            public List<ComunidadeResumo> listarComunidades(Connection c, String busca) {
                return List.of(new ComunidadeResumo(1, "Comunidade Rock", "Desc", "img.jpg", 100, "Festival Rock", Instant.now()));
            }

            @Override
            public ComunidadeResumo buscarComunidade(Connection c, long id) {
                if (id == 1) return new ComunidadeResumo(1, "Comunidade Rock", "Desc", "img.jpg", 100, "Festival Rock", Instant.now());
                return null;
            }

            @Override
            public boolean verificarMembroAtivo(Connection c, long comunidadeId, long participanteId) {
                return participanteId == 1 && p1Membro;
            }

            @Override
            public boolean verificarIngressoElegivel(Connection c, long comunidadeId, long participanteId) {
                return participanteId == 1; // Participante 1 possui ingresso ativo ou utilizado (RF-COM-05)
            }

            @Override
            public boolean verificarOrganizadorDono(Connection c, long comunidadeId, long organizadorId) {
                return organizadorId == 10;
            }

            @Override
            public void entrarComunidade(Connection c, long comunidadeId, long participanteId) {
                p1Membro = true;
            }

            @Override
            public void sairComunidade(Connection c, long comunidadeId, long participanteId) {
                p1Membro = false;
            }

            @Override
            public List<MensagemComunidade> listarMensagens(Connection c, long comunidadeId) {
                return List.of(new MensagemComunidade(1, 1, "participante", 1L, null, "Ana", "Olá!", null, null, Instant.now()));
            }

            @Override
            public long enviarMensagem(Connection c, long comunidadeId, String autorTipo, Long partId, Long orgId, String msg, String img, Long resp) {
                return 42;
            }

            @Override
            public List<Postagem> listarPostagens(Connection c, long comunidadeId, Long participanteIdLogado) {
                return List.of(new Postagem(1, 1, 1, "Ana", "Post legal", null, 5, 2, Instant.now(), false));
            }

            @Override
            public Long buscarComunidadeIdDaPostagem(Connection c, long postagemId) {
                return postagemId == 1 ? 1L : null;
            }

            @Override
            public Postagem buscarPostagem(Connection c, long postagemId, Long participanteIdLogado) {
                if (postagemId == 1) return new Postagem(1, 1, 1, "Ana", "Post legal", null, 5, 2, Instant.now(), false);
                return null;
            }

            @Override
            public long criarPostagem(Connection c, long comunidadeId, long participanteId, String descricao, String imagem) {
                return 99;
            }

            @Override
            public void curtirPostagem(Connection c, long postagemId, long participanteId) {}

            @Override
            public void descurtirPostagem(Connection c, long postagemId, long participanteId) {}

            @Override
            public List<Comentario> listarComentarios(Connection c, long postagemId) {
                return List.of(new Comentario(1, 1, 1, "Ana", "Comentário show", Instant.now()));
            }

            @Override
            public long adicionarComentario(Connection c, long postagemId, long participanteId, String texto) {
                return 88;
            }
        };

        service = new ComunidadeSocialService(null, null, repo);
    }

    @Test
    void descobertaPublicaDeComunidadesSemLogin() throws Exception {
        var lista = service.listar(fakeConnection, null);
        assertEquals(1, lista.size());
        assertEquals("Comunidade Rock", lista.getFirst().nome());

        var detalhe = service.detalhe(fakeConnection, null, 1);
        assertEquals(1, detalhe.id());
        assertFalse(detalhe.membroAtivo());
        assertFalse(detalhe.elegivelParaEntrar());
    }

    @Test
    void participanteComIngressoEntraESaiDaComunidade() throws Exception {
        var detalheAntes = service.detalhe(fakeConnection, participante1, 1);
        assertTrue(detalheAntes.elegivelParaEntrar());

        assertDoesNotThrow(() -> service.sair(fakeConnection, participante1, 1));
        assertFalse(repo.verificarMembroAtivo(fakeConnection, 1, 1));

        assertDoesNotThrow(() -> service.entrar(fakeConnection, participante1, 1));
        assertTrue(repo.verificarMembroAtivo(fakeConnection, 1, 1));
    }

    @Test
    void participanteSemIngressoElegivelBloqueadoDeEntrar() {
        var ex = assertThrows(HttpException.class, () -> service.entrar(fakeConnection, participanteSemIngresso, 1));
        assertEquals(403, ex.status());
        assertEquals("ACESSO_COMUNIDADE_NEGADO", ex.codigo());
    }

    @Test
    void participanteNaoMembroBloqueadoDeAcessarMensagensEFeed() {
        var exMsg = assertThrows(HttpException.class, () -> service.listarMensagens(fakeConnection, participanteSemIngresso, 1));
        assertEquals(403, exMsg.status());

        var exFeed = assertThrows(HttpException.class, () -> service.listarPostagens(fakeConnection, participanteSemIngresso, 1));
        assertEquals(403, exFeed.status());
    }

    @Test
    void participanteMembroComIngressoAcessaMensagensEFeed() throws Exception {
        var mensagens = service.listarMensagens(fakeConnection, participante1, 1);
        assertEquals(1, mensagens.size());
        assertEquals("Olá!", mensagens.getFirst().mensagem());

        var posts = service.listarPostagens(fakeConnection, participante1, 1);
        assertEquals(1, posts.size());
        assertEquals("Post legal", posts.getFirst().descricao());
    }

    @Test
    void organizadorDonoAcessaMensagensMasOutroOrganizadorEBloqueado() throws Exception {
        var msgsDono = service.listarMensagens(fakeConnection, organizadorDono, 1);
        assertEquals(1, msgsDono.size());

        var ex = assertThrows(HttpException.class, () -> service.listarMensagens(fakeConnection, organizadorAlheio, 1));
        assertEquals(403, ex.status());
    }

    @Test
    void validacaoDeTextoEmMensagensEPostagens() {
        assertThrows(HttpException.class, () -> service.enviarMensagem(fakeConnection, participante1, 1, new EnviarMensagemRequest("", null, null)));
        assertThrows(HttpException.class, () -> service.enviarMensagem(fakeConnection, participante1, 1, new EnviarMensagemRequest("   ", null, null)));
        assertThrows(HttpException.class, () -> service.criarPostagem(fakeConnection, participante1, 1, new CriarPostagemRequest("", null)));
        assertThrows(HttpException.class, () -> service.adicionarComentario(fakeConnection, participante1, 1, new CriarComentarioRequest("")));
    }

    @Test
    void organizadorNaoPodeCriarPostagemOuComentarNoFeed() {
        assertThrows(HttpException.class, () -> service.criarPostagem(fakeConnection, organizadorDono, 1, new CriarPostagemRequest("Post do org", null)));
        assertThrows(HttpException.class, () -> service.adicionarComentario(fakeConnection, organizadorDono, 1, new CriarComentarioRequest("Comentário do org")));
    }

    @Test
    void curtirEDescurtirPostagem() {
        assertDoesNotThrow(() -> service.curtir(fakeConnection, participante1, 1));
        assertDoesNotThrow(() -> service.descurtir(fakeConnection, participante1, 1));
    }
}
