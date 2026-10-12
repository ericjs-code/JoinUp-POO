package br.com.joinup.suporte.dto;

import br.com.joinup.suporte.domain.Ticket;
import java.util.List;

public record PaginaTickets(List<Ticket> dados, int pagina, int tamanho, long total) {}
