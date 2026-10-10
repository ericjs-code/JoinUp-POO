package br.com.joinup.compras;

import br.com.joinup.compras.dto.CompraDados;
import br.com.joinup.compras.service.ValidacaoComercio;
import br.com.joinup.http.HttpException;
import br.com.joinup.revendas.dto.FiltroRevendas;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ValidacaoComercioTest {
    @ParameterizedTest @ValueSource(strings={"0","-0.01","1000000","0.001","12.345"})
    void precoNaoArredondaNemUltrapassaBanco(String preco) { assertEquals(422,assertThrows(HttpException.class,()->ValidacaoComercio.precoRevenda(new BigDecimal(preco))).status()); }
    @ParameterizedTest @ValueSource(strings={"0.01","12.10","999999.99","120.000"})
    void precoValidoExato(String preco) { assertEquals(new BigDecimal(preco).setScale(2),ValidacaoComercio.precoRevenda(new BigDecimal(preco))); }
    @Test void totalExatoEOverflowDecimal() {
        assertEquals(new BigDecimal("99999999.99"),ValidacaoComercio.total(new BigDecimal("99999999.99")));
        assertEquals(422,assertThrows(HttpException.class,()->ValidacaoComercio.total(new BigDecimal("100000000"))).status());
    }
    @Test void instanteInicioInclusiveBloqueia() {
        Instant agora=Instant.parse("2026-10-10T20:00:00Z");
        for(Instant inicio:List.of(agora,agora.minusNanos(1))) assertEquals(409,assertThrows(HttpException.class,()->ValidacaoComercio.antesInicio(inicio,agora)).status());
        assertDoesNotThrow(()->ValidacaoComercio.antesInicio(agora.plusNanos(1),agora));
    }
    @Test void tiposRepetidosRejeitadosEOrdenadosParaLock() {
        var item=new CompraDados.Item(2L,1);
        assertThrows(HttpException.class,()->ValidacaoComercio.itens(new CompraDados(1L,List.of(item,item))));
        assertEquals(List.of(1L,2L),ValidacaoComercio.itens(new CompraDados(1L,List.of(item,new CompraDados.Item(1L,2)))).stream().map(CompraDados.Item::tipoIngressoId).toList());
    }
    @Test void ingressosRepetidosNulosEOrdem() {
        assertThrows(HttpException.class,()->ValidacaoComercio.ingressos(List.of(1L,1L)));
        assertThrows(HttpException.class,()->ValidacaoComercio.ingressos(Arrays.asList(1L,null)));
        assertThrows(HttpException.class,()->ValidacaoComercio.ingressos(List.of()));
        assertEquals(List.of(1L,9L),ValidacaoComercio.ingressos(List.of(9L,1L)));
    }
    @ParameterizedTest @ValueSource(ints={0,-1})
    void quantidadePositiva(int n) { assertThrows(HttpException.class,()->ValidacaoComercio.itens(new CompraDados(1L,List.of(new CompraDados.Item(1L,n))))); }
    @Test void paginacaoNaoEstouraIntNemAceitaExtras() {
        assertThrows(HttpException.class,()->new FiltroRevendas(null,Integer.MAX_VALUE,100));
        assertThrows(HttpException.class,()->FiltroRevendas.de(Map.of("titular","1")));
        assertEquals(new FiltroRevendas(1L,2,6),FiltroRevendas.de(Map.of("eventoId","1","pagina","2","tamanho","6")));
    }
}
