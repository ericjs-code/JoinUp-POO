package br.com.joinup.ra3;

import br.com.joinup.ra3.apresentacao.DiagnosticoRa3;
import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;

public final class Ra3Main {
    private Ra3Main() { }

    public static void main(String[] args) {
        try {
            if (DiagnosticoRa3.executar(System.getenv(), System.out, System.err) != 0) {
                throw new IllegalStateException("Diagnóstico RA3 não concluído; consulte o status sanitizado acima.");
            }
        } finally {
            // Processo terminal dedicado: não deixar o daemon do Connector/J no classloader do exec.
            AbandonedConnectionCleanupThread.checkedShutdown();
        }
    }
}
