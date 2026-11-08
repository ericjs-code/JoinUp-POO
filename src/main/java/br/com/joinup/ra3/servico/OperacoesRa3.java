package br.com.joinup.ra3.servico;

import java.util.List;

/** Contrato usado pelo terminal e seus testes com streams, implementado pelo serviço JPA. */
public interface OperacoesRa3 {
    int LIMITE = 20;
    record Registro(int id, String resumo) { }
    Registro cadastrar(DadosRa3.Cadastro cadastro);
    Registro consultar(TipoRegistro tipo, int id);
    List<Registro> listar(TipoRegistro tipo, int pagina);
    Registro atualizar(DadosRa3.Alteracao alteracao);
    boolean excluir(TipoRegistro tipo, int id, String confirmacao);
}
