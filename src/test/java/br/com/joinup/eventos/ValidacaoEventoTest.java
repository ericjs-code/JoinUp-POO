package br.com.joinup.eventos;

import br.com.joinup.eventos.dto.*;
import br.com.joinup.eventos.service.*;
import br.com.joinup.http.HttpException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.time.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ValidacaoEventoTest {
    private final Instant agora=Instant.parse("2026-10-10T20:00:00Z");
    @ParameterizedTest @ValueSource(strings={"2026-10-10T19:59:59Z","2026-10-10T20:00:00Z","2026-10-10T17:00:00-03:00","2026-10-10T20:00:00.9Z"})
    void destinoAtualPassadoOuTruncadoNaoPodeSerPersistido(String data) {
        assertEquals("DATA_EVENTO_INVALIDA",assertThrows(HttpException.class,()->ValidacaoEvento.dataFutura(OffsetDateTime.parse(data),agora)).codigo());
    }
    @Test void offsetNormalizadoESemDependenciaDaZonaDaJvm() {
        assertEquals(Instant.parse("2026-10-10T21:00:00Z"),ValidacaoEvento.dataFutura(OffsetDateTime.parse("2026-10-10T18:00:00-03:00"),agora));
        ValidacaoEvento.antesInicio(agora.plusSeconds(1),agora);
        assertEquals(409,assertThrows(HttpException.class,()->ValidacaoEvento.antesInicio(agora,agora)).status());
    }
    @ParameterizedTest @ValueSource(strings={"-0.01","1000000.00","1.001"})
    void precoNaoPodeSerNegativoArredondadoOuExcederDecimal(String preco) {
        assertThrows(HttpException.class,()->TipoIngressoService.validar(new TipoIngressoDados("Pista",new BigDecimal(preco),10)));
    }
    @Test void estoqueZeroPrecoZeroELimitesValidos() {
        var t=TipoIngressoService.validar(new TipoIngressoDados("Pista",BigDecimal.ZERO,0));
        assertEquals(new BigDecimal("0.00"),t.preco());
        assertEquals(new BigDecimal("999999.99"),TipoIngressoService.validar(new TipoIngressoDados("Pista",new BigDecimal("999999.99"),Integer.MAX_VALUE)).preco());
        assertThrows(HttpException.class,()->TipoIngressoService.validar(new TipoIngressoDados("Pista",BigDecimal.ONE,-1)));
    }
    @ParameterizedTest @ValueSource(strings={"https://evil.test/x.png","file:///etc/passwd","/assets/../x.svg","//evil.test/x.png","data:image/svg+xml,x","C:\\imagem.png"})
    void imagensForaDoClasspathPermitidoSaoRecusadas(String img) { assertThrows(HttpException.class,()->ValidacaoEvento.imagem(img)); }
    @Test void limitesDeTextoCepUfEIds() {
        assertEquals("a".repeat(45),ValidacaoEvento.texto("a".repeat(45),45,"nome"));
        assertThrows(HttpException.class,()->ValidacaoEvento.texto("a".repeat(46),45,"nome"));
        assertThrows(HttpException.class,()->ValidacaoEvento.cep("80000-000"));
        assertThrows(HttpException.class,()->ValidacaoEvento.uf("XX"));
        assertThrows(HttpException.class,()->ValidacaoEvento.id(0L,"id"));
        assertNull(ValidacaoEvento.imagem(""));
    }
    @ParameterizedTest @ValueSource(strings={"pagina=0","tamanho=101","pagina=100001","generoId=-1","desconhecido=1"})
    void filtrosInvalidosNaoChegamAoBanco(String q) {
        String[] v=q.split("="); assertThrows(HttpException.class,()->FiltroEventos.de(Map.of(v[0],v[1])));
    }
}
