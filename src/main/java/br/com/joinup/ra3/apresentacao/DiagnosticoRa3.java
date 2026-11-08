package br.com.joinup.ra3.apresentacao;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Map;

public final class DiagnosticoRa3 {
    private DiagnosticoRa3() { }

    public static int executar(Map<String, String> ambiente, PrintStream saida, PrintStream erro) {
        return new AplicacaoRa3().executar(new String[]{"--diagnostico"}, ambiente, Reader.nullReader(),
                new PrintWriter(saida, true), new PrintWriter(erro, true));
    }
}
