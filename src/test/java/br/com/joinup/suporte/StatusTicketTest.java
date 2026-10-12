package br.com.joinup.suporte;

import br.com.joinup.http.HttpException;
import br.com.joinup.suporte.domain.StatusTicket;
import br.com.joinup.suporte.service.TicketService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class StatusTicketTest {
    @ParameterizedTest @CsvSource({"aberto,aguardando_usuario", "em_atendimento,aguardando_usuario", "aguardando_usuario,aguardando_usuario"})
    void suporteResponde(StatusTicket origem, StatusTicket destino) { assertEquals(destino,origem.responder()); }
    @ParameterizedTest @ValueSource(strings={"aberto","em_atendimento","fechado"})
    void solicitanteNaoPodeResolverRetornar(StatusTicket origem) {
        assertEquals(409,assertThrows(HttpException.class,origem::resolver).status());
        assertEquals(409,assertThrows(HttpException.class,origem::retornar).status());
    }
    @ParameterizedTest @ValueSource(strings={"aberto","em_atendimento"})
    void encerramentoPermitido(StatusTicket origem) { assertEquals(StatusTicket.fechado,origem.encerrar()); }
    @ParameterizedTest @ValueSource(strings={"aguardando_usuario","fechado"})
    void encerramentoBloqueado(StatusTicket origem) { assertThrows(HttpException.class,origem::encerrar); }
    @ParameterizedTest @ValueSource(strings={"", "  ", "\u0000"})
    void mensagemVaziaOuControleInvalida(String s) { assertThrows(HttpException.class,()->TicketService.texto(s,500)); }
}
