package br.com.joinup.bootstrap;

import br.com.joinup.config.ConfiguracaoSistema;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class ApplicationBootstrapTest {
    @Test void composicaoRealEFechamentoLiberamPorta() throws Exception {
        int porta;
        var aplicacao = new ApplicationBootstrap(ConfiguracaoSistema.getInstancia(), new InetSocketAddress("127.0.0.1", 0));
        try (aplicacao; var cliente = HttpClient.newHttpClient()) {
            aplicacao.iniciar();
            porta = aplicacao.getPortaHttp();
            assertNotNull(aplicacao.getConnectionFactory());
            for (String caminho : new String[]{"/api/health", "/", "/css/inicio.css", "/js/inicio.js"}) {
                var resposta = cliente.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + porta + caminho))
                        .timeout(Duration.ofSeconds(5)).GET().build(), HttpResponse.BodyHandlers.ofString());
                assertEquals(200, resposta.statusCode());
            }
        }
        aplicacao.close();
        assertTimeoutPreemptively(Duration.ofSeconds(1), aplicacao::aguardarEncerramento);
        assertThrows(IllegalStateException.class, aplicacao::iniciar);
        try (var socket = new ServerSocket()) {
            socket.bind(new InetSocketAddress("127.0.0.1", porta));
            assertEquals(porta, socket.getLocalPort());
        }
    }

    @Test void fechamentoAntesDeIniciarTambemLiberaPorta() throws Exception {
        int porta;
        try (var reserva = new ServerSocket(0)) { porta = reserva.getLocalPort(); }
        try (var aplicacao = new ApplicationBootstrap(ConfiguracaoSistema.getInstancia(), new InetSocketAddress("127.0.0.1", porta))) {
            assertThrows(IllegalStateException.class, aplicacao::getPortaHttp);
        }
        try (var socket = new ServerSocket()) {
            socket.bind(new InetSocketAddress("127.0.0.1", porta));
        }
    }
}
