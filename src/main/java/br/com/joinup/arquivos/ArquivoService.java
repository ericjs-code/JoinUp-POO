package br.com.joinup.arquivos;

import br.com.joinup.auth.GerenciadorSessao;
import br.com.joinup.comunidades.service.ComunidadeSocialService;
import br.com.joinup.eventos.service.EventoService;
import br.com.joinup.eventos.service.ValidacaoEvento;
import br.com.joinup.http.HttpException;
import br.com.joinup.shared.db.ExecutorTransacional;
import br.com.joinup.usuarios.domain.*;
import java.io.IOException;
import java.security.*;
import java.sql.*;
import java.time.Clock;
import java.util.*;

public final class ArquivoService {
    private final ExecutorTransacional transacoes;
    private final GerenciadorSessao sessoes;
    private final ComunidadeSocialService social;
    private final EventoService eventos;
    private final ArmazenamentoLocal armazenamento;
    private final Clock clock;
    public ArquivoService(ExecutorTransacional t, GerenciadorSessao s, ComunidadeSocialService social,
                          EventoService eventos, ArmazenamentoLocal a, Clock clock) {
        transacoes=t; sessoes=s; this.social=social; this.eventos=eventos; armazenamento=a; this.clock=clock;
    }
    public record ArquivoView(String id, String imagem) { }
    public ArquivoView enviar(String token, MultipartImagem m) {
        // Decodificar antes de adquirir locks; elegibilidade será calculada após isso.
        byte[] b=ImagemSegura.normalizar(m);
        String id=UUID.randomUUID().toString().replace("-", ""), url=url(id);
        boolean[] criado={false};
        try {
            return transacoes.executar(c -> {
                Usuario u=sessoes.exigir(c,token,Perfil.PARTICIPANTE,Perfil.ORGANIZADOR);
                boolean evento=m.contexto().equals("evento");
                br.com.joinup.eventos.domain.Evento e=null;
                if (evento) {
                    if(u.perfil()!=Perfil.ORGANIZADOR) throw negado();
                    e=eventos.bloquearEditavel(c,m.alvoId(),u.id(),clock);
                } else {
                    if(m.contexto().equals("postagem") && u.perfil()!=Perfil.PARTICIPANTE) throw negado();
                    social.validarAcessoComunidade(c,m.alvoId(),u);
                }
                try { armazenamento.gravar(id,b); criado[0]=true; }
                catch(IOException ex) { throw new HttpException(503,"ARMAZENAMENTO_INDISPONIVEL","Não foi possível guardar a imagem"); }
                try(var p=c.prepareStatement("INSERT INTO arquivos(id,contexto,id_evento,id_comunidade,id_participante,id_organizador,tamanho,sha256) VALUES(?,?,?,?,?,?,?,?)")) {
                    p.setString(1,id); p.setString(2,m.contexto()); p.setObject(3,evento?m.alvoId():null); p.setObject(4,evento?null:m.alvoId());
                    p.setObject(5,u.perfil()==Perfil.PARTICIPANTE?u.id():null); p.setObject(6,u.perfil()==Perfil.ORGANIZADOR?u.id():null);
                    p.setInt(7,b.length); p.setString(8,hash(b)); p.executeUpdate();
                }
                if(evento) {
                    try(var p=c.prepareStatement("UPDATE eventos SET imagem=? WHERE id_evento=?")) { p.setString(1,url);p.setLong(2,m.alvoId());p.executeUpdate(); }
                    ValidacaoEvento.antesInicio(e.data(),clock.instant());
                }
                return new ArquivoView(id,url);
            });
        } catch(RuntimeException ex) {
            if(criado[0]) try {
                // Commit/close com resultado incerto: nunca remover conteúdo possivelmente confirmado.
                boolean persistido=transacoes.executar(c -> {
                    try(var p=c.prepareStatement("SELECT id FROM arquivos WHERE id=?")){p.setString(1,id);try(var r=p.executeQuery()){return r.next();}}
                });
                if(!persistido) armazenamento.apagar(id);
            } catch(IOException | RuntimeException cleanup) { System.getLogger(ArquivoService.class.getName()).log(System.Logger.Level.ERROR,"Limpeza de arquivo requer reconciliação local após falha"); }
            throw ex;
        }
    }
    /** Chamado após autorização do social e INSERT da entidade, na mesma transação. */
    public void vincular(Connection c, Usuario u, long comunidade, String contexto, String imagem, long entidade) throws SQLException {
        if(imagem==null) return;
        String id=idReferencia(imagem);
        try(var p=c.prepareStatement("SELECT * FROM arquivos WHERE id=? FOR UPDATE")) {
            p.setString(1,id);
            try(var r=p.executeQuery()) {
                if(!r.next() || !r.getString("contexto").equals(contexto) || r.getLong("id_comunidade")!=comunidade
                        || !dono(r,u) || r.getObject("id_postagem")!=null || r.getObject("id_mensagem")!=null
                        || r.getTimestamp("criado_em").toInstant().plusSeconds(86400).isBefore(clock.instant())) throw inexistente();
                verificarConteudo(id,r.getInt("tamanho"),r.getString("sha256"));
            }
        }
        String coluna=contexto.equals("postagem")?"id_postagem":"id_mensagem";
        try(var p=c.prepareStatement("UPDATE arquivos SET "+coluna+"=? WHERE id=?")) {p.setLong(1,entidade);p.setString(2,id);p.executeUpdate();}
    }
    public byte[] ler(String token, String id) {
        if(!id.matches("[a-f0-9]{32}")) throw inexistente();
        return transacoes.executar(c -> {
            // Pista sem lock: não inverter ordem conta -> evento -> arquivo.
            String contexto; long alvo;
            try(var p=c.prepareStatement("SELECT contexto,id_evento,id_comunidade FROM arquivos WHERE id=?")) {
                p.setString(1,id);try(var r=p.executeQuery()){if(!r.next())throw inexistente();contexto=r.getString(1);alvo=r.getLong(contexto.equals("evento")?2:3);}
            }
            Usuario u=null;
            if(!contexto.equals("evento")) {
                u=sessoes.exigir(c,token,Perfil.PARTICIPANTE,Perfil.ORGANIZADOR);
                social.validarAcessoComunidade(c,alvo,u);
            }
            try(var p=c.prepareStatement("SELECT * FROM arquivos WHERE id=? FOR SHARE")) {
                p.setString(1,id);try(var r=p.executeQuery()) {
                    if(!r.next())throw inexistente();
                    if(contexto.equals("evento")) {
                        if(!url(id).equals(eventos.buscar(c,alvo).imagem()))throw inexistente();
                    } else if(r.getObject("id_postagem")==null && r.getObject("id_mensagem")==null) {
                        if(!dono(r,u) || r.getTimestamp("criado_em").toInstant().plusSeconds(86400).isBefore(clock.instant()))throw inexistente();
                    }
                    return verificarConteudo(id,r.getInt("tamanho"),r.getString("sha256"));
                }
            }
        });
    }
    private byte[] verificarConteudo(String id,int tamanho,String sha) {
        byte[] b=armazenamento.ler(id);
        if(b.length!=tamanho || !hash(b).equals(sha))throw inexistente();
        return b;
    }
    private static boolean dono(ResultSet r,Usuario u) throws SQLException {
        return u!=null && r.getLong(u.perfil()==Perfil.PARTICIPANTE?"id_participante":"id_organizador")==u.id();
    }
    public static String idReferencia(String imagem) {
        if(!imagem.matches("/api/arquivos/[a-f0-9]{32}")) throw new HttpException(400,"IMAGEM_NAO_SUPORTADA","Use uma imagem enviada e autorizada neste contexto");
        return imagem.substring("/api/arquivos/".length());
    }
    private static String url(String id) {return "/api/arquivos/"+id;}
    private static String hash(byte[] b) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    private static HttpException negado(){return new HttpException(403,"PERMISSAO_NEGADA","Perfil sem permissão neste contexto");}
    private static HttpException inexistente(){return new HttpException(404,"RECURSO_NAO_ENCONTRADO","Imagem indisponível neste contexto");}
}
