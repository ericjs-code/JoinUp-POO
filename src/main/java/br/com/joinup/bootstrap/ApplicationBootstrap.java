package br.com.joinup.bootstrap;

import br.com.joinup.config.ConfiguracaoSistema;
import br.com.joinup.http.HealthHandler;
import br.com.joinup.http.HttpResponses;
import br.com.joinup.http.JsonCodec;
import br.com.joinup.http.Router;
import br.com.joinup.http.StaticFileHandler;
import br.com.joinup.shared.db.ConnectionFactory;
import br.com.joinup.shared.db.ExecutorTransacional;
import br.com.joinup.auth.*;
import br.com.joinup.usuarios.repository.*;
import br.com.joinup.usuarios.service.*;
import br.com.joinup.usuarios.http.UsuarioRoutes;
import br.com.joinup.eventos.facade.EventoFacade;
import br.com.joinup.eventos.http.EventoRoutes;
import br.com.joinup.eventos.repository.*;
import br.com.joinup.eventos.service.*;
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
        new br.com.joinup.demo.DemoRunner(connectionFactory).executar(configuracao);
        var json = new JsonCodec();
        var respostas = new HttpResponses(json);
        router = new Router(respostas, new StaticFileHandler(respostas), configuracao.getLimiteRequisicaoBytes());
        router.registrar("GET", "/api/health", new HealthHandler(respostas));
        var participantes = new ParticipanteRepositoryJdbc();
        var organizadores = new OrganizadorRepositoryJdbc();
        var suportes = new SuporteRepositoryJdbc();
        var usuarios = new RepositoriosUsuarios(participantes, organizadores, suportes);
        var transacoes = new ExecutorTransacional(connectionFactory);
        var senhas = new SenhasPbkdf2(configuracao.getIteracoesPbkdf2());
        var sessoes = new GerenciadorSessao(new SessaoRepositoryJdbc(), usuarios, configuracao.getDuracaoSessaoSegundos());
        var contas = new ContaService(transacoes, sessoes, usuarios, new ContaRepositoryJdbc(), senhas);
        var seguranca = new SegurancaHttp(() -> configuracao.getOrigemPublica().isEmpty()
                ? "http://127.0.0.1:" + getPortaHttp() : configuracao.getOrigemPublica());
        new UsuarioRoutes(json, respostas, seguranca, contas).registrar(router,
                new CadastroParticipanteCreator(transacoes, senhas, participantes),
                new CadastroOrganizadorCreator(transacoes, senhas, organizadores),
                new CadastroSuporteCreator(transacoes, senhas, suportes, sessoes),
                new AutenticacaoParticipante(transacoes, senhas, sessoes, participantes),
                new AutenticacaoOrganizador(transacoes, senhas, sessoes, organizadores),
                new AutenticacaoSuporte(transacoes, senhas, sessoes, suportes));
        var eventoFacade = new EventoFacade(transacoes, sessoes,
                new EventoService(new EventoRepositoryJdbc()),
                new ArtistaService(new ArtistaRepositoryJdbc()),
                new TipoIngressoService(new TipoIngressoRepositoryJdbc()),
                new ComunidadeService(new ComunidadeRepositoryJdbc()),
                java.time.Clock.systemUTC());
        new EventoRoutes(json, respostas, seguranca, contas, eventoFacade).registrar(router);
        var compras = new br.com.joinup.compras.repository.CompraRepositoryJdbc();
        var ingressos = new br.com.joinup.compras.repository.IngressoRepositoryJdbc();
        var tipos = new TipoIngressoRepositoryJdbc();
        var revendas = new br.com.joinup.revendas.repository.RevendaRepositoryJdbc();
        var clock = java.time.Clock.systemUTC();
        new br.com.joinup.compras.http.ComercioRoutes(json, respostas, seguranca, contas,
                new br.com.joinup.compras.service.CompraService(transacoes, sessoes, compras, tipos, ingressos, revendas, clock),
                new br.com.joinup.revendas.service.RevendaService(transacoes, sessoes, compras, ingressos, tipos, revendas, clock)).registrar(router);
        var comunidadeSocialRepo = new br.com.joinup.comunidades.repository.ComunidadeSocialRepositoryJdbc();
        var comunidadeSocialService = new br.com.joinup.comunidades.service.ComunidadeSocialService(transacoes, sessoes, comunidadeSocialRepo);
        var arquivos = new br.com.joinup.arquivos.ArquivoService(transacoes,sessoes,comunidadeSocialService,
                new EventoService(new EventoRepositoryJdbc()),new br.com.joinup.arquivos.ArmazenamentoLocal(configuracao.getDiretorioArquivos()),clock);
        comunidadeSocialService.configurarArquivos(arquivos);
        new br.com.joinup.arquivos.ArquivoRoutes(arquivos,contas,seguranca,respostas).registrar(router);
        new br.com.joinup.comunidades.http.ComunidadeRoutes(json, respostas, seguranca, contas, comunidadeSocialService).registrar(router);
        new br.com.joinup.suporte.http.TicketRoutes(json, respostas, seguranca, contas,
                new br.com.joinup.suporte.service.TicketService(transacoes, sessoes,
                        new br.com.joinup.suporte.repository.TicketRepositoryJdbc())).registrar(router);
        new br.com.joinup.paineis.http.PainelRoutes(respostas,
                new br.com.joinup.paineis.service.PainelService(transacoes, sessoes,
                        new br.com.joinup.paineis.repository.PainelRepositoryJdbc(), clock)).registrar(router);
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
