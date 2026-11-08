package br.com.joinup.ra3.apresentacao;

import br.com.joinup.ra3.persistencia.*;
import br.com.joinup.ra3.servico.ServicoCrudRa3;
import jakarta.persistence.EntityManagerFactory;
import java.io.*;
import java.util.Map;
import java.util.function.Function;

/** Composição e ciclo de vida: uma fábrica por execução, inclusive no modo diagnóstico. */
public final class AplicacaoRa3 {
    private final Function<ConfiguracaoRa3, EntityManagerFactory> conectar;
    public AplicacaoRa3() { this(PersistenciaRa3::abrir); }
    public AplicacaoRa3(Function<ConfiguracaoRa3, EntityManagerFactory> conectar) { this.conectar = conectar; }

    public int executar(String[] args, Map<String, String> ambiente, Reader entrada, PrintWriter saida, PrintWriter erro) {
        boolean diagnostico = args.length == 1 && "--diagnostico".equals(args[0]);
        if (args.length > 0 && !diagnostico) { erro.println("Argumentos inválidos. Use --diagnostico ou inicie sem argumentos para abrir o menu."); erro.flush(); return 1; }
        ConfiguracaoRa3 configuracao;
        try { configuracao = ConfiguracaoRa3.doAmbiente(ambiente); }
        catch (IllegalArgumentException e) { erro.println("RA3_CONFIGURACAO_INVALIDA: " + e.getMessage()); erro.flush(); return 1; }
        try (var fabrica = conectar.apply(configuracao)) {
            if (diagnostico) {
                saida.println("RA3_CONEXAO_OK: EclipseLink conectado ao catálogo joinup_poo_ra3; DDL automático desabilitado.");
                saida.println("Diagnóstico encerrado sem escrita e sem aguardar entrada.");
            } else {
                new MenuRa3(entrada, saida, new ServicoCrudRa3(fabrica)).executar();
            }
            saida.flush(); return 0;
        } catch (IOException e) {
            erro.println("Não foi possível ler a entrada do terminal."); erro.flush(); return 1;
        } catch (RuntimeException e) {
            erro.println("RA3_CONEXAO_FALHOU: confira MySQL, credenciais, permissões e schema manual."); erro.flush(); return 1;
        }
    }
}
