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
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

/** HTTP + JDBC real; todas as alterações usam fixtures próprias e são limpas. */
class ComercioMySqlIT {
    static final JsonCodec JSON=new JsonCodec();
    static final AtomicLong NUMERO=new AtomicLong(30000000000L);
    static final List<String> EVIDENCIA=new ArrayList<>();
    static ConfiguracaoSistema config;
    static ApplicationBootstrap app;
    static HttpClient client;
    static String base;
    static final Map<String,Long> LIMITES=new LinkedHashMap<>();
    static final Map<String,String> PK=Map.ofEntries(Map.entry("participantes","id_participante"),Map.entry("organizadores","id_organizador"),Map.entry("eventos","id_evento"),Map.entry("tipos_ingressos","id_tipos_ingressos"),Map.entry("comunidades","id_comunidade"),Map.entry("compras","id_compra"),Map.entry("ingressos","id_ingresso"),Map.entry("revenda_anuncios","id_revenda_anuncios"),Map.entry("sessoes","id_sessao"),Map.entry("auditoria_reativacoes","id_auditoria_reativacao"));
    long evento,tipo,vendedor,comprador,terceiro,organizador;
    String cv,cc,ct,co;
    @BeforeAll static void iniciar() throws Exception {
        assertEquals("instancia-isolada",System.getenv("JOINUP_IT_DATABASE"));
        config=ConfiguracaoSistema.getInstancia();
        try(var c=admin()) {
            assertTrue(escalar(c,"SELECT VERSION()").startsWith("8.4."),"Entrega03 exige MySQL 8.4 real");
            assertEquals("joinup_poo",escalar(c,"SELECT DATABASE()"));
            for(String recurso:List.of("schema_v0.2.sql","seed_demo_v0.2.sql")) {
                try(var in=ComercioMySqlIT.class.getResourceAsStream("/db/"+recurso)) {
                    String sql=new String(Objects.requireNonNull(in).readAllBytes(),StandardCharsets.UTF_8).replaceAll("(?m)^\\s*--.*$","");
                    for(String s:sql.split(";")) if(!s.isBlank()) try(var st=c.createStatement()) { st.execute(s); }
                }
            }
            for(var p:PK.entrySet()) LIMITES.put(p.getKey(),Long.parseLong(escalar(c,"SELECT COALESCE(MAX("+p.getValue()+"),0) FROM "+p.getKey())));
            try(var st=c.createStatement();var rs=st.executeQuery("SELECT host FROM mysql.user WHERE user='joinup_it' AND host IN('%','127.0.0.1')")) {
                var hosts=new ArrayList<String>();while(rs.next())hosts.add(rs.getString(1));assertFalse(hosts.isEmpty());
                for(String host:hosts)try(var grants=c.createStatement()) {
                    grants.execute("REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'joinup_it'@'"+host+"'");
                    grants.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON joinup_poo.* TO 'joinup_it'@'"+host+"'");
                }
            }
            EVIDENCIA.add("Entrega03 MySQL="+escalar(c,"SELECT VERSION()")+", banco=joinup_poo, sessão="+escalar(c,"SELECT @@session.time_zone"));
        }
        app=new ApplicationBootstrap(config,new InetSocketAddress("127.0.0.1",0)); app.iniciar(); base="http://127.0.0.1:"+app.getPortaHttp(); client=HttpClient.newHttpClient();
    }
    @BeforeEach void fixture() throws Exception {
        vendedor=participante(); comprador=participante(); terceiro=participante();
        cv=login("participantes",vendedor); cc=login("participantes",comprador); ct=login("participantes",terceiro);
        long n=NUMERO.incrementAndGet();
        organizador=insert("INSERT INTO organizadores(nome,email,senha,cnpj) SELECT 'Organizador teste 03',?,senha,? FROM organizadores WHERE id_organizador=1","o"+n+"@joinup.test","000"+n);
        co=login("organizadores",organizador);
        var r=pedir("POST","/api/organizador/eventos",Map.ofEntries(Map.entry("nome","Evento comércio 03"),Map.entry("descricao","Fixture de compra e revenda"),Map.entry("data",Instant.now().plusSeconds(2592000).toString()),Map.entry("localizacao","Arena 03"),Map.entry("cidade","Curitiba"),Map.entry("estado","PR"),Map.entry("cep","80000000"),Map.entry("generoId",2),Map.entry("artistaIds",List.of(1,3)),Map.entry("tiposIngresso",List.of(Map.of("nome","Pista","preco",new BigDecimal("10.10"),"quantidade",10),Map.of("nome","VIP","preco",new BigDecimal("20.25"),"quantidade",10)))),co);
        assertEquals(201,r.statusCode(),r.body()); evento=num(dados(r),"id"); tipo=valor("SELECT MIN(id_tipos_ingressos) FROM tipos_ingressos WHERE id_evento="+evento);
    }
    @AfterEach void limpar() throws Exception {
        exec("DELETE FROM comunidade_mensagens WHERE id_comunidade IN(SELECT id_comunidade FROM comunidades WHERE id_evento>?)",LIMITES.get("eventos"));
        exec("DELETE FROM postagens WHERE id_comunidade IN(SELECT id_comunidade FROM comunidades WHERE id_evento>?)",LIMITES.get("eventos"));
        exec("DELETE FROM line_up WHERE id_evento>?",LIMITES.get("eventos"));
        for(String t:List.of("auditoria_reativacoes","sessoes","revenda_anuncios","ingressos","compras","comunidades","tipos_ingressos","eventos","participantes","organizadores")) exec("DELETE FROM "+t+" WHERE "+PK.get(t)+">?",LIMITES.get(t));
    }
    @AfterAll static void terminar() throws Exception {
        if(client!=null) client.close(); if(app!=null) app.close();
        Files.createDirectories(Path.of("target/mysql-validation")); Files.write(Path.of("target/mysql-validation/comercio03.txt"),EVIDENCIA,StandardCharsets.UTF_8);
    }
    static Connection admin() throws SQLException { return DriverManager.getConnection(config.getUrlJdbc(),System.getenv("JOINUP_IT_ADMIN_USER"),System.getenv("JOINUP_IT_ADMIN_PASSWORD")); }
    static String escalar(Connection c,String sql) throws SQLException { try(var s=c.createStatement();var r=s.executeQuery(sql)) { r.next(); return r.getString(1); } }
    static long valor(String sql) throws SQLException { try(var c=admin()) { return Long.parseLong(escalar(c,sql)); } }
    static void exec(String sql,Object... args) throws SQLException { try(var c=admin();var p=c.prepareStatement(sql)) { for(int i=0;i<args.length;i++) p.setObject(i+1,args[i]); p.executeUpdate(); } }
    static long insert(String sql,Object... args) throws SQLException { try(var c=admin();var p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) { for(int i=0;i<args.length;i++) p.setObject(i+1,args[i]); p.executeUpdate(); try(var r=p.getGeneratedKeys()) { r.next(); return r.getLong(1); } } }
    static long participante() throws SQLException { long n=NUMERO.incrementAndGet(); return insert("INSERT INTO participantes(nome,email,cpf,data_nascimento,senha) SELECT 'Participante teste 03',?,?,'1990-01-01',senha FROM participantes WHERE id_participante=1","p"+n+"@joinup.test",Long.toString(n)); }
    static HttpResponse<String> pedir(String method,String path,Object body,String cookie) throws Exception { return pedir(method,path,body,cookie,"self"); }
    static HttpResponse<String> pedir(String method,String path,Object body,String cookie,String origin) throws Exception {
        String b=body==null?"":body instanceof String s?s:new String(JSON.escrever(body),StandardCharsets.UTF_8);
        var q=HttpRequest.newBuilder(URI.create(base+path)).timeout(Duration.ofSeconds(25)).header("Content-Type","application/json").method(method,HttpRequest.BodyPublishers.ofString(b));
        if(cookie!=null) q.header("Cookie",cookie); if(origin!=null) q.header("Origin",origin.equals("self")?base:origin);
        return client.send(q.build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
    static String login(String perfil,long id) throws Exception {
        String tabela=perfil.equals("suporte")?"usuarios_suporte":perfil; String coluna=perfil.equals("participantes")?"id_participante":perfil.equals("organizadores")?"id_organizador":"id_usuario_suporte";
        String email; try(var c=admin()) { email=escalar(c,"SELECT email FROM "+tabela+" WHERE "+coluna+"="+id); }
        var r=pedir("POST","/api/auth/"+perfil+"/login",Map.of("email",email,"senha","JoinUpDemo#2026"),null); assertEquals(200,r.statusCode(),r.body()); return r.headers().firstValue("Set-Cookie").orElseThrow().split(";",2)[0];
    }
    static Map<?,?> envelope(HttpResponse<String> r) { return JSON.ler(r.body().getBytes(StandardCharsets.UTF_8),Map.class); }
    static Map<?,?> dados(HttpResponse<String> r) { return (Map<?,?>)envelope(r).get("dados"); }
    static List<?> lista(HttpResponse<String> r) { return (List<?>)envelope(r).get("dados"); }
    static long num(Map<?,?> m,String chave) { return ((Number)m.get(chave)).longValue(); }
    Map<String,Object> pedido(int quantidade) { return Map.of("eventoId",evento,"itens",List.of(Map.of("tipoIngressoId",tipo,"quantidade",quantidade))); }
    long comprar(String ck) throws Exception { var r=pedir("POST","/api/compras",pedido(1),ck); assertEquals(201,r.statusCode(),r.body()); return ((Number)((List<?>)dados(r).get("ingressoIds")).getFirst()).longValue(); }
    long anunciar(long ingresso) throws Exception { var r=pedir("POST","/api/revendas",Map.of("ingressoIds",List.of(ingresso),"precoUnitario",new BigDecimal("12.34")),cv); assertEquals(201,r.statusCode(),r.body()); return num((Map<?,?>)lista(r).getFirst(),"id"); }
    String anuncio(long id,String acao) { return "/api/revendas/"+id+"/"+acao; }
    void associar() throws SQLException { exec("INSERT INTO participante_comunidades(id_participante,id_comunidade) SELECT ?,id_comunidade FROM comunidades WHERE id_evento=?",vendedor,evento); }
    long membro() throws SQLException { return valor("SELECT ativo FROM participante_comunidades pc JOIN comunidades c ON c.id_comunidade=pc.id_comunidade WHERE pc.id_participante="+vendedor+" AND c.id_evento="+evento); }
    Map<String,List<List<String>>> snapshot() throws SQLException {
        var m=new TreeMap<String,List<List<String>>>(); try(var c=admin()) {
            for(String t:List.of("tipos_ingressos","compras","compra_itens","ingressos","revenda_anuncios","participante_comunidades","postagens","comunidade_mensagens")) {
                var linhas=new ArrayList<List<String>>(); try(var st=c.createStatement();var rs=st.executeQuery("SELECT * FROM "+t)) { while(rs.next()) { var v=new ArrayList<String>(); for(int i=1;i<=rs.getMetaData().getColumnCount();i++) v.add(rs.getString(i)); linhas.add(v); } }
                linhas.sort(Comparator.comparing(Object::toString)); m.put(t,linhas);
            }
        } return m;
    }

    @Test void variosTiposTotalExatoSnapshotsEmissaoEstoqueHistoricoEPrivacidade() throws Exception {
        long vip=valor("SELECT MAX(id_tipos_ingressos) FROM tipos_ingressos WHERE id_evento="+evento);
        var r=pedir("POST","/api/compras",Map.of("eventoId",evento,"itens",List.of(Map.of("tipoIngressoId",vip,"quantidade",1),Map.of("tipoIngressoId",tipo,"quantidade",2))),cv);
        assertEquals(201,r.statusCode(),r.body()); long compra=num(dados(r),"compraId"); assertEquals(new BigDecimal("40.45"),new BigDecimal(dados(r).get("total").toString()));
        assertEquals(3,((List<?>)dados(r).get("ingressoIds")).size()); assertEquals(3,valor("SELECT COUNT(*) FROM ingressos WHERE id_compra="+compra+" AND status='ativo' AND id_participante="+vendedor));
        assertEquals(8,valor("SELECT quantidade_disponivel FROM tipos_ingressos WHERE id_tipos_ingressos="+tipo)); assertEquals(2,valor("SELECT quantidade_vendida FROM tipos_ingressos WHERE id_tipos_ingressos="+tipo));
        assertEquals(9,valor("SELECT quantidade_disponivel FROM tipos_ingressos WHERE id_tipos_ingressos="+vip)); assertEquals(1,valor("SELECT quantidade_vendida FROM tipos_ingressos WHERE id_tipos_ingressos="+vip));
        try(var c=admin()) { assertEquals("40.45",escalar(c,"SELECT valor_total FROM compras WHERE id_compra="+compra)); assertEquals("20.20",escalar(c,"SELECT subtotal FROM compra_itens WHERE id_compra="+compra+" AND id_tipo_ingresso="+tipo)); assertEquals("Evento comércio 03",escalar(c,"SELECT nome_evento_na_compra FROM compras WHERE id_compra="+compra)); }
        assertEquals(0,valor("SELECT COUNT(*) FROM participante_comunidades WHERE id_participante="+vendedor));
        assertEquals(200,pedir("PATCH","/api/organizador/eventos/"+evento,Map.of("nome","Evento renomeado","localizacao","Outro local","data",Instant.now().plusSeconds(3000000).toString()),co).statusCode());
        var h=pedir("GET","/api/me/compras",null,cv); assertEquals(1,lista(h).size()); assertTrue(h.body().contains("Evento comércio 03")); assertFalse(h.body().contains("Evento renomeado"));
        var tickets=pedir("GET","/api/me/ingressos",null,cv); assertEquals(3,lista(tickets).size()); assertTrue(tickets.body().contains("Evento renomeado")); assertTrue(tickets.body().contains("Evento comércio 03"));
        for(String privado:List.of("cpf","cnpj","senha","hash","token","email","id_participante","participanteId")) { assertFalse(h.body().contains("\""+privado+"\"")); assertFalse(tickets.body().contains("\""+privado+"\"")); }
        EVIDENCIA.add("Compra mult categoria: total 40.45; 3 ingressos; estoque 8/9; vendidos 2/1; snapshots preservados após edição; sem filiação automática.");
    }
    @Test void ultimaUnidadeDoisCompradoresSomenteUmaCompra() throws Exception {
        exec("UPDATE tipos_ingressos SET quantidade_disponivel=1 WHERE id_tipos_ingressos=?",tipo);
        var status=disputar(()->pedir("POST","/api/compras",pedido(1),cv),()->pedir("POST","/api/compras",pedido(1),cc)); assertEquals(List.of(201,409),status);
        assertEquals(0,valor("SELECT quantidade_disponivel FROM tipos_ingressos WHERE id_tipos_ingressos="+tipo)); assertEquals(1,valor("SELECT quantidade_vendida FROM tipos_ingressos WHERE id_tipos_ingressos="+tipo)); assertEquals(1,valor("SELECT COUNT(*) FROM ingressos WHERE id_evento="+evento)); assertEquals(1,valor("SELECT COUNT(*) FROM compras WHERE id_evento="+evento));
        EVIDENCIA.add("Última unidade: dois POST concorrentes -> [201,409]; estoque=0, vendido=1, compra=1, ingresso=1.");
    }
    static List<Integer> disputar(Callable<HttpResponse<String>> a,Callable<HttpResponse<String>> b) throws Exception {
        try(var pool=Executors.newFixedThreadPool(2)) { var inicio=new CountDownLatch(1); var x=pool.submit(()->{inicio.await();return a.call();}); var y=pool.submit(()->{inicio.await();return b.call();}); inicio.countDown(); var s=new ArrayList<>(List.of(x.get(30,TimeUnit.SECONDS).statusCode(),y.get(30,TimeUnit.SECONDS).statusCode())); Collections.sort(s);return s; }
    }
    @ParameterizedTest @ValueSource(strings={"[]","{}","{\"itens\":[]}","null","duplicado","quantidade0","fracao","texto","preco","total","titular","tipoOutro","acima","nulo","overflow"})
    void compraInvalidaNaoMudaNenhumaTabela(String caso) throws Exception {
        Object body=pedido(1); int esperado=400;
        var item=new HashMap<String,Object>(Map.of("tipoIngressoId",tipo,"quantidade",1)); var d=new HashMap<String,Object>(pedido(1));
        switch(caso) {
            case "[]","{}","{\"itens\":[]}","null" -> { body=caso; if(caso.equals("{}") || caso.equals("{\"itens\":[]}")) esperado=422; }
            case "duplicado" -> { d.put("itens",List.of(item,item));body=d;esperado=422; }
            case "quantidade0" -> { item.put("quantidade",0);d.put("itens",List.of(item));body=d;esperado=422; }
            case "fracao","texto" -> { item.put("quantidade",caso.equals("fracao")?1.5:"1");d.put("itens",List.of(item));body=d; }
            case "preco","total","titular" -> { d.put(caso.equals("preco")?"preco":caso.equals("total")?"valor_total":"id_participante",1);body=d; }
            case "tipoOutro" -> { item.put("tipoIngressoId",1);d.put("itens",List.of(item));body=d;esperado=422; }
            case "acima" -> { body=pedido(11);esperado=409; }
            case "nulo" -> { item.put("quantidade",null);d.put("itens",List.of(item));body=d; }
            case "overflow" -> { item.put("quantidade",2147483648L);d.put("itens",List.of(item));body=d; }
        }
        var antes=snapshot(); var r=pedir("POST","/api/compras",body,cv);assertEquals(esperado,r.statusCode(),r.body());assertEquals(antes,snapshot());
    }
    @ParameterizedTest @ValueSource(strings={"inicio","passado","organizador","zeroEstoque"})
    void elegibilidadeDiretaRevalidadaNoServidor(String caso) throws Exception {
        if(caso.equals("organizador")) exec("UPDATE organizadores SET ativo=0 WHERE id_organizador=?",organizador);
        else if(caso.equals("zeroEstoque")) exec("UPDATE tipos_ingressos SET quantidade_disponivel=0 WHERE id_tipos_ingressos=?",tipo);
        else exec("UPDATE eventos SET data=TIMESTAMPADD(SECOND,?,UTC_TIMESTAMP()) WHERE id_evento=?",caso.equals("inicio")?0:-86400,evento);
        var antes=snapshot();assertEquals(409,pedir("POST","/api/compras",pedido(1),cv).statusCode());assertEquals(antes,snapshot());
    }
    @Test void autorizacaoTodasRotasCsrfIdorEPrecoNaoControladoPeloCliente() throws Exception {
        long i=comprar(cv),a=anunciar(i); String suporte=login("suporte",1);
        for(String ck:Arrays.asList(null,co,suporte)) {
            int status=ck==null?401:403;
            for(String path:List.of("/api/revendas","/api/revendas/"+a,"/api/me/revendas","/api/me/compras","/api/me/ingressos","/api/me/ingressos/"+i)) assertEquals(status,pedir("GET",path,null,ck).statusCode(),path);
            for(String path:List.of("/api/compras","/api/revendas",anuncio(a,"compra"),anuncio(a,"cancelamento"))) assertEquals(status,pedir("POST",path,Map.of(),ck).statusCode(),path);
        }
        assertEquals(404,pedir("GET","/api/me/ingressos/"+i,null,cc).statusCode());assertEquals(404,pedir("POST",anuncio(a,"cancelamento"),Map.of(),cc).statusCode());
        assertEquals(403,pedir("POST",anuncio(a,"compra"),Map.of(),cv).statusCode());
        for(String path:List.of("/api/compras","/api/revendas",anuncio(a,"compra"),anuncio(a,"cancelamento"))) for(String origin:Arrays.asList(null,"https://evil.test","null")) assertEquals(403,pedir("POST",path,Map.of(),cc,origin).statusCode());
        for(String campo:List.of("preco","total","id_comprador","status","id_participante")) assertEquals(400,pedir("POST",anuncio(a,"compra"),Map.of(campo,1),cc).statusCode());
        assertEquals(200,pedir("POST",anuncio(a,"compra"),Map.of(),cc).statusCode());assertEquals(404,pedir("GET","/api/me/ingressos/"+i,null,cv).statusCode());assertEquals(200,pedir("GET","/api/me/ingressos/"+i,null,cc).statusCode());
        var h=pedir("GET","/api/me/compras",null,cc);assertEquals(1,((List<?>)envelope(h).get("revendasRecebidas")).size());assertEquals(0,lista(h).size());assertTrue(h.body().contains("12.34"));
        for(String p:List.of("cpf","cnpj","email","senha","hash_token","id_comprador","id_participante")) assertFalse(pedir("GET","/api/revendas/"+a,null,ct).body().contains("\""+p+"\""));
    }
    @Test void participanteInativoESessaoRevogadaNegamListaECompras() throws Exception {
        long i=comprar(cv),a=anunciar(i); assertEquals(200,pedir("POST","/api/me/desativacao",null,cv).statusCode());
        var antes=snapshot(); for(String path:List.of("/api/revendas","/api/me/ingressos","/api/me/compras")) assertEquals(401,pedir("GET",path,null,cv).statusCode());assertEquals(401,pedir("POST","/api/compras",pedido(1),cv).statusCode());assertEquals(409,pedir("POST",anuncio(a,"compra"),Map.of(),cc).statusCode());assertEquals(antes,snapshot());
    }
    @ParameterizedTest @ValueSource(strings={"compras","compra_itens","ingressos"})
    void rollbackCompraDepoisDaBaixaPorTrigger(String tabela) throws Exception {
        String cond=tabela.equals("compra_itens")?"NEW.id_tipo_ingresso="+tipo:"NEW.id_evento="+evento;
        exec("CREATE TRIGGER it03_compra BEFORE INSERT ON "+tabela+" FOR EACH ROW BEGIN IF "+cond+" THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='falha fixture'; END IF; END");
        try { var antes=snapshot();var r=pedir("POST","/api/compras",pedido(2),cv);assertEquals(503,r.statusCode(),r.body());assertEquals(antes,snapshot());assertFalse(r.body().contains("fixture"));EVIDENCIA.add("Rollback compra: trigger "+tabela+" depois da baixa -> 503; todas as colunas de estoque/compras/itens/ingressos inalteradas."); }
        finally { exec("DROP TRIGGER it03_compra"); }
    }
    @ParameterizedTest @ValueSource(strings={"cancelado","utilizado","eventoPassado","organizadorInativo","outroDono","duplicado"})
    void anuncioSomenteIngressoElegivelProprioSemDuplicacao(String caso) throws Exception {
        long i=comprar(cv);
        if(caso.equals("cancelado") || caso.equals("utilizado")) exec("UPDATE ingressos SET status=? WHERE id_ingresso=?",caso,i);
        if(caso.equals("eventoPassado")) exec("UPDATE eventos SET data=UTC_TIMESTAMP() WHERE id_evento=?",evento);
        if(caso.equals("organizadorInativo")) exec("UPDATE organizadores SET ativo=0 WHERE id_organizador=?",organizador);
        if(caso.equals("outroDono")) exec("UPDATE ingressos SET id_participante=? WHERE id_ingresso=?",comprador,i);
        if(caso.equals("duplicado")) anunciar(i);
        var antes=snapshot();assertEquals(409,pedir("POST","/api/revendas",Map.of("ingressoIds",List.of(i),"precoUnitario",12),cv).statusCode());assertEquals(antes,snapshot());
    }
    @ParameterizedTest @ValueSource(strings={"0","-1","1000000","0.001","12.345","\"12.00\"","null"})
    void precoRevendaDecimalExatoPositivoELimite(String preco) throws Exception {
        long i=comprar(cv);var antes=snapshot();var r=pedir("POST","/api/revendas","{\"ingressoIds\":["+i+"],\"precoUnitario\":"+preco+"}",cv);
        assertEquals(preco.startsWith("\"") || preco.equals("null")?400:422,r.statusCode(),r.body());assertEquals(antes,snapshot());
    }
    @Test void loteAnuncioAtomicoUniqueMySqlECancelarReanunciarHistorico() throws Exception {
        long i=comprar(cv),j=comprar(cv);associar();
        var r=pedir("POST","/api/revendas",Map.of("ingressoIds",List.of(j,i),"precoUnitario",new BigDecimal("999999.99")),cv);assertEquals(201,r.statusCode(),r.body());assertEquals(2,lista(r).size());assertEquals(1,membro());
        long a=num((Map<?,?>)lista(r).getFirst(),"id");
        try(var c=admin()) { c.setAutoCommit(false);try { assertEquals(1062,assertThrows(SQLException.class,()->{try(var p=c.prepareStatement("INSERT INTO revenda_anuncios(id_ingresso,id_participante,valor_revenda,status) VALUES(?,?,1,'disponivel')")) { p.setLong(1,i);p.setLong(2,vendedor);p.executeUpdate(); }}).getErrorCode()); } finally { c.rollback(); } }
        assertEquals(200,pedir("POST",anuncio(a,"cancelamento"),Map.of(),cv).statusCode());assertEquals(vendedor,valor("SELECT id_participante FROM ingressos WHERE id_ingresso="+i));assertEquals(1,membro());
        long novo=anunciar(i);assertNotEquals(a,novo);assertEquals(409,pedir("POST",anuncio(a,"compra"),Map.of(),cc).statusCode());
        try(var c=admin()) { assertEquals("cancelado",escalar(c,"SELECT status FROM revenda_anuncios WHERE id_revenda_anuncios="+a));assertEquals("999999.99",escalar(c,"SELECT valor_revenda FROM revenda_anuncios WHERE id_revenda_anuncios="+a)); }
        assertEquals(3,lista(pedir("GET","/api/me/revendas",null,cv)).size());assertEquals(200,pedir("POST",anuncio(novo,"compra"),Map.of(),cc).statusCode());assertEquals(409,pedir("POST",anuncio(novo,"cancelamento"),Map.of(),cv).statusCode());
    }
    @Test void loteComUmIngressoInvalidoNaoPublicaNenhumAnuncio() throws Exception {
        long i=comprar(cv),j=comprar(cv);exec("UPDATE ingressos SET status='utilizado' WHERE id_ingresso=?",j);var antes=snapshot();assertEquals(409,pedir("POST","/api/revendas",Map.of("ingressoIds",List.of(i,j),"precoUnitario",12),cv).statusCode());assertEquals(antes,snapshot());
    }
    @Test void doisCompradoresMesmoAnuncioSoUmaTransferenciaSemNovoIngresso() throws Exception {
        long i=comprar(cv),a=anunciar(i);associar();var status=disputar(()->pedir("POST",anuncio(a,"compra"),Map.of(),cc),()->pedir("POST",anuncio(a,"compra"),Map.of(),ct));assertEquals(List.of(200,409),status);
        assertEquals(1,valor("SELECT COUNT(*) FROM ingressos WHERE id_evento="+evento));assertEquals(1,valor("SELECT COUNT(*) FROM compras WHERE id_evento="+evento));assertEquals(1,valor("SELECT COUNT(*) FROM revenda_anuncios WHERE id_revenda_anuncios="+a+" AND status='vendido' AND id_comprador IN("+comprador+","+terceiro+") AND data_venda IS NOT NULL"));
        assertEquals(1,valor("SELECT COUNT(*) FROM ingressos i JOIN revenda_anuncios a ON a.id_ingresso=i.id_ingresso WHERE a.id_revenda_anuncios="+a+" AND i.id_participante=a.id_comprador"));assertEquals(0,membro());
        EVIDENCIA.add("Mesmo anúncio: dois compradores -> [200,409]; 1 venda/transferência, sem emissão/compra direta adicional; vendedor sai após último ingresso.");
    }
    @ParameterizedTest @ValueSource(strings={"nenhum","ativo","utilizado","cancelado"})
    void comunidadeSomenteSaiAposVendaUltimoElegivel(String outroStatus) throws Exception {
        long i=comprar(cv);associar();
        if(!outroStatus.equals("nenhum")) { long j=comprar(cv);exec("UPDATE ingressos SET status=? WHERE id_ingresso=?",outroStatus,j); }
        exec("INSERT INTO comunidade_mensagens(id_comunidade,autor_tipo,id_participante,mensagem) SELECT id_comunidade,'participante',?,'Mensagem histórica' FROM comunidades WHERE id_evento=?",vendedor,evento);
        exec("INSERT INTO postagens(id_comunidade,id_participante,descricao) SELECT id_comunidade,?,'Post histórico' FROM comunidades WHERE id_evento=?",vendedor,evento);
        long a=anunciar(i);assertEquals(1,membro());assertEquals(200,pedir("POST",anuncio(a,"compra"),Map.of(),cc).statusCode());assertEquals(Set.of("ativo","utilizado").contains(outroStatus)?1:0,membro());
        assertEquals(0,valor("SELECT COUNT(*) FROM participante_comunidades pc JOIN comunidades c ON c.id_comunidade=pc.id_comunidade WHERE pc.id_participante="+comprador+" AND c.id_evento="+evento));
        assertEquals(1,valor("SELECT COUNT(*) FROM comunidade_mensagens m JOIN comunidades c ON c.id_comunidade=m.id_comunidade WHERE c.id_evento="+evento));assertEquals(1,valor("SELECT COUNT(*) FROM postagens p JOIN comunidades c ON c.id_comunidade=p.id_comunidade WHERE c.id_evento="+evento));
        if(membro()==0) assertEquals(1,valor("SELECT COUNT(*) FROM participante_comunidades pc JOIN comunidades c ON c.id_comunidade=pc.id_comunidade WHERE pc.id_participante="+vendedor+" AND c.id_evento="+evento+" AND data_saida IS NOT NULL"));
    }
    @Test void organizadorInativoPreservaAnuncioCancelarPermitidoReativarSoDisponivel() throws Exception {
        long i=comprar(cv),j=comprar(cv),a=anunciar(i),b=anunciar(j);
        assertEquals(200,pedir("POST","/api/me/desativacao",null,co).statusCode());
        var r=pedir("GET","/api/revendas?eventoId="+evento,null,cc);assertEquals(2,lista(r).size());for(Object obj:lista(r)) assertEquals(false,((Map<?,?>)obj).get("compravel"));
        assertEquals(409,pedir("POST",anuncio(a,"compra"),Map.of(),cc).statusCode());assertEquals(200,pedir("POST",anuncio(b,"cancelamento"),Map.of(),cv).statusCode());
        assertEquals(200,pedir("POST","/api/suporte/contas/organizador/"+organizador+"/reativacao",null,login("suporte",1)).statusCode());
        assertEquals(true,dados(pedir("GET","/api/revendas/"+a,null,cc)).get("compravel"));assertEquals(false,dados(pedir("GET","/api/revendas/"+b,null,cc)).get("compravel"));assertEquals(200,pedir("POST",anuncio(a,"compra"),Map.of(),cc).statusCode());
    }
    @Test void passadoBloqueiaCompraERevendaMasPreservaIngressoUtilizadoEMembro() throws Exception {
        long i=comprar(cv),a=anunciar(i);associar();exec("UPDATE ingressos SET status='utilizado' WHERE id_ingresso=?",i);exec("UPDATE eventos SET data=UTC_TIMESTAMP()-INTERVAL 1 DAY WHERE id_evento=?",evento);var antes=snapshot();
        assertEquals(409,pedir("POST","/api/compras",pedido(1),cc).statusCode());assertEquals(409,pedir("POST",anuncio(a,"compra"),Map.of(),cc).statusCode());assertEquals(409,pedir("POST","/api/revendas",Map.of("ingressoIds",List.of(i),"precoUnitario",12),cv).statusCode());assertEquals(antes,snapshot());assertEquals(1,membro());assertEquals("utilizado",dados(pedir("GET","/api/me/ingressos/"+i,null,cv)).get("status"));
    }
    @ParameterizedTest @ValueSource(strings={"ingressos","revenda_anuncios","participante_comunidades"})
    void rollbackRevendaPorTriggerIncluiTransferenciaAnuncioEMembro(String tabela) throws Exception {
        long i=comprar(cv),a=anunciar(i);associar();String cond=tabela.equals("ingressos")?"NEW.id_ingresso="+i:tabela.equals("revenda_anuncios")?"NEW.id_revenda_anuncios="+a:"NEW.id_participante="+vendedor;
        exec("CREATE TRIGGER it03_revenda BEFORE UPDATE ON "+tabela+" FOR EACH ROW BEGIN IF "+cond+" THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='falha fixture'; END IF; END");
        try { var antes=snapshot();assertEquals(503,pedir("POST",anuncio(a,"compra"),Map.of(),cc).statusCode());assertEquals(antes,snapshot());EVIDENCIA.add("Rollback revenda: trigger "+tabela+" -> 503; titularidade/anúncio/membership e históricos integralmente preservados."); }
        finally { exec("DROP TRIGGER it03_revenda"); }
    }
    @Test void vendedorDesativacaoConcorrenteComVendaCancelaOuConcluiAtomicamente() throws Exception {
        long i=comprar(cv),a=anunciar(i);var status=disputar(()->pedir("POST",anuncio(a,"compra"),Map.of(),cc),()->pedir("POST","/api/me/desativacao",null,cv));assertTrue(status.equals(List.of(200,200)) || status.equals(List.of(200,409)),status.toString());
        assertEquals(0,valor("SELECT ativo FROM participantes WHERE id_participante="+vendedor));assertEquals(0,valor("SELECT COUNT(*) FROM revenda_anuncios WHERE id_participante="+vendedor+" AND status='disponivel'"));
        try(var c=admin()) { String s=escalar(c,"SELECT status FROM revenda_anuncios WHERE id_revenda_anuncios="+a);long dono=valor("SELECT id_participante FROM ingressos WHERE id_ingresso="+i);assertEquals(s.equals("vendido")?comprador:vendedor,dono); }
        EVIDENCIA.add("Venda/desativação vendedor concorrentes: "+status+"; conta inativa, nenhum anúncio disponível, estado/titularidade consistentes.");
    }
    @Test void comprasCruzadasEntreParticipantesSemDeadlock() throws Exception {
        long i=comprar(cv),j=comprar(cc),a=anunciar(i);
        var r=pedir("POST","/api/revendas",Map.of("ingressoIds",List.of(j),"precoUnitario",15),cc);assertEquals(201,r.statusCode());long b=num((Map<?,?>)lista(r).getFirst(),"id");
        assertEquals(List.of(200,200),disputar(()->pedir("POST",anuncio(a,"compra"),Map.of(),cc),()->pedir("POST",anuncio(b,"compra"),Map.of(),cv)));assertEquals(comprador,valor("SELECT id_participante FROM ingressos WHERE id_ingresso="+i));assertEquals(vendedor,valor("SELECT id_participante FROM ingressos WHERE id_ingresso="+j));
        EVIDENCIA.add("Compras cruzadas A->B/B->A: [200,200]; contas em ordem crescente evitam ciclo de locks.");
    }
    @ParameterizedTest @ValueSource(strings={"direta","revenda","publicacao"})
    void horarioRevalidadoDepoisDeEsperarLockDoEvento(String fluxo) throws Exception {
        long i=comprar(cv),a=anunciar(i),j=comprar(cv); var antes=snapshot();
        String path=fluxo.equals("direta")?"/api/compras":fluxo.equals("revenda")?anuncio(a,"compra"):"/api/revendas";
        Object body=fluxo.equals("direta")?pedido(1):fluxo.equals("revenda")?Map.of():Map.of("ingressoIds",List.of(j),"precoUnitario",12);
        String ck=fluxo.equals("publicacao")?cv:cc;
        try(var c=admin();var pool=Executors.newSingleThreadExecutor()) {
            c.setAutoCommit(false);bloquear(c,"eventos","id_evento",evento);
            var futuro=pool.submit(()->pedir("POST",path,body,ck));
            esperarLock("SELECT id_evento,id_organizador,nome,data,localizacao FROM eventos WHERE id_evento=");
            try(var p=c.prepareStatement("UPDATE eventos SET data=UTC_TIMESTAMP() WHERE id_evento=?")) {p.setLong(1,evento);p.executeUpdate();} c.commit();
            assertEquals(409,futuro.get(15,TimeUnit.SECONDS).statusCode());assertEquals(antes,snapshot());
        }
        EVIDENCIA.add("Espera real FOR UPDATE evento em "+fluxo+": início alterado durante espera; 409 com comércio integralmente inalterado.");
    }
    static void bloquear(Connection c,String tabela,String chave,long id) throws SQLException {
        try(var p=c.prepareStatement("SELECT "+chave+" FROM "+tabela+" WHERE "+chave+"=? FOR UPDATE")) {p.setLong(1,id);try(var r=p.executeQuery()){assertTrue(r.next());}}
    }
    static void esperarLock(String consulta) throws Exception {
        long fim=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);boolean esperando=false;
        while(System.nanoTime()<fim) {
            try(var c=admin();var p=c.prepareStatement("SELECT COUNT(*) FROM information_schema.processlist WHERE USER='joinup_it' AND INFO LIKE ?")) {p.setString(1,consulta+"%FOR UPDATE");try(var r=p.executeQuery()){r.next();if(r.getInt(1)>0){esperando=true;break;}}}
            Thread.sleep(25);
        }
        assertTrue(esperando,"Requisição deve ter chegado ao SELECT bloqueado no MySQL");
    }
    @ParameterizedTest @ValueSource(strings={"direta","revenda"})
    void compradorDesativacaoConcorrenteSemTransacaoParcial(String fluxo) throws Exception {
        long i=comprar(cv),a=anunciar(i);
        var status=disputar(()->pedir("POST",fluxo.equals("direta")?"/api/compras":anuncio(a,"compra"),fluxo.equals("direta")?pedido(1):Map.of(),cc),()->pedir("POST","/api/me/desativacao",null,cc));
        assertTrue(fluxo.equals("direta")?status.equals(List.of(200,201)) || status.equals(List.of(200,401)):status.equals(List.of(200,200)) || status.equals(List.of(200,401)),status.toString());
        assertEquals(0,valor("SELECT ativo FROM participantes WHERE id_participante="+comprador));
        long compras=valor("SELECT COUNT(*) FROM compras WHERE id_participante="+comprador+" AND id_evento="+evento),tickets=valor("SELECT COUNT(*) FROM ingressos WHERE id_participante="+comprador+" AND id_evento="+evento);
        assertTrue(tickets==0 || tickets==1);if(fluxo.equals("direta")) assertEquals(tickets,compras);
        else assertEquals(tickets,valor("SELECT COUNT(*) FROM revenda_anuncios WHERE id_revenda_anuncios="+a+" AND status='vendido' AND id_comprador="+comprador));
        EVIDENCIA.add("Desativação comprador / "+fluxo+" concorrentes: "+status+"; conta/sessão inativas, estado comercial consistente.");
    }
    @Test void organizadorDesativacaoConcorrenteComCompra() throws Exception {
        var status=disputar(()->pedir("POST","/api/compras",pedido(1),cc),()->pedir("POST","/api/me/desativacao",null,co));
        assertTrue(status.equals(List.of(200,201)) || status.equals(List.of(200,409)),status.toString());assertEquals(0,valor("SELECT ativo FROM organizadores WHERE id_organizador="+organizador));
        long n=valor("SELECT COUNT(*) FROM ingressos WHERE id_evento="+evento);assertEquals(n,valor("SELECT quantidade_vendida FROM tipos_ingressos WHERE id_tipos_ingressos="+tipo));assertEquals(10-n,valor("SELECT quantidade_disponivel FROM tipos_ingressos WHERE id_tipos_ingressos="+tipo));assertEquals(409,pedir("POST","/api/compras",pedido(1),cc).statusCode());
    }
    @Test void precoEstoqueAtualizadosDuranteEsperaUsamValorPersistido() throws Exception {
        try(var c=admin();var pool=Executors.newSingleThreadExecutor()) {
            c.setAutoCommit(false);bloquear(c,"eventos","id_evento",evento);
            var futuro=pool.submit(()->pedir("POST","/api/compras",pedido(2),cc));esperarLock("SELECT id_evento,id_organizador,nome,data,localizacao FROM eventos WHERE id_evento=");
            try(var p=c.prepareStatement("UPDATE tipos_ingressos SET valor=25.15,quantidade_disponivel=2 WHERE id_tipos_ingressos=?")) {p.setLong(1,tipo);p.executeUpdate();}c.commit();
            var r=futuro.get(15,TimeUnit.SECONDS);assertEquals(201,r.statusCode(),r.body());assertEquals(0,new BigDecimal("50.30").compareTo(new BigDecimal(dados(r).get("total").toString())));
            try(var ver=admin()) {assertEquals("25.15",escalar(ver,"SELECT valor_unitario FROM compra_itens WHERE id_tipo_ingresso="+tipo));}assertEquals(0,valor("SELECT quantidade_disponivel FROM tipos_ingressos WHERE id_tipos_ingressos="+tipo));
        }
    }
    @Test void timeoutDeLockRetornaConflitoEReverteAtualizacaoAnterior() throws Exception {
        var executor=new br.com.joinup.shared.db.ExecutorTransacional(new br.com.joinup.shared.db.ConnectionFactory(config));
        try(var c=admin()) {
            c.setAutoCommit(false);bloquear(c,"eventos","id_evento",evento);
            var e=assertThrows(br.com.joinup.http.HttpException.class,()->executor.executar(tx->{
                try(var st=tx.createStatement()) {st.execute("SET SESSION innodb_lock_wait_timeout=1");}
                try(var p=tx.prepareStatement("UPDATE tipos_ingressos SET quantidade_disponivel=0 WHERE id_tipos_ingressos=?")) {p.setLong(1,tipo);p.executeUpdate();}
                bloquear(tx,"eventos","id_evento",evento);return null;
            }));
            assertEquals(409,e.status());assertEquals("CONCORRENCIA_CONFLITO",e.codigo());assertEquals(10,valor("SELECT quantidade_disponivel FROM tipos_ingressos WHERE id_tipos_ingressos="+tipo));c.rollback();
        }
        EVIDENCIA.add("Lock timeout MySQL 1205 real: HTTP/domínio 409 CONCORRENCIA_CONFLITO; executor fez rollback da alteração anterior.");
    }
    @Test void limiteDeTotalContadorEEstoqueZeroProtegidos() throws Exception {
        exec("UPDATE tipos_ingressos SET valor=999999.99,quantidade_disponivel=101 WHERE id_tipos_ingressos=?",tipo);var antes=snapshot();assertEquals(422,pedir("POST","/api/compras",pedido(101),cv).statusCode());assertEquals(antes,snapshot());
        exec("UPDATE tipos_ingressos SET quantidade_vendida=2147483647 WHERE id_tipos_ingressos=?",tipo);antes=snapshot();assertEquals(409,pedir("POST","/api/compras",pedido(1),cv).statusCode());assertEquals(antes,snapshot());
    }
    @Test void filtrosIdsDadosExtrasEFrontendRealServidos() throws Exception {
        long i=comprar(cv),a=anunciar(i);
        for(String q:List.of("pagina=0","tamanho=101","pagina=2147483647&tamanho=100","eventoId=-1","id_participante=1","pagina=1&pagina=2")) assertEquals(400,pedir("GET","/api/revendas?"+q,null,cc).statusCode());
        assertEquals(1,lista(pedir("GET","/api/revendas?eventoId="+evento+"&tamanho=1&pagina=1",null,cc)).size());assertEquals(0,lista(pedir("GET","/api/revendas?eventoId="+evento+"&tamanho=1&pagina=2",null,cc)).size());
        for(String id:List.of("0","-1","2147483648","abc")) assertEquals(400,pedir("GET","/api/me/ingressos/"+id,null,cc).statusCode());
        assertEquals(404,pedir("GET","/api/revendas/2147483647",null,cc).statusCode());assertEquals(400,pedir("POST","/api/compras?valor=1",pedido(1),cc).statusCode());
        assertEquals(400,pedir("POST",anuncio(a,"compra"),"",cc).statusCode());assertEquals(400,pedir("POST",anuncio(a,"cancelamento"),"[]",cv).statusCode());
        for(String path:List.of("/ingressos.html","/js/ingressos.js","/css/ingressos.css")) assertEquals(200,pedir("GET",path,null,null).statusCode());
        var js=pedir("GET","/js/ingressos.js",null,null).body();assertFalse(js.contains("innerHTML"));assertFalse(js.contains("localStorage"));
    }
}
