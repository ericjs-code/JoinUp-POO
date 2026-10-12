package br.com.joinup.integration;

import br.com.joinup.bootstrap.ApplicationBootstrap;
import br.com.joinup.config.ConfiguracaoSistema;
import br.com.joinup.http.JsonCodec;
import org.junit.jupiter.api.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

/** Infraestrutura compartilhada apenas pelos novos ITs. Mantém intactas as suítes anteriores. */
abstract class MySql05Fixture {
    static final JsonCodec JSON = new JsonCodec();
    static final AtomicLong NUMERO = new AtomicLong(50000000000L);
    static ConfiguracaoSistema config;
    static ApplicationBootstrap app;
    static HttpClient client;
    static String base;
    final List<Long> participantes=new ArrayList<>(), organizadores=new ArrayList<>(), suportes=new ArrayList<>(), eventos=new ArrayList<>();
    long ana,bruno,orgA,orgB,supA,supB;
    String ca,cb,co,coB,cs,csB;

    @BeforeAll static void iniciar() throws Exception {
        assertEquals("instancia-isolada",System.getenv("JOINUP_IT_DATABASE"));
        config=ConfiguracaoSistema.getInstancia();
        try(var c=admin()) {
            assertTrue(escalar(c,"SELECT VERSION()").startsWith("8.4."));
            assertEquals("joinup_poo",escalar(c,"SELECT DATABASE()"));
            for(String recurso:List.of("schema_v0.2.sql","seed_demo_v0.2.sql")) {
                try(var in=MySql05Fixture.class.getResourceAsStream("/db/"+recurso)) {
                    String sql=new String(Objects.requireNonNull(in).readAllBytes(),StandardCharsets.UTF_8).replaceAll("(?m)^\\s*--.*$","");
                    for(String s:sql.split(";"))if(!s.isBlank())try(var st=c.createStatement()){st.execute(s);}
                }
            }
            var hosts=new ArrayList<String>();
            try(var st=c.createStatement();var r=st.executeQuery("SELECT host FROM mysql.user WHERE user='joinup_it' AND host IN('%','127.0.0.1')")){while(r.next())hosts.add(r.getString(1));}
            assertFalse(hosts.isEmpty());
            for(String host:hosts)try(var st=c.createStatement()) {
                st.execute("REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'joinup_it'@'"+host+"'");
                st.execute("GRANT SELECT,INSERT,UPDATE,DELETE ON joinup_poo.* TO 'joinup_it'@'"+host+"'");
            }
        }
        app=new ApplicationBootstrap(config,new InetSocketAddress("127.0.0.1",0));app.iniciar();
        base="http://127.0.0.1:"+app.getPortaHttp();client=HttpClient.newHttpClient();
    }
    @BeforeEach void contas() throws Exception {
        ana=participante();bruno=participante();orgA=organizador();orgB=organizador();supA=suporte();supB=suporte();
        ca=login("participantes",ana);cb=login("participantes",bruno);co=login("organizadores",orgA);coB=login("organizadores",orgB);cs=login("suporte",supA);csB=login("suporte",supB);
    }
    @AfterEach void limpar() throws Exception {
        for(long id:participantes)limparTickets("id_participante",id);
        for(long id:organizadores)limparTickets("id_organizador",id);
        for(long e:eventos) {
            exec("DELETE FROM comunidade_mensagens WHERE id_comunidade IN(SELECT id_comunidade FROM comunidades WHERE id_evento=?)",e);
            exec("DELETE FROM postagens WHERE id_comunidade IN(SELECT id_comunidade FROM comunidades WHERE id_evento=?)",e);
            exec("DELETE FROM revenda_anuncios WHERE id_ingresso IN(SELECT id_ingresso FROM ingressos WHERE id_evento=?)",e);
            exec("DELETE FROM ingressos WHERE id_evento=?",e);exec("DELETE FROM compras WHERE id_evento=?",e);
            exec("DELETE FROM comunidades WHERE id_evento=?",e);exec("DELETE FROM tipos_ingressos WHERE id_evento=?",e);
            exec("DELETE FROM line_up WHERE id_evento=?",e);exec("DELETE FROM eventos WHERE id_evento=?",e);
        }
        for(long id:participantes){exec("DELETE FROM sessoes WHERE id_participante=?",id);exec("DELETE FROM participantes WHERE id_participante=?",id);}
        for(long id:organizadores){exec("DELETE FROM sessoes WHERE id_organizador=?",id);exec("DELETE FROM organizadores WHERE id_organizador=?",id);}
        for(long id:suportes){exec("DELETE FROM sessoes WHERE id_usuario_suporte=?",id);exec("DELETE FROM usuarios_suporte WHERE id_usuario_suporte=?",id);}
    }
    static void limparTickets(String coluna,long id) throws SQLException {
        exec("DELETE FROM ticket_mensagens WHERE id_ticket IN(SELECT id_ticket FROM ticket WHERE "+coluna+"=?)",id);
        exec("DELETE FROM ticket WHERE "+coluna+"=?",id);
    }
    @AfterAll static void terminar() {if(client!=null)client.close();if(app!=null)app.close();}
    static Connection admin() throws SQLException {return DriverManager.getConnection(config.getUrlJdbc(),System.getenv("JOINUP_IT_ADMIN_USER"),System.getenv("JOINUP_IT_ADMIN_PASSWORD"));}
    static String escalar(Connection c,String sql) throws SQLException {try(var st=c.createStatement();var r=st.executeQuery(sql)){assertTrue(r.next());return r.getString(1);}}
    static long valor(String sql) throws SQLException {try(var c=admin()){return Long.parseLong(escalar(c,sql));}}
    static void exec(String sql,Object...args) throws SQLException {try(var c=admin();var p=c.prepareStatement(sql)){for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);p.executeUpdate();}}
    static long insert(String sql,Object...args) throws SQLException {try(var c=admin();var p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);p.executeUpdate();try(var r=p.getGeneratedKeys()){assertTrue(r.next());return r.getLong(1);}}}
    long participante() throws SQLException {long n=NUMERO.incrementAndGet();long id=insert("INSERT INTO participantes(nome,email,cpf,data_nascimento,senha) SELECT 'Participante 05',?,?,'1990-01-01',senha FROM participantes WHERE id_participante=1","p"+n+"@joinup.test",Long.toString(n));participantes.add(id);return id;}
    long organizador() throws SQLException {long n=NUMERO.incrementAndGet();long id=insert("INSERT INTO organizadores(nome,email,cnpj,senha) SELECT 'Organizador 05',?,?,senha FROM organizadores WHERE id_organizador=1","o"+n+"@joinup.test","000"+n);organizadores.add(id);return id;}
    long suporte() throws SQLException {long n=NUMERO.incrementAndGet();long id=insert("INSERT INTO usuarios_suporte(nome,email,senha) SELECT 'Suporte 05',?,senha FROM usuarios_suporte WHERE id_usuario_suporte=1","s"+n+"@joinup.test");suportes.add(id);return id;}
    String login(String perfil,long id) throws Exception {
        String tabela=perfil.equals("suporte")?"usuarios_suporte":perfil,pk=perfil.equals("participantes")?"id_participante":perfil.equals("organizadores")?"id_organizador":"id_usuario_suporte";
        String email;try(var c=admin()){email=escalar(c,"SELECT email FROM "+tabela+" WHERE "+pk+"="+id);}
        var r=pedir("POST","/api/auth/"+perfil+"/login",Map.of("email",email,"senha","JoinUpDemo#2026"),null);status(200,r);
        String cookie=r.headers().firstValue("Set-Cookie").orElseThrow();assertTrue(cookie.contains("HttpOnly"));return cookie.split(";",2)[0];
    }
    static HttpResponse<String> pedir(String method,String path,Object body,String cookie) throws Exception {return pedir(method,path,body,cookie,"self");}
    static HttpResponse<String> pedir(String method,String path,Object body,String cookie,String origin) throws Exception {
        String b=body==null?"":body instanceof String s?s:new String(JSON.escrever(body),StandardCharsets.UTF_8);
        var q=HttpRequest.newBuilder(URI.create(base+path)).timeout(Duration.ofSeconds(25)).header("Content-Type","application/json").method(method,HttpRequest.BodyPublishers.ofString(b));
        if(cookie!=null)q.header("Cookie",cookie);if(origin!=null)q.header("Origin",origin.equals("self")?base:origin);
        return client.send(q.build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
    static void status(int n,HttpResponse<String> r){assertEquals(n,r.statusCode(),r.body());}
    static Map<?,?> envelope(HttpResponse<String> r){return JSON.ler(r.body().getBytes(StandardCharsets.UTF_8),Map.class);}
    static Map<?,?> dados(HttpResponse<String> r){return (Map<?,?>)envelope(r).get("dados");}
    static long num(Map<?,?> m,String key){return ((Number)m.get(key)).longValue();}
    static String me(long id,String acao){return "/api/me/chamados/"+id+(acao.isEmpty()?"":"/"+acao);}
    static String sup(long id,String acao){return "/api/suporte/chamados/"+id+(acao.isEmpty()?"":"/"+acao);}
    long abrir(String cookie) throws Exception {var r=pedir("POST","/api/chamados",Map.of("titulo","Ajuda 05","descricao","Solicitação privada 05"),cookie);status(201,r);return num(dados(r),"id");}
    long ticket(String estado,String cookie) throws Exception {long id=abrir(cookie);exec("UPDATE ticket SET status_ticket=? WHERE id_ticket=?",estado,id);return id;}
    static String estado(long id) throws SQLException {try(var c=admin()){return escalar(c,"SELECT status_ticket FROM ticket WHERE id_ticket="+id);}}
    static long mensagens(long id) throws SQLException {return valor("SELECT COUNT(*) FROM ticket_mensagens WHERE id_ticket="+id);}
    static void esperarEsperas(int quantidade) throws Exception {
        long fim=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
        while(System.nanoTime()<fim){if(valor("SELECT COUNT(*) FROM performance_schema.data_lock_waits")>=quantidade)return;Thread.sleep(25);}
        fail("Esperava "+quantidade+" requisições bloqueadas no MySQL");
    }
    static void bloquear(Connection c,long id) throws SQLException {c.setAutoCommit(false);try(var p=c.prepareStatement("SELECT id_ticket FROM ticket WHERE id_ticket=? FOR UPDATE")){p.setLong(1,id);try(var r=p.executeQuery()){assertTrue(r.next());}}}
    long evento(String cookie) throws Exception {
        var r=pedir("POST","/api/organizador/eventos",Map.ofEntries(Map.entry("nome","Evento 05"),Map.entry("descricao","Fixture 05"),Map.entry("data",Instant.now().plusSeconds(2592000).toString()),Map.entry("localizacao","Arena"),Map.entry("cidade","Curitiba"),Map.entry("estado","PR"),Map.entry("cep","80000000"),Map.entry("generoId",2),Map.entry("artistaIds",List.of(1)),Map.entry("tiposIngresso",List.of(Map.of("nome","Pista","preco",10,"quantidade",10),Map.of("nome","VIP","preco",20,"quantidade",8)))),cookie);
        status(201,r);long id=num(dados(r),"id");eventos.add(id);return id;
    }
    long comprar(long evento,int qtd,String cookie) throws Exception {
        long tipo=valor("SELECT MIN(id_tipos_ingressos) FROM tipos_ingressos WHERE id_evento="+evento);
        var r=pedir("POST","/api/compras",Map.of("eventoId",evento,"itens",List.of(Map.of("tipoIngressoId",tipo,"quantidade",qtd))),cookie);status(201,r);
        return valor("SELECT MIN(id_ingresso) FROM ingressos WHERE id_compra="+num(dados(r),"compraId"));
    }
}
