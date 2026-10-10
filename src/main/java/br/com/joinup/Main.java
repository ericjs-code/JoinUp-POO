package br.com.joinup;

import br.com.joinup.bootstrap.ApplicationBootstrap;
import br.com.joinup.config.ConfiguracaoSistema;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        try {
            var configuracao = ConfiguracaoSistema.getInstancia();
            try (var aplicacao = new ApplicationBootstrap(configuracao)) {
                Thread shutdown = Thread.ofPlatform().unstarted(aplicacao::close);
                Runtime.getRuntime().addShutdownHook(shutdown);
                try {
                    aplicacao.iniciar();
                    System.out.println("JoinUp-POO disponível em http://" + configuracao.getHostHttp() + ":" + aplicacao.getPortaHttp());
                    aplicacao.aguardarEncerramento();
                } finally {
                    try {
                        Runtime.getRuntime().removeShutdownHook(shutdown);
                    } catch (IllegalStateException ignored) {
                        // A JVM já está encerrando; o hook faz o fechamento idempotente.
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception | ExceptionInInitializerError e) {
            System.err.println("Falha ao iniciar JoinUp-POO. Verifique configuração e disponibilidade da porta.");
            System.exit(1);
        }
    }
}
