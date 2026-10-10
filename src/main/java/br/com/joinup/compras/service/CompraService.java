package br.com.joinup.compras.service;

import br.com.joinup.auth.GerenciadorSessao;
import br.com.joinup.compras.dto.CompraDados;
import br.com.joinup.compras.repository.*;
import br.com.joinup.eventos.repository.TipoIngressoRepositoryJdbc;
import br.com.joinup.http.HttpException;
import br.com.joinup.shared.db.ExecutorTransacional;
import br.com.joinup.usuarios.domain.Perfil;
import br.com.joinup.revendas.repository.RevendaRepositoryJdbc;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.*;
import static br.com.joinup.compras.service.ValidacaoComercio.*;
import static br.com.joinup.eventos.service.ValidacaoEvento.naoEncontrado;

public final class CompraService {
    private final ExecutorTransacional transacoes;
    private final GerenciadorSessao sessoes;
    private final CompraRepositoryJdbc compras;
    private final TipoIngressoRepositoryJdbc tipos;
    private final IngressoRepositoryJdbc ingressos;
    private final RevendaRepositoryJdbc revendas;
    private final Clock clock;
    public CompraService(ExecutorTransacional t,GerenciadorSessao s,CompraRepositoryJdbc c,TipoIngressoRepositoryJdbc tipos,IngressoRepositoryJdbc i,RevendaRepositoryJdbc r,Clock clock) {
        transacoes=t;sessoes=s;compras=c;this.tipos=tipos;ingressos=i;revendas=r;this.clock=clock;
    }
    public Map<String,Object> comprar(String token,CompraDados dados) {
        var pedido=itens(dados);
        return transacoes.executar(c -> {
            var pista=compras.evento(c,dados.eventoId(),false); if(pista==null) throw naoEncontrado();
            long comprador=sessoes.exigirParticipanteComContas(c,token,List.of(),List.of(pista.organizadorId())).id();
            var evento=compras.evento(c,dados.eventoId(),true);
            if(evento==null || evento.organizadorId()!=pista.organizadorId()) throw naoEncontrado();
            antesInicio(evento.data(),clock.instant());
            var itens=new ArrayList<Map<String,Object>>(); BigDecimal total=BigDecimal.ZERO;
            for(var i:pedido) {
                var tipo=tipos.bloquear(c,evento.id(),i.tipoIngressoId());
                if(tipo==null) throw new HttpException(422,"REQUISICAO_INVALIDA","Categoria não pertence ao evento");
                if(tipo.preco().signum()<0 || tipo.quantidade()<i.quantidade()) throw new HttpException(409,"ESTOQUE_INSUFICIENTE","Quantidade indisponível");
                BigDecimal subtotal=total(tipo.preco().multiply(BigDecimal.valueOf(i.quantidade()))); total=total(total.add(subtotal));
                itens.add(Map.of("tipoIngressoId",tipo.id(),"quantidade",i.quantidade(),"valorUnitario",tipo.preco(),"subtotal",subtotal));
            }
            // Todos os tipos bloqueados em ordem antes de qualquer baixa.
            for(var i:pedido) compras.baixarEstoque(c,i.tipoIngressoId(),i.quantidade());
            long compra=compras.criar(c,comprador,evento,total); var emitidos=new ArrayList<Long>();
            for(var i:itens) {
                long tipo=(long)i.get("tipoIngressoId"); int quantidade=(int)i.get("quantidade");
                compras.item(c,compra,tipo,quantidade,(BigDecimal)i.get("valorUnitario"),(BigDecimal)i.get("subtotal"));
                for(int n=0;n<quantidade;n++) emitidos.add(compras.emitir(c,compra,comprador,evento.id(),tipo));
            }
            antesInicio(evento.data(),clock.instant());
            return Map.of("compraId",compra,"eventoId",evento.id(),"status","confirmada","total",total,"itens",List.copyOf(itens),"ingressoIds",List.copyOf(emitidos),"pagamento","simulado");
        });
    }
    public Map<String,Object> historico(String token) {
        return transacoes.executar(c -> {
            long dono=sessoes.exigir(c,token,Perfil.PARTICIPANTE).id();
            return Map.of("dados",compras.historico(c,dono),"revendasRecebidas",revendas.recebidas(c,dono));
        });
    }
    public List<Map<String,Object>> ingressos(String token) {
        return transacoes.executar(c -> ingressos.listar(c,sessoes.exigir(c,token,Perfil.PARTICIPANTE).id(),null));
    }
    public Map<String,Object> ingresso(String token,long id) {
        return transacoes.executar(c -> {
            var lista=ingressos.listar(c,sessoes.exigir(c,token,Perfil.PARTICIPANTE).id(),id);
            if(lista.isEmpty()) throw naoEncontrado(); return lista.getFirst();
        });
    }
}
