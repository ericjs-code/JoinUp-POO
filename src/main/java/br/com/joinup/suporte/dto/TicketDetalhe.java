package br.com.joinup.suporte.dto;

import br.com.joinup.suporte.domain.*;
import java.util.List;

public record TicketDetalhe(Ticket ticket, List<MensagemTicket> mensagens,
                           int pagina, int tamanho, long totalMensagens) {}
