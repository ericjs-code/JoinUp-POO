package br.com.joinup.eventos.facade;

import br.com.joinup.auth.GerenciadorSessao;
import br.com.joinup.eventos.domain.*;
import br.com.joinup.eventos.dto.*;
import br.com.joinup.eventos.service.*;
import br.com.joinup.shared.db.ExecutorTransacional;
import br.com.joinup.usuarios.domain.Perfil;
import java.sql.*;
import java.time.Clock;
import java.util.*;

/** GoF Facade: API única coordenando quatro subsistemas, commit apenas após sucesso integral. */
public final class EventoFacade {
    private final ExecutorTransacional transacoes;
    private final GerenciadorSessao sessoes;
    private final EventoService eventos;
    private final ArtistaService artistas;
    private final TipoIngressoService tipos;
    private final ComunidadeService comunidades;
    private final Clock clock;
    public EventoFacade(ExecutorTransacional t,GerenciadorSessao s,EventoService e,ArtistaService a,
                        TipoIngressoService i,ComunidadeService c,Clock clock) {
        transacoes=t; sessoes=s; eventos=e; artistas=a; tipos=i; comunidades=c; this.clock=clock;
    }
    public EventoView criarEvento(String token,CriarEventoDados dados) {
        return transacoes.executar(c -> criarIntegrado(c,sessoes.exigir(c,token,Perfil.ORGANIZADOR).id(),dados));
    }
    // Visibilidade de pacote permite testar a coordenação com serviços reais e repositórios de teste.
    EventoView criarIntegrado(Connection c,long dono,CriarEventoDados d) throws SQLException {
        Evento e=eventos.criar(c,d,dono,clock.instant());
        artistas.sincronizarLineup(c,e.id(),d.artistaIds());
        tipos.cadastrarTipos(c,e.id(),d.tiposIngresso());
        comunidades.criarParaEvento(c,e);
        ValidacaoEvento.antesInicio(e.data(),clock.instant());
        return view(c,e);
    }
    public EventoView editarEvento(String token,long id,EditarEventoDados d) {
        return transacoes.executar(c -> {
            long dono=sessoes.exigir(c,token,Perfil.ORGANIZADOR).id();
            Evento atual=eventos.bloquearEditavel(c,id,dono,clock);
            Evento novo=eventos.editar(c,atual,d,clock.instant());
            if(d.artistaIds()!=null) artistas.sincronizarLineup(c,id,d.artistaIds());
            // Revalidar depois dos locks de referências/lineup; qualquer falha reverte o conjunto.
            ValidacaoEvento.antesInicio(atual.data(),clock.instant());
            if(d.data()!=null) ValidacaoEvento.dataFutura(d.data(),clock.instant());
            return view(c,novo);
        });
    }
    public TipoIngresso adicionarTipo(String token,long evento,TipoIngressoDados d) {
        return transacoes.executar(c -> {
            long dono=sessoes.exigir(c,token,Perfil.ORGANIZADOR).id();
            Evento e=eventos.bloquearEditavel(c,evento,dono,clock);
            long id=tipos.adicionar(c,evento,d);
            ValidacaoEvento.antesInicio(e.data(),clock.instant());
            return tipos.listar(c,evento).stream().filter(t -> t.id()==id).findFirst().orElseThrow();
        });
    }
    public TipoIngresso editarTipo(String token,long evento,long tipo,TipoIngressoDados d) {
        return transacoes.executar(c -> {
            long dono=sessoes.exigir(c,token,Perfil.ORGANIZADOR).id();
            Evento e=eventos.bloquearEditavel(c,evento,dono,clock);
            tipos.editar(c,evento,tipo,d);
            ValidacaoEvento.antesInicio(e.data(),clock.instant());
            return tipos.listar(c,evento).stream().filter(t -> t.id()==tipo).findFirst().orElseThrow();
        });
    }
    public EventoView consultar(long id) { return transacoes.executar(c -> view(c,eventos.buscar(c,id))); }
    public List<TipoIngresso> tiposIngresso(long evento) {
        return transacoes.executar(c -> { eventos.buscar(c,evento); return tipos.listar(c,evento); });
    }
    public Map<String,Object> explorar(FiltroEventos f) { return transacoes.executar(c -> listar(c,f,null)); }
    public Map<String,Object> meusEventos(String token,FiltroEventos f) {
        return transacoes.executar(c -> listar(c,f,sessoes.exigir(c,token,Perfil.ORGANIZADOR).id()));
    }
    public List<ItemCatalogo> generos() { return transacoes.executar(artistas::generos); }
    public List<ItemCatalogo> artistas(String busca) { return transacoes.executar(c -> artistas.buscar(c,busca)); }
    private Map<String,Object> listar(Connection c,FiltroEventos f,Long dono) throws SQLException {
        var lista=new ArrayList<EventoView>();
        for(Evento e:eventos.listar(c,f,dono)) lista.add(view(c,e));
        return Map.of("dados",List.copyOf(lista),"pagina",f.pagina(),"tamanho",f.tamanho(),"total",eventos.contar(c,f,dono));
    }
    private EventoView view(Connection c,Evento e) throws SQLException {
        return EventoView.de(e,artistas.lineup(c,e.id()),tipos.listar(c,e.id()),comunidades.buscar(c,e.id()),clock.instant());
    }
}
