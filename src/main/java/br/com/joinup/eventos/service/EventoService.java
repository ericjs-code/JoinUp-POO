package br.com.joinup.eventos.service;

import br.com.joinup.eventos.domain.Evento;
import br.com.joinup.eventos.dto.*;
import br.com.joinup.eventos.repository.EventoRepositoryJdbc;
import br.com.joinup.http.HttpException;
import java.sql.*;
import java.time.*;
import java.util.List;
import static br.com.joinup.eventos.service.ValidacaoEvento.*;

public final class EventoService {
    private final EventoRepositoryJdbc eventos;
    public EventoService(EventoRepositoryJdbc eventos) { this.eventos=eventos; }
    public Evento criar(Connection c,CriarEventoDados d,long dono,Instant agora) throws SQLException {
        if(d==null) throw invalido("evento");
        Evento e=validar(c,0,d.nome(),d.descricao(),dataFutura(d.data(),agora),d.imagem(),d.localizacao(),d.cidade(),d.estado(),d.cep(),d.generoId(),dono);
        long id=eventos.criar(c,e); return buscar(c,id);
    }
    public Evento bloquearEditavel(Connection c,long id,long dono,Clock clock) throws SQLException {
        Evento e=eventos.buscar(c,id,true);
        if(e==null) throw naoEncontrado();
        if(e.organizadorId()!=dono) throw new HttpException(403,"PERMISSAO_NEGADA","Evento de outro organizador");
        antesInicio(e.data(),clock.instant()); return e;
    }
    public Evento editar(Connection c,Evento e,EditarEventoDados d,Instant agora) throws SQLException {
        if(d==null || d.vazio()) throw invalido("evento vazio");
        // Instante observado após a aquisição do lock, antes da persistência.
        antesInicio(e.data(),agora);
        Evento novo=validar(c,e.id(),ou(d.nome(),e.nome()),ou(d.descricao(),e.descricao()),d.data()==null?e.data():dataFutura(d.data(),agora),
                ou(d.imagem(),e.imagem()),ou(d.localizacao(),e.localizacao()),ou(d.cidade(),e.cidade()),ou(d.estado(),e.estado()),ou(d.cep(),e.cep()),
                d.generoId()==null?e.generoId():d.generoId(),e.organizadorId());
        eventos.atualizar(c,novo); return buscar(c,e.id());
    }
    public Evento buscar(Connection c,long id) throws SQLException {
        Evento e=eventos.buscar(c,id,false); if(e==null) throw naoEncontrado(); return e;
    }
    public List<Evento> listar(Connection c,FiltroEventos f,Long dono) throws SQLException { return eventos.listar(c,f,dono); }
    public long contar(Connection c,FiltroEventos f,Long dono) throws SQLException { return eventos.contar(c,f,dono); }
    private Evento validar(Connection c,long id,String nome,String descricao,Instant data,String imagem,String local,String cidade,String uf,String cep,Long genero,long dono) throws SQLException {
        long g=id(genero,"generoId"); if(!eventos.generoExiste(c,g)) throw invalido("generoId inexistente");
        String imagemValidada;
        if(id>0 && imagem!=null && imagem.matches("/api/arquivos/[a-f0-9]{32}") && imagem.equals(buscar(c,id).imagem())) imagemValidada=imagem;
        else imagemValidada=imagem(imagem);
        return new Evento(id,texto(nome,45,"nome"),data,texto(descricao,450,"descricao"),imagemValidada,texto(local,45,"localizacao"),texto(cidade,60,"cidade"),uf(uf),cep(cep),dono,g,null);
    }
    private static String ou(String novo,String atual) { return novo==null?atual:novo; }
}
