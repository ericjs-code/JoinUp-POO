package br.com.joinup.eventos.service;

import br.com.joinup.eventos.domain.ItemCatalogo;
import br.com.joinup.eventos.repository.ArtistaRepositoryJdbc;
import java.sql.*;
import java.util.*;
import static br.com.joinup.eventos.service.ValidacaoEvento.*;

public final class ArtistaService {
    private final ArtistaRepositoryJdbc artistas;
    public ArtistaService(ArtistaRepositoryJdbc artistas) { this.artistas=artistas; }
    public void sincronizarLineup(Connection c,long evento,List<Long> ids) throws SQLException {
        if(ids==null || ids.isEmpty()) throw invalido("artistaIds");
        var unicos=new TreeSet<Long>();
        for(Long id:ids) if(!unicos.add(id(id,"artistaIds"))) throw invalido("artista repetido");
        // Locks de referências em ordem crescente; a tabela não tem posição de palco.
        for(long id:unicos) if(!artistas.existe(c,id)) throw invalido("artista inexistente");
        artistas.substituirLineup(c,evento,List.copyOf(unicos));
    }
    public List<ItemCatalogo> lineup(Connection c,long evento) throws SQLException { return artistas.lineup(c,evento); }
    public List<ItemCatalogo> buscar(Connection c,String busca) throws SQLException {
        if(busca==null || busca.length()>80) throw invalido("busca"); return artistas.buscar(c,busca.trim());
    }
    public List<ItemCatalogo> generos(Connection c) throws SQLException { return artistas.generos(c); }
}
