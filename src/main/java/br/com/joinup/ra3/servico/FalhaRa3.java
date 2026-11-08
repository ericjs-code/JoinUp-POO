package br.com.joinup.ra3.servico;

/** Somente mensagens próprias; nunca encadeia exceções SQL com URL/credenciais. */
public final class FalhaRa3 extends RuntimeException {
    public enum Motivo { NAO_ENCONTRADO, DUPLICIDADE, VINCULO, CONCORRENCIA, BANCO }
    private final Motivo motivo;
    public FalhaRa3(Motivo motivo, String mensagem) { super(mensagem); this.motivo = motivo; }
    public Motivo motivo() { return motivo; }
}
