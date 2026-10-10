package br.com.joinup.http;

/** Erro de transporte com mensagem pública, nunca mensagem original de SQL/parser. */
public final class HttpException extends RuntimeException {
    private final int status;
    private final String codigo;

    public HttpException(int status, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }

    public int status() { return status; }
    public String codigo() { return codigo; }
}
