package br.com.joinup.suporte.domain;

import br.com.joinup.http.HttpException;

/** Matriz RF-SUP-04. Valores iguais ao ENUM já aprovado no banco. */
public enum StatusTicket {
    aberto, em_atendimento, aguardando_usuario, fechado;

    public StatusTicket responder() { exigirNaoFechado(); return aguardando_usuario; }
    public StatusTicket retornar() { exigirAguardando(); return em_atendimento; }
    public StatusTicket resolver() { exigirAguardando(); return fechado; }
    public StatusTicket encerrar() {
        exigirNaoFechado();
        if (this == aguardando_usuario)
            throw new HttpException(409, "TICKET_AGUARDANDO_USUARIO", "Aguarde o solicitante resolver ou retornar");
        return fechado;
    }
    private void exigirNaoFechado() {
        if (this == fechado) throw new HttpException(409, "TICKET_FECHADO", "Chamado fechado; abra outro para um novo problema");
    }
    private void exigirAguardando() {
        exigirNaoFechado();
        if (this != aguardando_usuario)
            throw new HttpException(409, "TICKET_ESTADO_INVALIDO", "Resolver e Retornar exigem aguardando usuário");
    }
}
