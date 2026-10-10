package br.com.joinup.compras.service;

import br.com.joinup.compras.dto.CompraDados;
import br.com.joinup.http.HttpException;
import java.math.*;
import java.time.Instant;
import java.util.*;
import static br.com.joinup.eventos.service.ValidacaoEvento.*;

public final class ValidacaoComercio {
    private ValidacaoComercio() {}
    public static List<CompraDados.Item> itens(CompraDados dados) {
        if(dados==null) throw invalido("compra"); id(dados.eventoId(),"eventoId");
        if(dados.itens()==null || dados.itens().isEmpty()) throw invalido("itens");
        var itens=new TreeMap<Long,CompraDados.Item>();
        for(var i:dados.itens()) {
            if(i==null || i.quantidade()==null || i.quantidade()<=0) throw invalido("quantidade");
            if(itens.put(id(i.tipoIngressoId(),"tipoIngressoId"),i)!=null) throw invalido("tipo repetido");
        }
        return List.copyOf(itens.values());
    }
    public static List<Long> ingressos(List<Long> ids) {
        if(ids==null || ids.isEmpty()) throw invalido("ingressoIds"); var unicos=new TreeSet<Long>();
        for(Long i:ids) if(!unicos.add(id(i,"ingressoIds"))) throw invalido("ingresso repetido");
        return List.copyOf(unicos);
    }
    public static BigDecimal precoRevenda(BigDecimal preco) {
        if(preco==null || preco.signum()<=0 || preco.compareTo(new BigDecimal("999999.99"))>0) throw invalido("precoUnitario");
        try { return preco.setScale(2,RoundingMode.UNNECESSARY); } catch(ArithmeticException e) { throw invalido("precoUnitario"); }
    }
    public static BigDecimal total(BigDecimal total) {
        if(total.signum()<0 || total.compareTo(new BigDecimal("99999999.99"))>0) throw invalido("total fora da precisão do banco");
        return total.setScale(2,RoundingMode.UNNECESSARY);
    }
    public static void antesInicio(Instant data,Instant agora) {
        if(!data.isAfter(agora)) throw new HttpException(409,"EVENTO_ENCERRADO","O evento já começou; compra e publicação de revenda bloqueadas");
    }
    public static HttpException indisponivel() { return new HttpException(409,"REVENDA_INDISPONIVEL","Anúncio ou ingresso indisponível"); }
}
