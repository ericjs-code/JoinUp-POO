package br.com.joinup.comunidades.service;

import br.com.joinup.auth.GerenciadorSessao;
import br.com.joinup.comunidades.domain.*;
import br.com.joinup.comunidades.dto.*;
import br.com.joinup.comunidades.repository.ComunidadeSocialRepositoryJdbc;
import br.com.joinup.http.HttpException;
import br.com.joinup.shared.db.ExecutorTransacional;
import br.com.joinup.usuarios.domain.Perfil;
import br.com.joinup.usuarios.domain.Usuario;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public final class ComunidadeSocialService {
    private final ExecutorTransacional transacoes;
    private final GerenciadorSessao sessoes;
    private final ComunidadeSocialRepositoryJdbc repository;
    private br.com.joinup.arquivos.ArquivoService arquivos;

    public void configurarArquivos(br.com.joinup.arquivos.ArquivoService arquivos) {
        if (this.arquivos != null) throw new IllegalStateException("Mídia já configurada");
        this.arquivos = java.util.Objects.requireNonNull(arquivos);
    }

    public ComunidadeSocialService(ExecutorTransacional transacoes, GerenciadorSessao sessoes, ComunidadeSocialRepositoryJdbc repository) {
        this.transacoes = transacoes;
        this.sessoes = sessoes;
        this.repository = repository;
    }

    public List<ComunidadeResumo> listar(String busca) {
        return transacoes.executar(c -> listar(c, busca));
    }

    public List<ComunidadeResumo> listar(Connection c, String busca) throws SQLException {
        validarBusca(busca);
        return repository.listarComunidades(c, busca);
    }

    public ComunidadeDetalheView detalhe(String tokenOpcional, long comunidadeId) {
        return transacoes.executar(c -> {
            Usuario u = null;
            if (tokenOpcional != null && !tokenOpcional.isBlank()) {
                try {
                    u = sessoes.exigir(c, tokenOpcional);
                } catch (HttpException ignored) {
                    // Token inválido não bloqueia visualização pública
                }
            }
            return detalhe(c, u, comunidadeId);
        });
    }

    public ComunidadeDetalheView detalhe(Connection c, Usuario usuarioOpcional, long comunidadeId) throws SQLException {
        var com = repository.buscarComunidade(c, comunidadeId);
        if (com == null) {
            throw new HttpException(404, "RECURSO_NAO_ENCONTRADO", "Comunidade não encontrada");
        }
        boolean membro = false;
        boolean elegivel = false;
        if (usuarioOpcional != null) {
            if (usuarioOpcional.perfil() == Perfil.PARTICIPANTE) {
                membro = repository.verificarMembroAtivo(c, comunidadeId, usuarioOpcional.id());
                elegivel = repository.verificarIngressoElegivel(c, comunidadeId, usuarioOpcional.id());
            } else if (usuarioOpcional.perfil() == Perfil.ORGANIZADOR) {
                membro = repository.verificarOrganizadorDono(c, comunidadeId, usuarioOpcional.id());
                elegivel = membro;
            }
        }
        return new ComunidadeDetalheView(
                com.id(),
                com.nome(),
                com.descricao(),
                com.imagem(),
                com.eventoId(),
                com.eventoNome(),
                com.dataCriacao(),
                membro,
                elegivel,
                membro && elegivel
        );
    }

    public void entrar(String token, long comunidadeId) {
        transacoes.executar(c -> {
            Usuario u = sessoes.exigir(c, token, Perfil.PARTICIPANTE);
            entrar(c, u, comunidadeId);
            return null;
        });
    }

    public void entrar(Connection c, Usuario u, long comunidadeId) throws SQLException {
        if (u.perfil() != Perfil.PARTICIPANTE) throw new HttpException(403, "PERMISSAO_NEGADA", "Apenas participantes podem alterar filiação");
        repository.bloquearAcesso(c, comunidadeId, u);
        var com = repository.buscarComunidade(c, comunidadeId);
        if (com == null) {
            throw new HttpException(404, "RECURSO_NAO_ENCONTRADO", "Comunidade não encontrada");
        }
        boolean elegivel = repository.verificarIngressoElegivel(c, comunidadeId, u.id());
        if (!elegivel) {
            throw new HttpException(403, "ACESSO_COMUNIDADE_NEGADO", "Para entrar na comunidade é necessário possuir ingresso ativo ou utilizado");
        }
        repository.entrarComunidade(c, comunidadeId, u.id());
    }

    public void sair(String token, long comunidadeId) {
        transacoes.executar(c -> {
            Usuario u = sessoes.exigir(c, token, Perfil.PARTICIPANTE);
            sair(c, u, comunidadeId);
            return null;
        });
    }

    public void sair(Connection c, Usuario u, long comunidadeId) throws SQLException {
        if (u.perfil() != Perfil.PARTICIPANTE) throw new HttpException(403, "PERMISSAO_NEGADA", "Apenas participantes podem alterar filiação");
        repository.bloquearAcesso(c, comunidadeId, u);
        var com = repository.buscarComunidade(c, comunidadeId);
        if (com == null) {
            throw new HttpException(404, "RECURSO_NAO_ENCONTRADO", "Comunidade não encontrada");
        }
        repository.sairComunidade(c, comunidadeId, u.id());
    }

    public List<MensagemComunidade> listarMensagens(String token, long comunidadeId) {
        return transacoes.executar(c -> {
            Usuario u = sessoes.exigir(c, token, Perfil.PARTICIPANTE, Perfil.ORGANIZADOR);
            return listarMensagens(c, u, comunidadeId);
        });
    }

    public List<MensagemComunidade> listarMensagens(Connection c, Usuario u, long comunidadeId) throws SQLException {
        validarAcessoComunidade(c, comunidadeId, u);
        return repository.listarMensagens(c, comunidadeId);
    }

    public MensagemComunidade enviarMensagem(String token, long comunidadeId, EnviarMensagemRequest req) {
        return transacoes.executar(c -> {
            Usuario u = sessoes.exigir(c, token, Perfil.PARTICIPANTE, Perfil.ORGANIZADOR);
            return enviarMensagem(c, u, comunidadeId, req);
        });
    }

    public MensagemComunidade enviarMensagem(Connection c, Usuario u, long comunidadeId, EnviarMensagemRequest req) throws SQLException {
        if (req == null || ((req.mensagem() == null || req.mensagem().trim().isEmpty()) && req.imagem()==null)) {
            throw new HttpException(400, "REQUISICAO_INVALIDA", "Mensagem não pode ser vazia");
        }
        if (req.mensagem()!=null && req.mensagem().length() > 1000) {
            throw new HttpException(400, "REQUISICAO_INVALIDA", "Mensagem não pode exceder 1000 caracteres");
        }
        validarImagem(req.imagem());
        validarAcessoComunidade(c, comunidadeId, u);
        if (req.idRespostaA() != null) {
            if (req.idRespostaA() <= 0 || req.idRespostaA() > Integer.MAX_VALUE)
                throw new HttpException(400, "REQUISICAO_INVALIDA", "ID de resposta inválido");
            if (!repository.respostaNaComunidade(c, comunidadeId, req.idRespostaA()))
                throw new HttpException(404, "RECURSO_NAO_ENCONTRADO", "Mensagem de resposta não encontrada nesta comunidade");
        }
        String autorTipo = u.perfil() == Perfil.ORGANIZADOR ? "organizador" : "participante";
        Long partId = u.perfil() == Perfil.PARTICIPANTE ? u.id() : null;
        Long orgId = u.perfil() == Perfil.ORGANIZADOR ? u.id() : null;

        long id = repository.enviarMensagem(c, comunidadeId, autorTipo, partId, orgId, req.mensagem()==null?"":req.mensagem().trim(), req.imagem(), req.idRespostaA());
        if(req.imagem()!=null) arquivos.vincular(c,u,comunidadeId,"mensagem",req.imagem(),id);
        var todas = repository.listarMensagens(c, comunidadeId, 1, 0, id);
        return todas.stream().filter(m -> m.id() == id).findFirst().orElseThrow();
    }

    public List<Postagem> listarPostagens(String token, long comunidadeId) {
        return transacoes.executar(c -> {
            Usuario u = sessoes.exigir(c, token, Perfil.PARTICIPANTE, Perfil.ORGANIZADOR);
            return listarPostagens(c, u, comunidadeId);
        });
    }

    public List<Postagem> listarPostagens(Connection c, Usuario u, long comunidadeId) throws SQLException {
        validarAcessoComunidade(c, comunidadeId, u);
        Long partId = u.perfil() == Perfil.PARTICIPANTE ? u.id() : null;
        return repository.listarPostagens(c, comunidadeId, partId);
    }

    public Postagem criarPostagem(String token, long comunidadeId, CriarPostagemRequest req) {
        return transacoes.executar(c -> {
            Usuario u = sessoes.exigir(c, token, Perfil.PARTICIPANTE);
            return criarPostagem(c, u, comunidadeId, req);
        });
    }

    public Postagem criarPostagem(Connection c, Usuario u, long comunidadeId, CriarPostagemRequest req) throws SQLException {
        if (u.perfil() != Perfil.PARTICIPANTE) {
            throw new HttpException(403, "PERMISSAO_NEGADA", "Apenas participantes podem criar postagens no feed");
        }
        if (req == null || ((req.descricao() == null || req.descricao().trim().isEmpty()) && req.imagem()==null)) {
            throw new HttpException(400, "REQUISICAO_INVALIDA", "Descrição da postagem não pode ser vazia");
        }
        if (req.descricao()!=null && req.descricao().length() > 500) {
            throw new HttpException(400, "REQUISICAO_INVALIDA", "Descrição não pode exceder 500 caracteres");
        }
        validarImagem(req.imagem());
        validarAcessoComunidade(c, comunidadeId, u);
        long id = repository.criarPostagem(c, comunidadeId, u.id(), req.descricao()==null?"":req.descricao().trim(), req.imagem());
        if(req.imagem()!=null) arquivos.vincular(c,u,comunidadeId,"postagem",req.imagem(),id);
        return repository.buscarPostagem(c, id, u.id());
    }

    public Postagem detalhePostagem(String token, long postagemId) {
        return transacoes.executar(c -> {
            Usuario u = sessoes.exigir(c, token, Perfil.PARTICIPANTE, Perfil.ORGANIZADOR);
            return detalhePostagem(c, u, postagemId);
        });
    }

    public Postagem detalhePostagem(Connection c, Usuario u, long postagemId) throws SQLException {
        Long comId = repository.buscarComunidadeIdDaPostagem(c, postagemId);
        if (comId == null) {
            throw new HttpException(404, "RECURSO_NAO_ENCONTRADO", "Postagem não encontrada");
        }
        validarAcessoComunidade(c, comId, u);
        Long partId = u.perfil() == Perfil.PARTICIPANTE ? u.id() : null;
        Postagem post = repository.buscarPostagem(c, postagemId, partId);
        if (post == null) throw new HttpException(404, "RECURSO_NAO_ENCONTRADO", "Postagem não encontrada");
        return post;
    }

    public Postagem curtir(String token, long postagemId) {
        return transacoes.executar(c -> {
            Usuario u = sessoes.exigir(c, token, Perfil.PARTICIPANTE);
            return curtir(c, u, postagemId);
        });
    }

    public Postagem curtir(Connection c, Usuario u, long postagemId) throws SQLException {
        if (u.perfil() != Perfil.PARTICIPANTE) {
            throw new HttpException(403, "PERMISSAO_NEGADA", "Apenas participantes podem curtir postagens");
        }
        Long comId = repository.buscarComunidadeIdDaPostagem(c, postagemId);
        if (comId == null) {
            throw new HttpException(404, "RECURSO_NAO_ENCONTRADO", "Postagem não encontrada");
        }
        validarAcessoComunidade(c, comId, u);
        repository.curtirPostagem(c, postagemId, u.id());
        return repository.buscarPostagem(c, postagemId, u.id());
    }

    public Postagem descurtir(String token, long postagemId) {
        return transacoes.executar(c -> {
            Usuario u = sessoes.exigir(c, token, Perfil.PARTICIPANTE);
            return descurtir(c, u, postagemId);
        });
    }

    public Postagem descurtir(Connection c, Usuario u, long postagemId) throws SQLException {
        if (u.perfil() != Perfil.PARTICIPANTE) {
            throw new HttpException(403, "PERMISSAO_NEGADA", "Apenas participantes podem descurtir postagens");
        }
        Long comId = repository.buscarComunidadeIdDaPostagem(c, postagemId);
        if (comId == null) {
            throw new HttpException(404, "RECURSO_NAO_ENCONTRADO", "Postagem não encontrada");
        }
        validarAcessoComunidade(c, comId, u);
        repository.descurtirPostagem(c, postagemId, u.id());
        return repository.buscarPostagem(c, postagemId, u.id());
    }

    public List<Comentario> listarComentarios(String token, long postagemId) {
        return transacoes.executar(c -> {
            Usuario u = sessoes.exigir(c, token, Perfil.PARTICIPANTE, Perfil.ORGANIZADOR);
            return listarComentarios(c, u, postagemId);
        });
    }

    public List<Comentario> listarComentarios(Connection c, Usuario u, long postagemId) throws SQLException {
        Long comId = repository.buscarComunidadeIdDaPostagem(c, postagemId);
        if (comId == null) {
            throw new HttpException(404, "RECURSO_NAO_ENCONTRADO", "Postagem não encontrada");
        }
        validarAcessoComunidade(c, comId, u);
        return repository.listarComentarios(c, postagemId);
    }

    public Comentario adicionarComentario(String token, long postagemId, CriarComentarioRequest req) {
        return transacoes.executar(c -> {
            Usuario u = sessoes.exigir(c, token, Perfil.PARTICIPANTE);
            return adicionarComentario(c, u, postagemId, req);
        });
    }

    public Comentario adicionarComentario(Connection c, Usuario u, long postagemId, CriarComentarioRequest req) throws SQLException {
        if (u.perfil() != Perfil.PARTICIPANTE) {
            throw new HttpException(403, "PERMISSAO_NEGADA", "Apenas participantes podem comentar no feed");
        }
        if (req == null || req.texto() == null || req.texto().trim().isEmpty()) {
            throw new HttpException(400, "REQUISICAO_INVALIDA", "Texto do comentário não pode ser vazio");
        }
        if (req.texto().length() > 500) {
            throw new HttpException(400, "REQUISICAO_INVALIDA", "Texto não pode exceder 500 caracteres");
        }
        Long comId = repository.buscarComunidadeIdDaPostagem(c, postagemId);
        if (comId == null) {
            throw new HttpException(404, "RECURSO_NAO_ENCONTRADO", "Postagem não encontrada");
        }
        validarAcessoComunidade(c, comId, u);
        long id = repository.adicionarComentario(c, postagemId, u.id(), req.texto().trim());
        var todos = repository.listarComentarios(c, postagemId, 1, 0, id);
        return todos.stream().filter(com -> com.id() == id).findFirst().orElseThrow();
    }

    public void validarAcessoComunidade(Connection c, long comunidadeId, Usuario usuario) throws SQLException {
        repository.bloquearAcesso(c, comunidadeId, usuario);
        if (usuario.perfil() == Perfil.PARTICIPANTE) {
            boolean membro = repository.verificarMembroAtivo(c, comunidadeId, usuario.id());
            boolean ingresso = repository.verificarIngressoElegivel(c, comunidadeId, usuario.id());
            if (!membro || !ingresso) {
                throw new HttpException(403, "ACESSO_COMUNIDADE_NEGADO", "Acesso à comunidade exige filiação ativa e ingresso elegível");
            }
        } else if (usuario.perfil() == Perfil.ORGANIZADOR) {
            boolean dono = repository.verificarOrganizadorDono(c, comunidadeId, usuario.id());
            if (!dono) {
                throw new HttpException(403, "ACESSO_COMUNIDADE_NEGADO", "Organizador não tem acesso à comunidade deste evento");
            }
        } else {
            throw new HttpException(403, "ACESSO_COMUNIDADE_NEGADO", "Perfil não autorizado para a comunidade");
        }
    }
    private void validarImagem(String imagem) {
        if(imagem==null) return;
        if(arquivos==null) throw new HttpException(400,"IMAGEM_NAO_SUPORTADA","Mídia indisponível");
        br.com.joinup.arquivos.ArquivoService.idReferencia(imagem);
    }
    private static void validarBusca(String busca) {
        if (busca != null && busca.length() > 100) throw new HttpException(400, "REQUISICAO_INVALIDA", "Busca limitada a 100 caracteres");
    }
    private static int deslocamento(int pagina, int tamanho) {
        if (pagina < 1 || pagina > 10000 || tamanho < 1 || tamanho > 100)
            throw new HttpException(400, "REQUISICAO_INVALIDA", "Página ou tamanho inválido");
        return (pagina - 1) * tamanho;
    }
    public List<ComunidadeResumo> listar(String busca, int pagina, int tamanho) {
        validarBusca(busca); int offset = deslocamento(pagina, tamanho);
        return transacoes.executar(c -> repository.listarComunidades(c, busca, tamanho, offset));
    }
    public List<MensagemComunidade> listarMensagens(String token, long comunidadeId, int pagina, int tamanho) {
        int offset = deslocamento(pagina, tamanho);
        return transacoes.executar(c -> {
            Usuario u = sessoes.exigir(c, token, Perfil.PARTICIPANTE, Perfil.ORGANIZADOR);
            validarAcessoComunidade(c, comunidadeId, u);
            return repository.listarMensagens(c, comunidadeId, tamanho, offset);
        });
    }
    public List<Postagem> listarPostagens(String token, long comunidadeId, int pagina, int tamanho) {
        int offset = deslocamento(pagina, tamanho);
        return transacoes.executar(c -> {
            Usuario u = sessoes.exigir(c, token, Perfil.PARTICIPANTE, Perfil.ORGANIZADOR);
            validarAcessoComunidade(c, comunidadeId, u);
            return repository.listarPostagens(c, comunidadeId, u.perfil() == Perfil.PARTICIPANTE ? u.id() : null, tamanho, offset);
        });
    }
    public List<Comentario> listarComentarios(String token, long postagemId, int pagina, int tamanho) {
        int offset = deslocamento(pagina, tamanho);
        return transacoes.executar(c -> {
            Usuario u = sessoes.exigir(c, token, Perfil.PARTICIPANTE, Perfil.ORGANIZADOR);
            Long comunidadeId = repository.buscarComunidadeIdDaPostagem(c, postagemId);
            if (comunidadeId == null) throw new HttpException(404, "RECURSO_NAO_ENCONTRADO", "Postagem não encontrada");
            validarAcessoComunidade(c, comunidadeId, u);
            return repository.listarComentarios(c, postagemId, tamanho, offset);
        });
    }

}
