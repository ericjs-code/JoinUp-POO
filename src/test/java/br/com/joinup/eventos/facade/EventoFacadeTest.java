package br.com.joinup.eventos.facade;

import br.com.joinup.eventos.domain.*;
import br.com.joinup.eventos.dto.*;
import br.com.joinup.eventos.repository.*;
import br.com.joinup.eventos.service.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Serviços e fachada reais; doubles apenas nas portas JDBC da coordenação unitária. */
class EventoFacadeTest {
    private final Connection conexao=(Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Connection.class},(p,m,a)->{throw new AssertionError("Conexão própria/operação inesperada");});
    private final List<String> passos=new ArrayList<>();
    private Evento salvo;
    private EventoFacade facade(String falha) {
        var er=new EventoRepositoryJdbc() {
            @Override public boolean generoExiste(Connection c,long id) { assertSame(conexao,c); return id==2; }
            @Override public long criar(Connection c,Evento e) { assertSame(conexao,c); passos.add("evento"); salvo=new Evento(42,e.nome(),e.data(),e.descricao(),e.imagem(),e.localizacao(),e.cidade(),e.estado(),e.cep(),e.organizadorId(),e.generoId(),"Pop"); return 42; }
            @Override public Evento buscar(Connection c,long id,boolean lock) { assertSame(conexao,c); return salvo; }
        };
        var ar=new ArtistaRepositoryJdbc() {
            @Override public boolean existe(Connection c,long id) { assertSame(conexao,c); return true; }
            @Override public void substituirLineup(Connection c,long id,List<Long> ids) throws SQLException { assertSame(conexao,c); assertEquals(42,id); assertEquals(List.of(1L,3L),ids); etapa("lineup",falha); }
            @Override public List<ItemCatalogo> lineup(Connection c,long id) { assertSame(conexao,c); return List.of(new ItemCatalogo(1,"Artista")); }
        };
        var tr=new TipoIngressoRepositoryJdbc() {
            @Override public long criar(Connection c,long id,TipoIngressoDados d) throws SQLException { assertSame(conexao,c); assertEquals(42,id); assertEquals(new BigDecimal("100.25"),d.preco()); etapa("categoria",falha); return 8; }
            @Override public List<TipoIngresso> listar(Connection c,long id) { assertSame(conexao,c); return List.of(new TipoIngresso(8,"Pista",new BigDecimal("100.25"),10,0,false)); }
        };
        var cr=new ComunidadeRepositoryJdbc() {
            @Override public long criar(Connection c,Evento e) throws SQLException { assertSame(conexao,c); assertEquals(42,e.id()); etapa("comunidade",falha); return 9; }
            @Override public Comunidade buscar(Connection c,long id) { assertSame(conexao,c); return new Comunidade(9,"Comunidade",null,id); }
        };
        return new EventoFacade(null,null,new EventoService(er),new ArtistaService(ar),new TipoIngressoService(tr),new ComunidadeService(cr),Clock.fixed(Instant.parse("2026-10-10T20:00:00Z"),ZoneOffset.UTC));
    }
    private void etapa(String nome,String falha) throws SQLException { passos.add(nome); if(nome.equals(falha)) throw new SQLException("Falha proposital"); }
    private CriarEventoDados dados() { return new CriarEventoDados("Evento","Descrição",OffsetDateTime.parse("2027-01-01T20:00:00Z"),null,"Arena","Curitiba","PR","80000000",2L,List.of(3L,1L),List.of(new TipoIngressoDados("Pista",new BigDecimal("100.25"),10))); }
    @Test void coordenaQuatroSubsistemasNaMesmaConexaoEDevolveIdentidades() throws Exception {
        var view=facade("").criarIntegrado(conexao,7,dados()); assertEquals(List.of("evento","lineup","categoria","comunidade"),passos);
        assertEquals(42,view.id()); assertEquals(9,view.comunidade().id()); assertEquals(7,salvo.organizadorId()); assertEquals(8,view.tiposIngresso().getFirst().id());
    }
    @Test void falhaLineupInterrompeCategoriasEComunidade() { assertThrows(SQLException.class,()->facade("lineup").criarIntegrado(conexao,7,dados())); assertEquals(List.of("evento","lineup"),passos); }
    @Test void falhaCategoriaInterrompeComunidade() { assertThrows(SQLException.class,()->facade("categoria").criarIntegrado(conexao,7,dados())); assertEquals(List.of("evento","lineup","categoria"),passos); }
    @Test void falhaComunidadePropagaAoExecutorParaRollback() { assertThrows(SQLException.class,()->facade("comunidade").criarIntegrado(conexao,7,dados())); assertEquals(List.of("evento","lineup","categoria","comunidade"),passos); }
}
