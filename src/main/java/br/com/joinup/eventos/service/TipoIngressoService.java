package br.com.joinup.eventos.service;

import br.com.joinup.eventos.domain.TipoIngresso;
import br.com.joinup.eventos.dto.TipoIngressoDados;
import br.com.joinup.eventos.repository.TipoIngressoRepositoryJdbc;
import br.com.joinup.http.HttpException;
import java.math.*;
import java.sql.*;
import java.util.*;
import static br.com.joinup.eventos.service.ValidacaoEvento.*;

public final class TipoIngressoService {
    private final TipoIngressoRepositoryJdbc tipos;
    public TipoIngressoService(TipoIngressoRepositoryJdbc tipos) { this.tipos=tipos; }
    public List<Long> cadastrarTipos(Connection c,long evento,List<TipoIngressoDados> dados) throws SQLException {
        if(dados==null || dados.isEmpty()) throw invalido("tiposIngresso");
        var ids=new ArrayList<Long>(); for(var d:dados) ids.add(adicionar(c,evento,d)); return List.copyOf(ids);
    }
    public long adicionar(Connection c,long evento,TipoIngressoDados d) throws SQLException { return tipos.criar(c,evento,validar(d)); }
    public void editar(Connection c,long evento,long id,TipoIngressoDados d) throws SQLException {
        TipoIngresso atual=tipos.bloquear(c,evento,id); if(atual==null) throw naoEncontrado();
        if(atual.temVendas()) throw new HttpException(409,"TIPO_INGRESSO_COM_VENDAS","Categoria com venda histórica não pode ser editada");
        if(d==null || (d.nome()==null && d.preco()==null && d.quantidade()==null)) throw invalido("categoria vazia");
        tipos.atualizar(c,id,validar(new TipoIngressoDados(d.nome()==null?atual.nome():d.nome(),d.preco()==null?atual.preco():d.preco(),d.quantidade()==null?atual.quantidade():d.quantidade())));
    }
    public List<TipoIngresso> listar(Connection c,long evento) throws SQLException { return tipos.listar(c,evento); }
    public static TipoIngressoDados validar(TipoIngressoDados d) {
        if(d==null) throw invalido("categoria");
        String nome=texto(d.nome(),45,"nome da categoria");
        BigDecimal preco=d.preco();
        if(preco==null || preco.signum()<0 || preco.compareTo(new BigDecimal("999999.99"))>0) throw invalido("preco");
        try { preco=preco.setScale(2,RoundingMode.UNNECESSARY); } catch(ArithmeticException e) { throw invalido("preco"); }
        if(d.quantidade()==null || d.quantidade()<0) throw invalido("quantidade");
        return new TipoIngressoDados(nome,preco,d.quantidade());
    }
}
