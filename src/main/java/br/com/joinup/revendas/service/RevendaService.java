package br.com.joinup.revendas.service;

import br.com.joinup.auth.GerenciadorSessao;
import br.com.joinup.compras.domain.*;
import br.com.joinup.compras.repository.*;
import br.com.joinup.eventos.repository.TipoIngressoRepositoryJdbc;
import br.com.joinup.http.HttpException;
import br.com.joinup.revendas.domain.Anuncio;
import br.com.joinup.revendas.dto.*;
import br.com.joinup.revendas.repository.RevendaRepositoryJdbc;
import br.com.joinup.shared.db.ExecutorTransacional;
import br.com.joinup.usuarios.domain.Perfil;
import java.sql.*;
import java.time.Clock;
import java.util.*;
import static br.com.joinup.compras.service.ValidacaoComercio.*;
import static br.com.joinup.eventos.service.ValidacaoEvento.naoEncontrado;

public final class RevendaService {
    private final ExecutorTransacional transacoes;
    private final GerenciadorSessao sessoes;
    private final CompraRepositoryJdbc eventos;
    private final IngressoRepositoryJdbc ingressos;
    private final TipoIngressoRepositoryJdbc tipos;
    private final RevendaRepositoryJdbc revendas;
    private final Clock clock;
    public RevendaService(ExecutorTransacional t,GerenciadorSessao s,CompraRepositoryJdbc e,IngressoRepositoryJdbc i,TipoIngressoRepositoryJdbc tipos,RevendaRepositoryJdbc r,Clock clock) {
        transacoes=t;sessoes=s;eventos=e;ingressos=i;this.tipos=tipos;revendas=r;this.clock=clock;
    }
    public List<Map<String,Object>> anunciar(String token,AnuncioDados dados) {
        if(dados==null) throw new HttpException(422,"REQUISICAO_INVALIDA","Informe o anúncio");
        var ids=ingressos(dados.ingressoIds()); var preco=precoRevenda(dados.precoUnitario());
        return transacoes.executar(c -> {
            var pistas=new TreeMap<Long,Ingresso>(); var eventosPistas=new TreeMap<Long,EventoComercial>();
            for(long id:ids) {
                var i=ingressos.buscar(c,id,false); if(i==null) throw naoEncontrado(); pistas.put(id,i);
                var e=eventos.evento(c,i.eventoId(),false); if(e==null) throw naoEncontrado(); eventosPistas.put(e.id(),e);
            }
            long vendedor=sessoes.exigirParticipanteComContas(c,token,List.of(),eventosPistas.values().stream().map(EventoComercial::organizadorId).toList()).id();
            for(var e:eventosPistas.values()) {
                var atual=eventos.evento(c,e.id(),true); if(atual==null || atual.organizadorId()!=e.organizadorId()) throw indisponivel();
                antesInicio(atual.data(),clock.instant()); eventosPistas.put(e.id(),atual);
            }
            bloquearTipos(c,pistas.values());
            // Ingressos em ordem crescente, depois todos os anúncios. Sem mutação parcial do lote.
            for(var pista:pistas.values()) validarIngresso(ingressos.buscar(c,pista.id(),true),pista,vendedor);
            for(long id:ids) if(revendas.disponivel(c,id)) throw indisponivel();
            var criados=new ArrayList<Long>(); for(long id:ids) criados.add(revendas.criar(c,id,vendedor,preco));
            for(var e:eventosPistas.values()) antesInicio(e.data(),clock.instant());
            var resposta=new ArrayList<Map<String,Object>>(); for(long id:criados) resposta.add(revendas.detalhe(c,vendedor,id,clock.instant()));
            return List.copyOf(resposta);
        });
    }
    public Map<String,Object> comprar(String token,long anuncioId) {
        return transacoes.executar(c -> {
            Anuncio pista=revendas.buscar(c,anuncioId,false); if(pista==null) throw naoEncontrado();
            Ingresso i=ingressos.buscar(c,pista.ingressoId(),false); if(i==null) throw indisponivel();
            EventoComercial e=eventos.evento(c,i.eventoId(),false); if(e==null) throw indisponivel();
            long comprador=sessoes.exigirParticipanteComContas(c,token,List.of(pista.vendedorId()),List.of(e.organizadorId())).id();
            if(comprador==pista.vendedorId()) throw new HttpException(403,"PERMISSAO_NEGADA","Você não pode comprar seu próprio anúncio");
            var atual=eventos.evento(c,e.id(),true); if(atual==null || atual.organizadorId()!=e.organizadorId()) throw indisponivel();
            antesInicio(atual.data(),clock.instant()); bloquearTipos(c,List.of(i));
            var ingresso=ingressos.buscar(c,i.id(),true); validarIngresso(ingresso,i,pista.vendedorId());
            var anuncio=revendas.buscar(c,anuncioId,true); validarAnuncio(anuncio,pista,pista.vendedorId());
            var preco=precoRevenda(anuncio.preco());
            ingressos.transferir(c,ingresso,comprador); revendas.vender(c,anuncioId,comprador);
            ingressos.sairSemIngressoElegivel(c,pista.vendedorId(),e.id());
            antesInicio(atual.data(),clock.instant());
            return Map.of("anuncioId",anuncioId,"ingressoId",i.id(),"status","vendido","total",preco,"pagamento","simulado");
        });
    }
    public Map<String,Object> cancelar(String token,long anuncioId) {
        return transacoes.executar(c -> {
            long vendedor=sessoes.exigir(c,token,Perfil.PARTICIPANTE).id();
            var pista=revendas.buscar(c,anuncioId,false); if(pista==null || pista.vendedorId()!=vendedor) throw naoEncontrado();
            var i=ingressos.buscar(c,pista.ingressoId(),false); if(i==null) throw indisponivel();
            eventos.evento(c,i.eventoId(),true); bloquearTipos(c,List.of(i));
            var ingresso=ingressos.buscar(c,i.id(),true);
            var anuncio=revendas.buscar(c,anuncioId,true); validarAnuncio(anuncio,pista,vendedor);
            if(ingresso==null || ingresso.participanteId()!=vendedor) throw indisponivel();
            // Cancelar é permitido durante inatividade do organizador e depois do início.
            revendas.cancelar(c,anuncioId,vendedor); return revendas.detalhe(c,vendedor,anuncioId,clock.instant());
        });
    }
    public Map<String,Object> listar(String token,FiltroRevendas filtro) {
        return transacoes.executar(c -> {
            long usuario=sessoes.exigir(c,token,Perfil.PARTICIPANTE).id();
            return Map.of("dados",revendas.listar(c,usuario,filtro,false,clock.instant()),"pagina",filtro.pagina(),"tamanho",filtro.tamanho(),"total",revendas.contar(c,filtro));
        });
    }
    public List<Map<String,Object>> meus(String token) { return transacoes.executar(c -> revendas.listar(c,sessoes.exigir(c,token,Perfil.PARTICIPANTE).id(),null,true,clock.instant())); }
    public Map<String,Object> detalhe(String token,long id) {
        return transacoes.executar(c -> { var a=revendas.detalhe(c,sessoes.exigir(c,token,Perfil.PARTICIPANTE).id(),id,clock.instant()); if(a==null) throw naoEncontrado(); return a; });
    }
    private void bloquearTipos(Connection c,Collection<Ingresso> lista) throws SQLException {
        var ordenados=new TreeMap<Long,Long>(); for(var i:lista) ordenados.put(i.tipoIngressoId(),i.eventoId());
        for(var t:ordenados.entrySet()) if(tipos.bloquear(c,t.getValue(),t.getKey())==null) throw indisponivel();
    }
    private static void validarIngresso(Ingresso i,Ingresso pista,long dono) {
        if(i==null || i.participanteId()!=dono || !i.status().equals("ativo") || i.eventoId()!=pista.eventoId() || i.tipoIngressoId()!=pista.tipoIngressoId()) throw indisponivel();
    }
    private static void validarAnuncio(Anuncio a,Anuncio pista,long dono) {
        if(a==null || a.vendedorId()!=dono || a.ingressoId()!=pista.ingressoId() || !a.status().equals("disponivel")) throw indisponivel();
    }
}
