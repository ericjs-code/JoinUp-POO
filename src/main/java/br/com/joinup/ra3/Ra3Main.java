package br.com.joinup.ra3;

import br.com.joinup.ra3.apresentacao.AplicacaoRa3;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;

public final class Ra3Main {
    private Ra3Main() { }

    public static void main(String[] args) {
        try {
            if (new AplicacaoRa3().executar(args, System.getenv(), new InputStreamReader(System.in, StandardCharsets.UTF_8),
                    new PrintWriter(System.out, true), new PrintWriter(System.err, true)) != 0) {
                throw new FalhaExecucao();
            }
        } finally {
            // Processo terminal dedicado: não deixar o daemon do Connector/J no classloader do exec.
            AbandonedConnectionCleanupThread.checkedShutdown();
        }
    }

    /** Mantém exit não zero no Java/Maven sem publicar uma stacktrace no terminal. */
    private static final class FalhaExecucao extends RuntimeException {
        private FalhaExecucao() {
            super("Execução RA3 não concluída; consulte o status sanitizado acima.", null, false, false);
        }
    }
}
