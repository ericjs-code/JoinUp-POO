package br.com.joinup.bootstrap;

import br.com.joinup.config.ConfiguracaoSistema;
import br.com.joinup.http.HealthHandler;
import br.com.joinup.http.HttpResponses;
import br.com.joinup.http.JsonCodec;
import br.com.joinup.http.Router;
import br.com.joinup.http.StaticFileHandler;
import br.com.joinup.shared.db.ConnectionFactory;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Composição manual e propriedade dos recursos do processo. */
public final class ApplicationBootstrap implements AutoCloseable {
    private HttpServer servidor;
    private final InetSocketAddress endereco;
    private final Router router;
    private final ThreadPoolExecutor executor;
    private final ConnectionFactory connectionFactory;
    private final CountDownLatch encerramento = new CountDownLatch(1);
    private boolean iniciado;
    private boolean fechado;

    public ApplicationBootstrap(ConfiguracaoSistema configuracao) throws IOException {
        this(configuracao, new InetSocketAddress(configuracao.getHostHttp(), configuracao.getPortaHttp()));
    }

    /** Permite porta efêmera nos testes, mantendo os demais componentes reais. */
    public ApplicationBootstrap(ConfiguracaoSistema configuracao, InetSocketAddress endereco) throws IOException {
        this.endereco = endereco;
        connectionFactory = new ConnectionFactory(configuracao);
        var respostas = new HttpResponses(new JsonCodec());
        router = new Router(respostas, new StaticFileHandler(respostas), configuracao.getLimiteRequisicaoBytes());
        router.registrar("GET", "/api/health", new HealthHandler(respostas));
        router.iniciar();
        int threads = configuracao.getThreadsHttp();
        executor = new ThreadPoolExecutor(threads, threads, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(128), Thread.ofPlatform().name("joinup-http-", 0).factory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    public synchronized void iniciar() throws IOException {
        if (fechado || iniciado) throw new IllegalStateException("Ciclo de vida inválido");
        // HttpServer.stop antes de start não libera o bind no JDK usado: alocar somente ao iniciar.
        servidor = HttpServer.create(endereco, 128);
        servidor.setExecutor(executor);
        servidor.createContext("/", router);
        servidor.start();
        iniciado = true;
    }

    public synchronized int getPortaHttp() {
        if (!iniciado) throw new IllegalStateException("Servidor não iniciado");
        return servidor.getAddress().getPort();
    }
    public ConnectionFactory getConnectionFactory() { return connectionFactory; }
    public void aguardarEncerramento() throws InterruptedException { encerramento.await(); }

    @Override public synchronized void close() {
        if (fechado) return;
        fechado = true;
        try {
            if (servidor != null) servidor.stop(iniciado ? 1 : 0);
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) executor.shutdownNow();
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            encerramento.countDown();
        }
    }
}
