package br.com.joinup.integration;

import br.com.joinup.bootstrap.ApplicationBootstrap;
import br.com.joinup.config.ConfiguracaoSistema;
import br.com.joinup.http.JsonCodec;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

/** Servidor, sessão, cookie e MySQL 8.4 reais. Somente fixtures próprias. */
class ComunidadeMySqlIT {
    static final JsonCodec JSON = new JsonCodec();
    static final AtomicLong NUMERO = new AtomicLong(40000000000L);
    static final List<String> EVIDENCIA = new ArrayList<>();
    static ConfiguracaoSistema config;
    static ApplicationBootstrap app;
    static HttpClient client;
    static String base;
    final List<Long> participantes = new ArrayList<>(), organizadores = new ArrayList<>(), eventos = new ArrayList<>();
    final List<String> cookies = new ArrayList<>();
    long ana, bruno, organizador, evento, tipo, comunidade, ingresso;
    String ca, cb, co;

    @BeforeAll static void iniciar() throws Exception {
        assertEquals("instancia-isolada", System.getenv("JOINUP_IT_DATABASE"));
        config = ConfiguracaoSistema.getInstancia();
        try (var c = admin()) {
            assertTrue(escalar(c, "SELECT VERSION()").startsWith("8.4."));
            assertEquals("joinup_poo", escalar(c, "SELECT DATABASE()"));
            for (String recurso : List.of("schema_v0.2.sql", "seed_demo_v0.2.sql")) {
                try (var in = ComunidadeMySqlIT.class.getResourceAsStream("/db/" + recurso)) {
                    String sql = new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8).replaceAll("(?m)^\\s*--.*$", "");
                    for (String s : sql.split(";")) if (!s.isBlank()) try (var st = c.createStatement()) { st.execute(s); }
                }
            }
            EVIDENCIA.add("Correção04.1 MySQL=" + escalar(c, "SELECT VERSION()") + "; banco=" + escalar(c, "SELECT DATABASE()"));
        }
        // Restringir a aplicação a DML, inclusive quando esta classe executa isoladamente.
        try (var c = admin(); var st = c.createStatement(); var rs = st.executeQuery("SELECT host FROM mysql.user WHERE user='joinup_it' AND host IN('%','127.0.0.1')")) {
            var hosts = new ArrayList<String>(); while (rs.next()) hosts.add(rs.getString(1)); assertFalse(hosts.isEmpty());
            for (String host : hosts) try (var grants = c.createStatement()) {
                grants.execute("REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'joinup_it'@'" + host + "'");
                grants.execute("GRANT SELECT,INSERT,UPDATE,DELETE ON joinup_poo.* TO 'joinup_it'@'" + host + "'");
            }
        }
        app = new ApplicationBootstrap(config, new InetSocketAddress("127.0.0.1", 0)); app.iniciar();
        base = "http://127.0.0.1:" + app.getPortaHttp(); client = HttpClient.newHttpClient();
    }

    @BeforeEach void fixture() throws Exception {
        ana = participante(); bruno = participante(); organizador = organizador();
        ca = login("participantes", ana); cb = login("participantes", bruno); co = login("organizadores", organizador);
        evento = evento(co); tipo = valor("SELECT MIN(id_tipos_ingressos) FROM tipos_ingressos WHERE id_evento=" + evento);
        comunidade = valor("SELECT id_comunidade FROM comunidades WHERE id_evento=" + evento);
        ingresso = comprar(ca); comprar(cb);
    }
    @AfterEach void limpar() throws Exception {
        for (long e : eventos) {
            exec("DELETE FROM comunidade_mensagens WHERE id_comunidade IN(SELECT id_comunidade FROM comunidades WHERE id_evento=?)", e);
            exec("DELETE FROM postagens WHERE id_comunidade IN(SELECT id_comunidade FROM comunidades WHERE id_evento=?)", e);
            exec("DELETE FROM revenda_anuncios WHERE id_ingresso IN(SELECT id_ingresso FROM ingressos WHERE id_evento=?)", e);
            exec("DELETE FROM ingressos WHERE id_evento=?", e); exec("DELETE FROM compras WHERE id_evento=?", e);
            exec("DELETE FROM comunidades WHERE id_evento=?", e); exec("DELETE FROM tipos_ingressos WHERE id_evento=?", e);
            exec("DELETE FROM line_up WHERE id_evento=?", e); exec("DELETE FROM eventos WHERE id_evento=?", e);
        }
        for (long id : participantes) {
            exec("DELETE FROM sessoes WHERE id_participante=?", id); exec("DELETE FROM participantes WHERE id_participante=?", id);
        }
        for (long id : organizadores) {
            exec("DELETE FROM sessoes WHERE id_organizador=?", id); exec("DELETE FROM organizadores WHERE id_organizador=?", id);
        }
        for (String cookie : cookies) exec("DELETE FROM sessoes WHERE hash_token=?", br.com.joinup.auth.GerenciadorSessao.hashToken(cookie.split("=", 2)[1]));
    }
    @AfterAll static void terminar() throws Exception {
        if (client != null) client.close(); if (app != null) app.close();
        Files.createDirectories(Path.of("target/mysql-validation"));
        Files.write(Path.of("target/mysql-validation/comunidade041.txt"), EVIDENCIA, StandardCharsets.UTF_8);
    }
    static Connection admin() throws SQLException { return DriverManager.getConnection(config.getUrlJdbc(), System.getenv("JOINUP_IT_ADMIN_USER"), System.getenv("JOINUP_IT_ADMIN_PASSWORD")); }
    static String escalar(Connection c, String sql) throws SQLException { try (var s=c.createStatement(); var r=s.executeQuery(sql)) { assertTrue(r.next()); return r.getString(1); } }
    static long valor(String sql) throws SQLException { try (var c=admin()) { return Long.parseLong(escalar(c,sql)); } }
    static void exec(String sql, Object... args) throws SQLException { try(var c=admin();var p=c.prepareStatement(sql)) { for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);p.executeUpdate(); } }
    static long insert(String sql, Object... args) throws SQLException { try(var c=admin();var p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) { for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);p.executeUpdate();try(var r=p.getGeneratedKeys()){assertTrue(r.next());return r.getLong(1);} } }
    long participante() throws SQLException {
        long n=NUMERO.incrementAndGet(); long id=insert("INSERT INTO participantes(nome,email,cpf,data_nascimento,senha) SELECT 'Participante 041',?,?,'1990-01-01',senha FROM participantes WHERE id_participante=1", "p"+n+"@joinup.test",Long.toString(n)); participantes.add(id);return id;
    }
    long organizador() throws SQLException {
        long n=NUMERO.incrementAndGet(); long id=insert("INSERT INTO organizadores(nome,email,cnpj,senha) SELECT 'Organizador 041',?,?,senha FROM organizadores WHERE id_organizador=1", "o"+n+"@joinup.test","000"+n);organizadores.add(id);return id;
    }
    long evento(String cookie) throws Exception {
        var r=pedir("POST","/api/organizador/eventos",Map.ofEntries(Map.entry("nome","Auditoria social 041"),Map.entry("descricao","Fixture própria"),Map.entry("data",Instant.now().plusSeconds(2592000).toString()),Map.entry("localizacao","Arena"),Map.entry("cidade","Curitiba"),Map.entry("estado","PR"),Map.entry("cep","80000000"),Map.entry("generoId",2),Map.entry("artistaIds",List.of(1)),Map.entry("tiposIngresso",List.of(Map.of("nome","Pista","preco",10,"quantidade",10)))),cookie);
        status(201,r);long id=num(dados(r),"id");eventos.add(id);return id;
    }
    String login(String perfil,long id) throws Exception {
        String tabela=perfil.equals("suporte")?"usuarios_suporte":perfil, pk=perfil.equals("participantes")?"id_participante":perfil.equals("organizadores")?"id_organizador":"id_usuario_suporte";
        String email;try(var c=admin()){email=escalar(c,"SELECT email FROM "+tabela+" WHERE "+pk+"="+id);}
        var r=pedir("POST","/api/auth/"+perfil+"/login",Map.of("email",email,"senha","JoinUpDemo#2026"),null);status(200,r);
        String set=r.headers().firstValue("Set-Cookie").orElseThrow();assertTrue(set.contains("HttpOnly"));String cookie=set.split(";",2)[0];cookies.add(cookie);return cookie;
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
    static List<?> lista(HttpResponse<String> r){return (List<?>)envelope(r).get("dados");}
    static long num(Map<?,?> m,String key){return ((Number)m.get(key)).longValue();}
    String com(String acao){return "/api/comunidades/"+comunidade+"/"+acao;}
    String post(long id,String acao){return "/api/postagens/"+id+(acao.isEmpty()?"":"/"+acao);}
    Map<String,Object> pedido(){return Map.of("eventoId",evento,"itens",List.of(Map.of("tipoIngressoId",tipo,"quantidade",1)));}
    long comprar(String cookie) throws Exception {var r=pedir("POST","/api/compras",pedido(),cookie);status(201,r);return ((Number)((List<?>)dados(r).get("ingressoIds")).getFirst()).longValue();}
    void entrar(String cookie) throws Exception {status(200,pedir("POST",com("participacao"),Map.of(),cookie));}
    long publicar(String cookie) throws Exception {var r=pedir("POST",com("postagens"),Map.of("descricao","Texto privado 041"),cookie);status(201,r);return num(dados(r),"id");}
    long mensagem(String cookie) throws Exception {var r=pedir("POST",com("mensagens"),Map.of("mensagem","Conversa privada 041"),cookie);status(201,r);return num(dados(r),"id");}
    long anunciar() throws Exception {var r=pedir("POST","/api/revendas",Map.of("ingressoIds",List.of(ingresso),"precoUnitario",12),ca);status(201,r);return num((Map<?,?>)lista(r).getFirst(),"id");}
    long membro() throws SQLException {return valor("SELECT COALESCE(MAX(ativo),0) FROM participante_comunidades WHERE id_comunidade="+comunidade+" AND id_participante="+ana);}
    long conteudo(String tabela) throws SQLException {return valor("SELECT COUNT(*) FROM "+tabela+" WHERE id_comunidade="+comunidade);}

    @Test void descobertaPublicaNaoVazaConteudoEEntradaSaidaReentrada() throws Exception {
        entrar(ca);long p=publicar(ca);mensagem(ca);
        for(String path:List.of("/api/comunidades","/api/comunidades/"+comunidade)) {
            var r=pedir("GET",path,null,null);status(200,r);
            for(String segredo:List.of("Texto privado","Conversa privada","autorNome","idParticipante","comentarios","postagens","mensagens"))assertFalse(r.body().contains(segredo),r.body());
        }
        for(String path:List.of(com("mensagens"),com("postagens"),post(p,""),post(p,"comentarios"))) {
            status(401,pedir("GET",path,null,null));status(403,pedir("GET",path,null,cb));
        }
        entrar(cb);status(200,pedir("GET",post(p,""),null,cb));
        status(200,pedir("DELETE",com("participacao"),null,cb));status(403,pedir("GET",post(p,""),null,cb));
        entrar(cb);status(200,pedir("GET",post(p,""),null,cb));
        assertEquals(1,valor("SELECT COUNT(*) FROM participante_comunidades WHERE id_participante="+bruno+" AND id_comunidade="+comunidade));
        EVIDENCIA.add("Metadados públicos sem conteúdo; cookie HttpOnly; APIs privadas 401/403; entrada/saída/reentrada manuais HTTP.");
    }
    @ParameterizedTest @ValueSource(strings={"ativo","utilizado","cancelado"})
    void posEventoIngressoElegivelEComercioBloqueado(String estado) throws Exception {
        long anuncio=anunciar();entrar(ca);
        exec("UPDATE eventos SET data=UTC_TIMESTAMP()-INTERVAL 1 DAY WHERE id_evento=?",evento);
        exec("UPDATE ingressos SET status=? WHERE id_ingresso=?",estado,ingresso);
        int permitido=estado.equals("cancelado")?403:200;
        status(permitido,pedir("GET",com("mensagens"),null,ca));status(permitido,pedir("GET",com("postagens"),null,ca));
        status(estado.equals("cancelado")?403:201,pedir("POST",com("mensagens"),Map.of("mensagem","Após evento"),ca));
        status(estado.equals("cancelado")?403:201,pedir("POST",com("postagens"),Map.of("descricao","Após evento"),ca));
        status(200,pedir("DELETE",com("participacao"),null,ca));status(403,pedir("GET",com("mensagens"),null,ca));
        status(permitido,pedir("POST",com("participacao"),Map.of(),ca));
        status(409,pedir("POST","/api/compras",pedido(),ca));status(409,pedir("POST","/api/revendas/"+anuncio+"/compra",Map.of(),cb));
        status(409,pedir("POST","/api/revendas",Map.of("ingressoIds",List.of(ingresso),"precoUnitario",12),ca));
        EVIDENCIA.add("Pós-evento status="+estado+": social="+permitido+", compra/revenda=409, reentrada manual validada.");
    }
    @ParameterizedTest @ValueSource(strings={"visitante","naoMembro","organizadorAlheio","suporte","inativo"})
    void permissoesEmTodasAsOperacoes(String perfil) throws Exception {
        entrar(ca);long p=publicar(ca);String cookie=switch(perfil) {
            case "visitante" -> null;case "naoMembro" -> cb;
            case "organizadorAlheio" -> login("organizadores",organizador());case "suporte" -> login("suporte",1);
            default -> {exec("UPDATE participantes SET ativo=0 WHERE id_participante=?",bruno);yield cb;}
        };
        int n=perfil.equals("visitante")?401:403;
        for(String path:List.of(com("mensagens"),com("postagens"),post(p,""),post(p,"comentarios")))status(n,pedir("GET",path,null,cookie));
        for(String acao:List.of("mensagens","postagens"))status(n,pedir("POST",com(acao),Map.of(acao.equals("mensagens")?"mensagem":"descricao","Teste"),cookie));
        status(n,pedir("POST",post(p,"comentarios"),Map.of("texto","Teste"),cookie));
        status(n,pedir("PUT",post(p,"curtida"),null,cookie));status(n,pedir("DELETE",post(p,"curtida"),null,cookie));
    }
    @Test void organizadorDonoMensagemComAutorCorretoEFeedSomenteLeitura() throws Exception {
        entrar(ca);long p=publicar(ca);long m=mensagem(co);
        status(200,pedir("GET",com("mensagens"),null,co));status(200,pedir("GET",com("postagens"),null,co));
        status(200,pedir("GET",post(p,"comentarios"),null,co));
        status(403,pedir("POST",com("postagens"),Map.of("descricao","X"),co));status(403,pedir("POST",post(p,"comentarios"),Map.of("texto","X"),co));
        status(403,pedir("PUT",post(p,"curtida"),null,co));status(403,pedir("POST",com("participacao"),Map.of(),co));
        assertEquals(1,valor("SELECT COUNT(*) FROM comunidade_mensagens WHERE id_mensagem="+m+" AND autor_tipo='organizador' AND id_organizador="+organizador+" AND id_participante IS NULL"));
        assertEquals(ana,valor("SELECT id_participante FROM postagens WHERE id_postagem="+p));
    }
    @ParameterizedTest @ValueSource(strings={"valida","outraComunidade","inexistente","zero","negativo","overflow","fracao","texto"})
    void respostaSempreNaMesmaComunidade(String caso) throws Exception {
        entrar(ca);long m=mensagem(ca);Object id=m;int esperado=201;
        switch(caso) {
            case "outraComunidade" -> {long e=evento(co),cm=valor("SELECT id_comunidade FROM comunidades WHERE id_evento="+e);id=insert("INSERT INTO comunidade_mensagens(id_comunidade,autor_tipo,id_organizador,mensagem) VALUES(?,'organizador',?,'SEGREDO outra comunidade')",cm,organizador);esperado=404;}
            case "inexistente" -> {id=Integer.MAX_VALUE;esperado=404;}
            case "zero" -> {id=0;esperado=400;}case "negativo" -> {id=-1;esperado=400;}
            case "overflow" -> {id=2147483648L;esperado=400;}case "fracao" -> {id=1.5;esperado=400;}case "texto" -> {id="1";esperado=400;}
        }
        long antes=conteudo("comunidade_mensagens");var r=pedir("POST",com("mensagens"),Map.of("mensagem","Resposta","idRespostaA",id),ca);status(esperado,r);
        assertEquals(antes+(esperado==201?1:0),conteudo("comunidade_mensagens"));assertFalse(r.body().contains("SEGREDO"));
        if(esperado==201){assertEquals(m,num(dados(r),"idRespostaA"));assertEquals(ana,num(dados(r),"participanteId"));}
        EVIDENCIA.add("Resposta "+caso+" -> "+esperado+"; inserções="+(conteudo("comunidade_mensagens")-antes)+".");
    }
    @ParameterizedTest @ValueSource(strings={"https://evil.test/x.png","http://localhost/a","file:///secret","data:image/png;base64,AA","../secret","/uploads/x.png",""})
    void imagemNaoDisponivelRejeitaReferenciasArbitrarias(String imagem) throws Exception {
        entrar(ca);status(400,pedir("POST",com("postagens"),Map.of("descricao","X","imagem",imagem),ca));
        status(400,pedir("POST",com("mensagens"),Map.of("mensagem","X","imagem",imagem),ca));
        status(400,pedir("POST",com("postagens"),Map.of("imagem",imagem),ca));
        assertEquals(0,conteudo("postagens"));assertEquals(0,conteudo("comunidade_mensagens"));
    }
    @Test void bordasTextoSemEdicaoExclusaoEAutoriaComentarios() throws Exception {
        entrar(ca);entrar(cb);
        var r=pedir("POST",com("postagens"),Map.of("descricao","x".repeat(500)),ca);status(201,r);long p=num(dados(r),"id");
        status(400,pedir("POST",com("postagens"),Map.of("descricao","x".repeat(501)),ca));
        status(201,pedir("POST",com("mensagens"),Map.of("mensagem","x".repeat(1000)),ca));status(400,pedir("POST",com("mensagens"),Map.of("mensagem","x".repeat(1001)),ca));
        var c=pedir("POST",post(p,"comentarios"),Map.of("texto","x".repeat(500)),cb);status(201,c);assertEquals(bruno,num(dados(c),"participanteId"));
        status(400,pedir("POST",post(p,"comentarios"),Map.of("texto","x".repeat(501)),cb));
        for(String metodo:List.of("PATCH","PUT","DELETE"))status(405,pedir(metodo,post(p,""),Map.of("descricao","Alteração"),ca));
        assertEquals(500,valor("SELECT CHAR_LENGTH(descricao) FROM postagens WHERE id_postagem="+p));
    }
    @ParameterizedTest @ValueSource(strings={"entra","sai","mensagem","postagem","comentario","curte","descurte"})
    void csrfTodasAsMutacoes(String acao) throws Exception {
        entrar(ca);long p=publicar(ca);String metodo=switch(acao){case "sai","descurte"->"DELETE";case "curte"->"PUT";default->"POST";};
        String path=switch(acao){case "entra","sai"->com("participacao");case "mensagem"->com("mensagens");case "postagem"->com("postagens");case "comentario"->post(p,"comentarios");default->post(p,"curtida");};
        for(String origin:Arrays.asList(null,"https://evil.test","null")){var r=pedir(metodo,path,Map.of("mensagem","X","descricao","X","texto","X"),ca,origin);status(403,r);assertTrue(r.body().contains("CSRF_NEGADO"));}
        assertEquals(1,conteudo("postagens"));assertEquals(0,conteudo("comunidade_mensagens"));assertEquals(1,membro());
    }
    @ParameterizedTest @ValueSource(strings={"null","[]","{}","{","{\"descricao\":3}","{\"descricao\":\"  \"}","{\"descricao\":\"a\",\"id_participante\":1}","{\"descricao\":\"a\",\"descricao\":\"b\"}"})
    void payloadInvalidoNaoInsere(String body) throws Exception {entrar(ca);status(400,pedir("POST",com("postagens"),body,ca));assertEquals(0,conteudo("postagens"));}
    @ParameterizedTest @ValueSource(strings={"0","-1","abc","1.5","2147483648"})
    void idsInvalidos(String id) throws Exception {status(400,pedir("GET","/api/comunidades/"+id,null,null));status(400,pedir("GET","/api/postagens/"+id,null,ca));}
    @Test void inexistentes404EBuscaParametrizada() throws Exception {
        entrar(ca);status(404,pedir("GET","/api/comunidades/2147483647",null,null));
        for(String path:List.of("/api/comunidades/2147483647/mensagens","/api/comunidades/2147483647/postagens","/api/postagens/2147483647","/api/postagens/2147483647/comentarios"))status(404,pedir("GET",path,null,ca));
        status(400,pedir("GET","/api/comunidades?busca="+"x".repeat(101),null,null));
        var r=pedir("GET","/api/comunidades?busca="+URLEncoder.encode("' OR 1=1 --",StandardCharsets.UTF_8),null,null);status(200,r);assertTrue(lista(r).isEmpty());
    }
    @Test void paginasLimitadasSemPerderRespostaDeCriacao() throws Exception {
        entrar(ca);long p=publicar(ca);
        for(int i=0;i<55;i++) {
            insert("INSERT INTO comunidade_mensagens(id_comunidade,autor_tipo,id_participante,mensagem) VALUES(?,'participante',?,'Página')",comunidade,ana);
            insert("INSERT INTO comentarios(id_postagem,id_participante,texto) VALUES(?,?,'Página')",p,ana);
            insert("INSERT INTO postagens(id_comunidade,id_participante,descricao) VALUES(?,?,'Página')",comunidade,ana);
        }
        for(String path:List.of(com("mensagens"),com("postagens"),post(p,"comentarios"))) {
            assertEquals(50,lista(pedir("GET",path,null,ca)).size());assertEquals(path.equals(com("postagens"))?6:5,lista(pedir("GET",path+"?pagina=2&tamanho=50",null,ca)).size());
            assertEquals(2,lista(pedir("GET",path+"?tamanho=2",null,ca)).size());
            for(String query:List.of("pagina=0","pagina=10001","tamanho=101","tamanho=1.5","tamanho=x"))status(400,pedir("GET",path+"?"+query,null,ca));
        }
        mensagem(ca);var r=pedir("POST",post(p,"comentarios"),Map.of("texto","Comentário após página"),ca);status(201,r);assertEquals("Comentário após página",dados(r).get("texto"));
        status(200,pedir("GET",post(p,""),null,ca));
    }
    @Test void curtidasEComentariosConcorrentesContadorConsistente() throws Exception {
        entrar(ca);entrar(cb);long p=publicar(ca);
        try(var pool=Executors.newFixedThreadPool(6)) {
            var calls=new ArrayList<Future<HttpResponse<String>>>();
            for(int i=0;i<12;i++){String ck=i%2==0?ca:cb;calls.add(pool.submit(()->pedir("PUT",post(p,"curtida"),null,ck)));}
            for(var f:calls)status(200,f.get(20,TimeUnit.SECONDS));
            assertEquals(2,valor("SELECT curtidas FROM postagens WHERE id_postagem="+p));
            calls.clear();
            for(int i=0;i<12;i++){String ck=i%2==0?ca:cb;String metodo=i%3==0?"PUT":"DELETE";calls.add(pool.submit(()->pedir(metodo,post(p,"curtida"),null,ck)));}
            for(var f:calls)status(200,f.get(20,TimeUnit.SECONDS));
            assertEquals(valor("SELECT COUNT(*) FROM postagem_curtidas WHERE id_postagem="+p),valor("SELECT curtidas FROM postagens WHERE id_postagem="+p));
            calls.clear();for(int i=0;i<8;i++){String ck=i%2==0?ca:cb;calls.add(pool.submit(()->pedir("POST",post(p,"comentarios"),Map.of("texto","Concorrente"),ck)));}
            for(var f:calls)status(201,f.get(20,TimeUnit.SECONDS));
        }
        for(String ck:List.of(ca,cb))for(int i=0;i<2;i++)status(200,pedir("DELETE",post(p,"curtida"),null,ck));
        assertEquals(0,valor("SELECT curtidas FROM postagens WHERE id_postagem="+p));assertEquals(8,valor("SELECT COUNT(*) FROM comentarios WHERE id_postagem="+p));
        EVIDENCIA.add("12 PUT repetidos concorrentes -> contador 2; 12 PUT/DELETE mistos -> COUNT consistente; 8 comentários concorrentes; DELETE repetido -> 0.");
    }
    @ParameterizedTest @ValueSource(strings={"curte","descurte"})
    void rollbackDepoisDeMutarCurtida(String acao) throws Exception {
        entrar(ca);long p=publicar(ca);if(acao.equals("descurte"))status(200,pedir("PUT",post(p,"curtida"),null,ca));
        exec("CREATE TRIGGER it041_curtida BEFORE UPDATE ON postagens FOR EACH ROW BEGIN IF NEW.id_postagem="+p+" THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='falha fixture'; END IF; END");
        try {
            int antes=acao.equals("curte")?0:1;var r=pedir(acao.equals("curte")?"PUT":"DELETE",post(p,"curtida"),null,ca);status(503,r);assertFalse(r.body().contains("fixture"));
            assertEquals(antes,valor("SELECT curtidas FROM postagens WHERE id_postagem="+p));assertEquals(antes,valor("SELECT COUNT(*) FROM postagem_curtidas WHERE id_postagem="+p));
        } finally {exec("DROP TRIGGER it041_curtida");}
        EVIDENCIA.add("Rollback "+acao+": trigger no contador após INSERT/DELETE da curtida -> 503; linha e contador preservados.");
    }
    @Test void mysqlConstraintsReais() throws Exception {
        var fk=assertThrows(SQLException.class,()->exec("INSERT INTO postagens(id_comunidade,id_participante,descricao) VALUES(2147483647,?,'FK')",ana));assertEquals(1452,fk.getErrorCode());
        var check=assertThrows(SQLException.class,()->exec("INSERT INTO comunidade_mensagens(id_comunidade,autor_tipo,id_participante,id_organizador,mensagem) VALUES(?,'participante',?,?,'CHECK')",comunidade,ana,organizador));assertEquals(3819,check.getErrorCode());
        var ativo=assertThrows(SQLException.class,()->exec("INSERT INTO participante_comunidades(id_comunidade,id_participante,ativo) VALUES(?,?,2)",comunidade,ana));assertEquals(3819,ativo.getErrorCode());
        assertEquals(0,conteudo("postagens"));assertEquals(0,conteudo("comunidade_mensagens"));
        EVIDENCIA.add("FK real erro1452; CHECK autoria e filiação erro3819; constraints intactas.");
    }
    @ParameterizedTest @ValueSource(strings={"postagemSocialPrimeiro","postagemVendaPrimeiro","entradaSocialPrimeiro","entradaVendaPrimeiro"})
    void revendaUltimoIngressoSerializaComSocial(String caso) throws Exception {
        boolean postagem=caso.startsWith("postagem"),socialPrimeiro=caso.endsWith("SocialPrimeiro");
        if(postagem)entrar(ca);long anuncio=anunciar();
        Callable<HttpResponse<String>> social=()->pedir("POST",com(postagem?"postagens":"participacao"),postagem?Map.of("descricao","Antes da venda"):Map.of(),ca);
        Callable<HttpResponse<String>> venda=()->pedir("POST","/api/revendas/"+anuncio+"/compra",Map.of(),cb);
        try(var c=admin();var pool=Executors.newFixedThreadPool(2)) {
            c.setAutoCommit(false);bloquearEvento(c);
            var primeiro=pool.submit(socialPrimeiro?social:venda);esperarEsperas(1);
            var segundo=pool.submit(socialPrimeiro?venda:social);esperarEsperas(2);c.commit();
            var r1=primeiro.get(20,TimeUnit.SECONDS);var r2=segundo.get(20,TimeUnit.SECONDS);
            status(socialPrimeiro?(postagem?201:200):200,r1);status(socialPrimeiro?200:403,r2);
            EVIDENCIA.add(caso+": dois locks reais observados -> HTTP "+r1.statusCode()+"/"+r2.statusCode()+".");
        }
        assertEquals(bruno,valor("SELECT id_participante FROM ingressos WHERE id_ingresso="+ingresso));
        assertEquals(1,valor("SELECT COUNT(*) FROM revenda_anuncios WHERE id_revenda_anuncios="+anuncio+" AND status='vendido' AND id_comprador="+bruno));
        assertEquals(0,membro());assertEquals(postagem&&socialPrimeiro?1:0,conteudo("postagens"));
        assertEquals(0,valor("SELECT COUNT(*) FROM participante_comunidades WHERE id_comunidade="+comunidade+" AND id_participante="+bruno));
        status(403,pedir("GET",com("postagens"),null,ca));entrar(cb);status(200,pedir("GET",com("postagens"),null,cb));
        assertEquals(8,valor("SELECT quantidade_disponivel FROM tipos_ingressos WHERE id_tipos_ingressos="+tipo));
    }
    void bloquearEvento(Connection c) throws SQLException {try(var p=c.prepareStatement("SELECT id_evento FROM eventos WHERE id_evento=? FOR UPDATE")){p.setLong(1,evento);try(var r=p.executeQuery()){assertTrue(r.next());}}}
    static void esperarEsperas(int quantidade) throws Exception {
        long fim=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
        while(System.nanoTime()<fim){if(valor("SELECT COUNT(*) FROM performance_schema.data_lock_waits")>=quantidade)return;Thread.sleep(25);}
        fail("Esperava "+quantidade+" requisições bloqueadas no MySQL real");
    }
    @Test void historicoPreservadoDepoisDaPerdaDoUltimoIngresso() throws Exception {
        entrar(ca);long p=publicar(ca),m=mensagem(ca);long anuncio=anunciar();status(200,pedir("POST","/api/revendas/"+anuncio+"/compra",Map.of(),cb));
        status(403,pedir("GET",post(p,""),null,ca));status(403,pedir("POST",com("participacao"),Map.of(),ca));
        assertEquals(ana,valor("SELECT id_participante FROM postagens WHERE id_postagem="+p));assertEquals(ana,valor("SELECT id_participante FROM comunidade_mensagens WHERE id_mensagem="+m));
        entrar(cb);status(200,pedir("GET",post(p,""),null,cb));assertTrue(pedir("GET",com("mensagens"),null,cb).body().contains("Conversa privada 041"));
    }
    @ParameterizedTest @ValueSource(strings={"mensagem","comentario","curte","descurte"})
    void vendaPrimeiroRevalidaTodasAsMutacoes(String acao) throws Exception {
        entrar(ca);long p=publicar(ca),anuncio=anunciar();
        if(acao.equals("descurte"))status(200,pedir("PUT",post(p,"curtida"),null,ca));
        String metodo=acao.equals("curte")?"PUT":acao.equals("descurte")?"DELETE":"POST";
        String path=acao.equals("mensagem")?com("mensagens"):post(p,acao.equals("comentario")?"comentarios":"curtida");
        Object body=acao.equals("mensagem")?Map.of("mensagem","Tarde"):acao.equals("comentario")?Map.of("texto","Tarde"):null;
        try(var c=admin();var pool=Executors.newFixedThreadPool(2)) {
            c.setAutoCommit(false);bloquearEvento(c);
            var venda=pool.submit(()->pedir("POST","/api/revendas/"+anuncio+"/compra",Map.of(),cb));esperarEsperas(1);
            var social=pool.submit(()->pedir(metodo,path,body,ca));esperarEsperas(2);c.commit();
            status(200,venda.get(20,TimeUnit.SECONDS));status(403,social.get(20,TimeUnit.SECONDS));
        }
        assertEquals(0,conteudo("comunidade_mensagens"));assertEquals(0,valor("SELECT COUNT(*) FROM comentarios WHERE id_postagem="+p));
        assertEquals(acao.equals("descurte")?1:0,valor("SELECT curtidas FROM postagens WHERE id_postagem="+p));assertEquals(0,membro());
        EVIDENCIA.add("Venda antes de "+acao+": locks reais -> 200/403; conteúdo/curtida históricos preservados.");
    }
    @ParameterizedTest @ValueSource(strings={"postagens","comunidade_mensagens","comentarios"})
    void falhaDeInsercaoHttpSemEfeitosParciais(String tabela) throws Exception {
        entrar(ca);long p=publicar(ca);String cond=tabela.equals("comentarios")?"NEW.id_postagem="+p:"NEW.id_comunidade="+comunidade;
        exec("CREATE TRIGGER it041_insercao BEFORE INSERT ON "+tabela+" FOR EACH ROW BEGIN IF "+cond+" THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='falha fixture'; END IF; END");
        try {
            String path=tabela.equals("comentarios")?post(p,"comentarios"):com(tabela.equals("postagens")?"postagens":"mensagens");
            String campo=tabela.equals("comentarios")?"texto":tabela.equals("postagens")?"descricao":"mensagem";
            var r=pedir("POST",path,Map.of(campo,"Falha controlada"),ca);status(503,r);assertFalse(r.body().contains("fixture"));
            assertEquals(1,conteudo("postagens"));assertEquals(0,conteudo("comunidade_mensagens"));assertEquals(0,valor("SELECT COUNT(*) FROM comentarios WHERE id_postagem="+p));assertEquals(1,membro());
        } finally {exec("DROP TRIGGER it041_insercao");}
    }
    @Test void contaDesativadaDuranteEsperaRevalidaAntesDaPublicacao() throws Exception {
        entrar(ca);
        try(var c=admin();var pool=Executors.newSingleThreadExecutor()) {
            c.setAutoCommit(false);
            try(var st=c.createStatement();var rs=st.executeQuery("SELECT id_participante FROM participantes WHERE id_participante="+ana+" FOR UPDATE")){assertTrue(rs.next());}
            var publicar=pool.submit(()->pedir("POST",com("postagens"),Map.of("descricao","Conta inativa"),ca));esperarEsperas(1);
            try(var p=c.prepareStatement("UPDATE participantes SET ativo=0 WHERE id_participante=?")){p.setLong(1,ana);p.executeUpdate();}c.commit();
            status(403,publicar.get(20,TimeUnit.SECONDS));
        }
        assertEquals(0,conteudo("postagens"));
    }
}
