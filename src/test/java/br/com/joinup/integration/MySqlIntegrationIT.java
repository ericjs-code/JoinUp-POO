package br.com.joinup.integration;

import br.com.joinup.auth.*;
import br.com.joinup.bootstrap.ApplicationBootstrap;
import br.com.joinup.config.ConfiguracaoSistema;
import br.com.joinup.http.*;
import br.com.joinup.shared.db.*;
import br.com.joinup.usuarios.domain.*;
import br.com.joinup.usuarios.dto.*;
import br.com.joinup.usuarios.repository.*;
import br.com.joinup.usuarios.service.*;
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
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in explícito: instância MySQL isolada, nunca mocks, H2 ou banco legado. */
class MySqlIntegrationIT {
    private static final JsonCodec JSON = new JsonCodec();
    private static final AtomicLong DOCUMENTO = new AtomicLong(10000000000L);
    private static final String SENHA = "JoinUpDemo#2026";
    private static final Map<String, Integer> CONTAGENS = Map.ofEntries(
            Map.entry("artistas",10), Map.entry("organizadores",3), Map.entry("generos_musicais",15), Map.entry("participantes",4),
            Map.entry("usuarios_suporte",2), Map.entry("eventos",4), Map.entry("line_up",6), Map.entry("tipos_ingressos",6),
            Map.entry("comunidades",4), Map.entry("compras",6), Map.entry("compra_itens",6), Map.entry("ingressos",7),
            Map.entry("revenda_anuncios",4), Map.entry("participante_comunidades",5), Map.entry("comunidade_mensagens",3),
            Map.entry("postagens",2), Map.entry("comentarios",1), Map.entry("postagem_curtidas",1), Map.entry("ticket",3),
            Map.entry("ticket_mensagens",5), Map.entry("sessoes",0), Map.entry("auditoria_reativacoes",0));
    private static final Map<String, String> PKS = Map.ofEntries(Map.entry("participantes","id_participante"),
            Map.entry("organizadores","id_organizador"), Map.entry("usuarios_suporte","id_usuario_suporte"),
            Map.entry("sessoes","id_sessao"), Map.entry("auditoria_reativacoes","id_auditoria_reativacao"),
            Map.entry("revenda_anuncios","id_revenda_anuncios"), Map.entry("ingressos","id_ingresso"), Map.entry("compras","id_compra"),
            Map.entry("eventos","id_evento"), Map.entry("comunidades","id_comunidade"), Map.entry("tipos_ingressos","id_tipos_ingressos"));
    private static final Map<String, Long> LIMITES = new HashMap<>();
    private static final List<String> EVIDENCIA = new ArrayList<>();
    private static ConfiguracaoSistema config;
    private static ApplicationBootstrap app;
    private static HttpClient client;
    private static String base;
    private static SenhasPbkdf2 senhas;
    private static ExecutorTransacional transacoes;
    private static ParticipanteRepositoryJdbc participantes;
    private static OrganizadorRepositoryJdbc organizadores;
    private static SuporteRepositoryJdbc suportes;
    private static GerenciadorSessao sessoes;
    private static String schema;

    @BeforeAll static void prepararBancoReal() throws Exception {
        assertEquals("instancia-isolada", System.getenv("JOINUP_IT_DATABASE"), "Configure JOINUP_IT_DATABASE=instancia-isolada; suíte aplica fixtures e triggers exclusivamente de teste");
        config = ConfiguracaoSistema.getInstancia();
        ConfiguracaoSistema.validarUrlJdbc(config.getUrlJdbc());
        schema = recurso("/db/schema_v0.2.sql");
        try (var c = admin()) {
            String version = escalar(c, "SELECT VERSION()");
            assertTrue(version.matches("8\\.(?:[1-9][0-9]*\\..*|0\\.(?:1[6-9]|[2-9][0-9]|[1-9][0-9]{2,}).*)"), "Exige MySQL 8.0.16+: " + version);
            assertEquals("joinup_poo", escalar(c, "SELECT DATABASE()"));
            EVIDENCIA.add("MySQL=" + version + ", DATABASE=joinup_poo, UTC=" + escalar(c,"SELECT @@session.time_zone"));
            aplicar(c, schema, Set.of(1007,1050,1681));
            long antes = numero(c,"SELECT COUNT(*) FROM participantes");
            aplicar(c, recurso("/db/seed_demo_v0.2.sql"), antes == 0 ? Set.of() : Set.of(1062));
            for (var par : CONTAGENS.entrySet()) assertEquals(par.getValue().longValue(), numero(c,"SELECT COUNT(*) FROM " + par.getKey()), par.getKey());
            Map<String, List<List<String>>> snapshot = snapshot(c);
            aplicar(c, schema, Set.of(1007,1050,1681));
            aplicar(c, recurso("/db/seed_demo_v0.2.sql"), Set.of(1062));
            assertEquals(snapshot, snapshot(c), "Reexecução precisa preservar linhas, timestamps e hashes, não só contagens");
            EVIDENCIA.add("22 tabelas; seed/reexecução com linhas integralmente iguais; contagens=" + new TreeMap<>(CONTAGENS));
            for (var par : PKS.entrySet()) LIMITES.put(par.getKey(), numero(c,"SELECT COALESCE(MAX(" + par.getValue() + "),0) FROM " + par.getKey()));
            // Conta usada pelo HTTP tem apenas DML; administração de fixtures/DDL é separada.
            assertEquals("joinup_it", config.getUsuarioBanco(), "Suíte requer usuário DML exclusivo joinup_it");
            try (var st = c.createStatement(); var rs = st.executeQuery("SELECT host FROM mysql.user WHERE user='joinup_it' AND host IN('%','127.0.0.1')")) {
                var hosts = new ArrayList<String>(); while (rs.next()) hosts.add(rs.getString(1));
                assertFalse(hosts.isEmpty(), "Provisionar conta de teste joinup_it");
                for (String host : hosts) try (var grants = c.createStatement()) {
                    grants.execute("REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'joinup_it'@'" + host + "'");
                    grants.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON joinup_poo.* TO 'joinup_it'@'" + host + "'");
                }
            }
        }
        participantes = new ParticipanteRepositoryJdbc(); organizadores = new OrganizadorRepositoryJdbc(); suportes = new SuporteRepositoryJdbc();
        var repos = new RepositoriosUsuarios(participantes, organizadores, suportes);
        transacoes = new ExecutorTransacional(new ConnectionFactory(config)); senhas = new SenhasPbkdf2(600000);
        sessoes = new GerenciadorSessao(new SessaoRepositoryJdbc(), repos, 28800);
        app = new ApplicationBootstrap(config, new InetSocketAddress("127.0.0.1",0)); app.iniciar();
        base = "http://127.0.0.1:" + app.getPortaHttp(); client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }
    @AfterAll static void encerrar() throws Exception {
        if (client != null) client.close(); if (app != null) app.close();
        if (!LIMITES.isEmpty()) try (var c = admin()) {
            // Limpeza somente das PKs criadas pela suíte nesta instância explicitamente isolada.
            for (String tabela : List.of("auditoria_reativacoes","sessoes","revenda_anuncios","ingressos","compras","comunidades","tipos_ingressos","eventos","participantes","organizadores","usuarios_suporte"))
                try (var ps = c.prepareStatement("DELETE FROM " + tabela + " WHERE " + PKS.get(tabela) + ">?")) { ps.setLong(1, LIMITES.get(tabela)); ps.executeUpdate(); }
            for (var par : CONTAGENS.entrySet()) assertEquals(par.getValue().longValue(), numero(c,"SELECT COUNT(*) FROM " + par.getKey()), "Limpeza " + par.getKey());
        }
        Files.createDirectories(Path.of("target/mysql-validation"));
        Files.write(Path.of("target/mysql-validation/integracao.txt"), EVIDENCIA, StandardCharsets.UTF_8);
    }
    private static Connection admin() throws SQLException {
        String usuario = System.getenv("JOINUP_IT_ADMIN_USER"), senha = System.getenv("JOINUP_IT_ADMIN_PASSWORD");
        return DriverManager.getConnection(config.getUrlJdbc(), usuario == null ? config.getUsuarioBanco() : usuario, senha == null ? config.getSenhaBanco() : senha);
    }
    private static String recurso(String path) throws Exception {
        try (var stream = MySqlIntegrationIT.class.getResourceAsStream(path)) { return new String(Objects.requireNonNull(stream).readAllBytes(), StandardCharsets.UTF_8); }
    }
    private static void aplicar(Connection c, String script, Set<Integer> warningsPermitidos) throws SQLException {
        // Fixtures do projeto: sem procedures, delimitadores ou ';' dentro dos valores.
        String sql = script.replaceAll("(?m)^\\s*--.*$", "");
        int warnings = 0;
        for (String trecho : sql.split(";")) {
            if (trecho.isBlank()) continue;
            try (var st = c.createStatement()) {
                st.execute(trecho);
                for (SQLWarning w = st.getWarnings(); w != null; w = w.getNextWarning()) {
                    assertTrue(warningsPermitidos.contains(w.getErrorCode()), "Warning SQL inesperado: " + w.getErrorCode()); warnings++;
                }
            }
        }
        EVIDENCIA.add("SQL aplicado: " + (script.contains("CREATE TABLE") ? "schema" : "seed") + ", warnings=" + warnings + ", códigos permitidos=" + warningsPermitidos);
    }
    private static Map<String, List<List<String>>> snapshot(Connection c) throws SQLException {
        var resultado = new TreeMap<String, List<List<String>>>();
        for (String tabela : CONTAGENS.keySet()) {
            try (var st = c.createStatement(); var rs = st.executeQuery("SELECT * FROM " + tabela)) {
                var linhas = new ArrayList<List<String>>();
                while (rs.next()) {
                    var campos = new ArrayList<String>();
                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) campos.add(rs.getString(i));
                    linhas.add(campos);
                }
                linhas.sort(Comparator.comparing(Object::toString)); resultado.put(tabela, linhas);
            }
        }
        return resultado;
    }
    private static String escalar(Connection c, String sql) throws SQLException { try (var st = c.createStatement(); var rs = st.executeQuery(sql)) { rs.next(); return rs.getString(1); } }
    private static long numero(Connection c, String sql) throws SQLException { return Long.parseLong(escalar(c,sql)); }
    private static void executar(String sql, Object... args) throws SQLException {
        try (var c = admin(); var ps = c.prepareStatement(sql)) { for (int i=0; i<args.length;i++) ps.setObject(i+1,args[i]); ps.executeUpdate(); }
    }
    private static long valor(String sql) throws SQLException { try (var c = admin()) { return numero(c, sql); } }
    private static HttpResponse<String> pedir(String metodo, String path, Object corpo, String cookie, String origin) throws Exception {
        String texto = corpo == null ? "" : corpo instanceof String s ? s : new String(JSON.escrever(corpo), StandardCharsets.UTF_8);
        var b = HttpRequest.newBuilder(URI.create(base+path)).timeout(Duration.ofSeconds(20))
                .header("Content-Type","application/json").method(metodo,HttpRequest.BodyPublishers.ofString(texto));
        if (cookie != null) b.header("Cookie", cookie);
        if (origin != null) b.header("Origin", origin.equals("self") ? base : origin);
        return client.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
    private static HttpResponse<String> pedir(String metodo,String path,Object body,String cookie) throws Exception { return pedir(metodo,path,body,cookie,"self"); }
    private static Map<?,?> dados(HttpResponse<String> r) { return (Map<?,?>) JSON.ler(r.body().getBytes(StandardCharsets.UTF_8), Map.class).get("dados"); }
    private static long id(HttpResponse<String> r) { return ((Number)dados(r).get("id")).longValue(); }
    private static String cookie(HttpResponse<String> r) { return r.headers().firstValue("Set-Cookie").orElseThrow().split(";",2)[0]; }
    private static String token(String cookie) { return cookie.substring(cookie.indexOf('=')+1); }
    private static String login(String tipo, String email) throws Exception {
        var r = pedir("POST","/api/auth/"+tipo+"/login",new Credenciais(email,SENHA),null); assertEquals(200,r.statusCode(),r.body()); return cookie(r);
    }
    private static CadastroDados cadastro(String tipo) {
        long n=DOCUMENTO.incrementAndGet();
        return new CadastroDados("Usuário teste", "u"+n+"@joinup.test", SENHA,
                tipo.equals("participantes") ? Long.toString(n) : null,
                tipo.equals("organizadores") ? "000"+n : null,
                tipo.equals("participantes") ? LocalDate.of(1990,2,10) : null);
    }
    private static HttpResponse<String> cadastrar(String tipo, CadastroDados d) throws Exception {
        var r=pedir("POST","/api/"+tipo,d,null); assertEquals(201,r.statusCode(),r.body()); return r;
    }
    private static long anuncio(long participante) throws SQLException {
        return anuncio(participante,1,1);
    }
    private static long anuncio(long participante,long evento,long tipo) throws SQLException {
        try (var c=admin()) {
            long compra=inserir(c,"INSERT INTO compras(id_participante,id_evento,valor_total) VALUES(?,?,100)",participante,evento);
            long ingresso=inserir(c,"INSERT INTO ingressos(data_compra,status,id_participante,id_evento,id_tipo_ingresso,id_compra) VALUES(CURRENT_DATE,'ativo',?,?,?,?)",participante,evento,tipo,compra);
            return inserir(c,"INSERT INTO revenda_anuncios(valor_revenda,status,id_ingresso,id_participante) VALUES(120,'disponivel',?,?)",ingresso,participante);
        }
    }
    private static long inserir(Connection c,String sql,Object...args) throws SQLException {
        try(var ps=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            for(int i=0;i<args.length;i++) ps.setObject(i+1,args[i]); ps.executeUpdate();
            try(var rs=ps.getGeneratedKeys()){rs.next();return rs.getLong(1);}
        }
    }

    @Test void schema22TabelasConstraintsIndicesETiposReais() throws Exception {
        try(var c=admin()) {
            assertEquals(22,numero(c,"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='joinup_poo'"));
            var fkMatcher=Pattern.compile("CONSTRAINT `([^`]+)`\\s+FOREIGN KEY").matcher(schema); int fks=0;
            while(fkMatcher.find()) { assertEquals(1,numero(c,"SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema='joinup_poo' AND constraint_type='FOREIGN KEY' AND constraint_name='"+fkMatcher.group(1)+"'")); fks++; }
            var ckMatcher=Pattern.compile("CONSTRAINT `([^`]+)` CHECK").matcher(schema); int checks=0;
            while(ckMatcher.find()) { assertEquals("YES",escalar(c,"SELECT enforced FROM information_schema.table_constraints WHERE constraint_schema='joinup_poo' AND constraint_name='"+ckMatcher.group(1)+"'")); checks++; }
            assertEquals(42,fks); assertEquals(15,checks);
            var idx=Pattern.compile("(?:UNIQUE )?INDEX `([^`]+)`").matcher(schema); int indices=0;
            while(idx.find()) { assertTrue(numero(c,"SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema='joinup_poo' AND index_name='"+idx.group(1)+"'")>0); indices++; }
            assertEquals(0,numero(c,"SELECT non_unique FROM information_schema.statistics WHERE table_schema='joinup_poo' AND index_name='uk_revenda_ingresso_disponivel'"));
            assertEquals("STORED GENERATED",escalar(c,"SELECT extra FROM information_schema.columns WHERE table_schema='joinup_poo' AND table_name='revenda_anuncios' AND column_name='id_ingresso_disponivel'"));
            assertEquals("enum('ativo','cancelado','utilizado')",escalar(c,"SELECT column_type FROM information_schema.columns WHERE table_schema='joinup_poo' AND table_name='ingressos' AND column_name='status'"));
            assertEquals("decimal(8,2)",escalar(c,"SELECT column_type FROM information_schema.columns WHERE table_schema='joinup_poo' AND table_name='revenda_anuncios' AND column_name='valor_revenda'"));
            EVIDENCIA.add("Inventário: FKs="+fks+", CHECKs efetivos="+checks+", índices nomeados="+indices+"; UNIQUE gerado e tipos confirmados");
        }
    }
    @Test void constraintsRejeitamDadosInvalidosNoMySql() throws Exception {
        try(var c=admin()) { c.setAutoCommit(false);
            try {
                assertEquals(3819,assertThrows(SQLException.class,()->c.createStatement().executeUpdate("UPDATE participantes SET ativo=2 WHERE id_participante=1")).getErrorCode());
                assertEquals(3819,assertThrows(SQLException.class,()->c.createStatement().executeUpdate("INSERT INTO sessoes(hash_token,expira_em) VALUES(REPEAT('a',64),UTC_TIMESTAMP())")).getErrorCode());
                assertEquals(1452,assertThrows(SQLException.class,()->c.createStatement().executeUpdate("INSERT INTO sessoes(hash_token,id_participante,expira_em) VALUES(REPEAT('b',64),2147483647,UTC_TIMESTAMP())")).getErrorCode());
                assertEquals(1062,assertThrows(SQLException.class,()->c.createStatement().executeUpdate("INSERT INTO revenda_anuncios(valor_revenda,status,id_ingresso,id_participante) VALUES(120,'disponivel',1,1)")).getErrorCode());
                assertEquals(3819,assertThrows(SQLException.class,()->c.createStatement().executeUpdate("UPDATE comunidade_mensagens SET id_participante=NULL WHERE id_mensagem=1")).getErrorCode());
                assertEquals(1451,assertThrows(SQLException.class,()->c.createStatement().executeUpdate("DELETE FROM usuarios_suporte WHERE id_usuario_suporte=2")).getErrorCode());
            } finally { c.rollback(); }
        }
    }
    @Test void noveHashesDoSeedPersistidosEVerificadosPeloServico() throws Exception {
        int quantidade=0;
        try(var c=admin()) {
            for(Perfil p:Perfil.values()) try(var st=c.createStatement();var rs=st.executeQuery("SELECT senha FROM "+p.tabela()+" WHERE "+p.colunaId()+"<="+LIMITES.get(p.tabela()))) {
                while(rs.next()){assertTrue(senhas.verificar(SENHA,rs.getString(1)));assertFalse(senhas.verificar("incorreta",rs.getString(1))); quantidade++;}
            }
        }
        assertEquals(9,quantidade); EVIDENCIA.add("9 hashes PBKDF2 do seed lidos do MySQL: senha correta aceita e errada rejeitada");
    }
    @Test void factoryMethodTresProdutosPersistidosESuporteRevalidaAutorizacao() throws Exception {
        var p=new CadastroParticipanteCreator(transacoes,senhas,participantes).cadastrar(cadastro("participantes"),null);
        var o=new CadastroOrganizadorCreator(transacoes,senhas,organizadores).cadastrar(cadastro("organizadores"),null);
        var creator=new CadastroSuporteCreator(transacoes,senhas,suportes,sessoes);
        assertEquals(401,assertThrows(HttpException.class,()->creator.cadastrar(cadastro("suporte"),null)).status());
        String cp=login("participantes",p.email());
        assertEquals(403,assertThrows(HttpException.class,()->creator.cadastrar(cadastro("suporte"),token(cp))).status());
        var s=creator.cadastrar(cadastro("suporte"),token(login("suporte","suporte1.demo@joinup.test")));
        assertInstanceOf(Participante.class,p);assertInstanceOf(Organizador.class,o);assertInstanceOf(UsuarioSuporte.class,s);
        try(var c=admin()){assertInstanceOf(Participante.class,participantes.buscarId(c,p.id(),false));assertInstanceOf(Organizador.class,organizadores.buscarId(c,o.id(),false));assertInstanceOf(UsuarioSuporte.class,suportes.buscarId(c,s.id(),false));}
        login("suporte",s.email());
    }
    @ParameterizedTest @ValueSource(strings={"participantes","organizadores","suporte"})
    void loginCorretoErradoIdentidadeCookieEHashPersistido(String tipo) throws Exception {
        String email=Map.of("participantes","ana.demo@joinup.test","organizadores","aurora.demo@joinup.test","suporte","suporte1.demo@joinup.test").get(tipo);
        var resposta=pedir("POST","/api/auth/"+tipo+"/login",new Credenciais(" "+email.toUpperCase(Locale.ROOT)+" ",SENHA),null);
        assertEquals(200,resposta.statusCode(),resposta.body());
        String cookie=cookie(resposta), token=token(cookie), hash=GerenciadorSessao.hashToken(token);
        assertEquals(43,token.length()); String completo=resposta.headers().firstValue("Set-Cookie").orElseThrow();
        assertTrue(completo.contains("HttpOnly"));assertTrue(completo.contains("SameSite=Lax"));assertTrue(completo.contains("Path=/"));assertFalse(completo.contains("; Secure"));
        assertFalse(resposta.body().contains(token));assertFalse(resposta.body().contains("senha"));
        assertEquals(1,valor("SELECT COUNT(*) FROM sessoes WHERE hash_token='"+hash+"'"));
        assertEquals(0,valor("SELECT COUNT(*) FROM sessoes WHERE hash_token='"+token+"'"));
        var me=pedir("GET","/api/auth/me",null,cookie);assertEquals(200,me.statusCode());
        assertEquals(Map.of("participantes","PARTICIPANTE","organizadores","ORGANIZADOR","suporte","SUPORTE").get(tipo),dados(me).get("perfil"));
        assertEquals(401,pedir("POST","/api/auth/"+tipo+"/login",new Credenciais(email,"errada"),null).statusCode());
        assertEquals(401,pedir("POST","/api/auth/"+tipo+"/login",new Credenciais("ausente@joinup.test",SENHA),null).statusCode());
        String outro=login(tipo,email);assertNotEquals(cookie,outro);
    }
    @Test void templateMethodSequenciaRealInterrompeAntesDaSessao() throws Exception {
        var p=new CadastroParticipanteCreator(transacoes,senhas,participantes).cadastrar(cadastro("participantes"),null);
        var etapas=new ArrayList<String>();
        VerificadorSenha v=(senha,hash)->{etapas.add("senha");return senhas.verificar(senha,hash);};
        var t=new AutenticacaoTemplate(transacoes,v,sessoes){
            @Override protected Usuario buscarUsuario(Connection c,String email)throws SQLException{etapas.add("busca:"+email);return participantes.buscarEmail(c,email,true);}
            @Override protected void validarRegraDoPerfil(Usuario u){etapas.add("regra");}
        };
        var sessao=t.autenticar(new Credenciais(" "+p.email().toUpperCase(Locale.ROOT)+" ",SENHA));
        assertEquals(List.of("busca:"+p.email(),"senha","regra"),etapas);
        assertEquals(1,valor("SELECT COUNT(*) FROM sessoes WHERE hash_token='"+GerenciadorSessao.hashToken(sessao.token())+"'"));
        etapas.clear();assertThrows(HttpException.class,()->t.autenticar(new Credenciais(p.email(),"errada")));
        assertEquals(List.of("busca:"+p.email(),"senha"),etapas);
        executar("UPDATE participantes SET ativo=0 WHERE id_participante=?",p.id());
        etapas.clear();assertEquals(403,assertThrows(HttpException.class,()->t.autenticar(new Credenciais(p.email(),SENHA))).status());
        assertEquals(List.of("busca:"+p.email(),"senha"),etapas);
        assertEquals(1,valor("SELECT COUNT(*) FROM sessoes WHERE id_participante="+p.id()));
        assertEquals(403,pedir("GET","/api/auth/me",null,"joinup_sessao="+sessao.token()).statusCode());
    }
    @ParameterizedTest @ValueSource(strings={"participantes","organizadores"})
    void contaInativaNaoAutentica(String tipo)throws Exception {
        String email=tipo.equals("participantes")?"inativo.demo@joinup.test":"inativa.demo@joinup.test";
        assertEquals(403,pedir("POST","/api/auth/"+tipo+"/login",new Credenciais(email,SENHA),null).statusCode());
        assertEquals(401,pedir("POST","/api/auth/"+tipo+"/login",new Credenciais(email,"errada"),null).statusCode());
    }
    @Test void suporte401403CadastroESegundoSuportePodeCadastrar()throws Exception {
        CadastroDados d=cadastro("suporte");
        assertEquals(401,pedir("POST","/api/suporte/usuarios",d,null).statusCode());
        for(String cookie:List.of(login("participantes","ana.demo@joinup.test"),login("organizadores","aurora.demo@joinup.test")))
            assertEquals(403,pedir("POST","/api/suporte/usuarios",d,cookie).statusCode());
        String s=login("suporte","suporte2.demo@joinup.test");
        assertEquals(201,pedir("POST","/api/suporte/usuarios",d,s).statusCode());
        String novo=login("suporte",d.email());
        assertEquals(201,pedir("POST","/api/suporte/usuarios",cadastro("suporte"),novo).statusCode());
        assertEquals(403,pedir("GET","/api/me/perfil",null,s).statusCode());
        assertEquals(403,pedir("POST","/api/me/desativacao",null,s).statusCode());
    }
    @Test void cadastroDuplicadoComprimentosFormatosESqlInjection()throws Exception {
        var d=cadastro("participantes");cadastrar("participantes",d);
        assertEquals(409,pedir("POST","/api/participantes",d,null).statusCode());
        var d2=cadastro("participantes");
        assertEquals(409,pedir("POST","/api/participantes",new CadastroDados(d2.nome(),d2.email(),SENHA,d.cpf(),null,d2.data_nascimento()),null).statusCode());
        var o=cadastro("organizadores");cadastrar("organizadores",o);
        var o2=cadastro("organizadores");
        assertEquals(409,pedir("POST","/api/organizadores",new CadastroDados(o2.nome(),o2.email(),SENHA,null,o.cnpj(),null),null).statusCode());
        for(CadastroDados invalido:List.of(new CadastroDados("n".repeat(61),d2.email(),SENHA,d2.cpf(),null,d2.data_nascimento()),
                new CadastroDados("nome","e".repeat(34)+"@joinup.test",SENHA,d2.cpf(),null,d2.data_nascimento()),
                new CadastroDados("nome",d2.email(),SENHA,"a",null,d2.data_nascimento())))
            assertEquals(422,pedir("POST","/api/participantes",invalido,null).statusCode());
        assertEquals(400,pedir("POST","/api/participantes","{\"id\":1}",null).statusCode());
        var inj=cadastro("participantes");var ri=cadastrar("participantes",new CadastroDados("Robert'); DROP TABLE participantes;--",inj.email(),SENHA,inj.cpf(),null,inj.data_nascimento()));
        assertTrue(id(ri)>0);assertEquals(22,valor("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='joinup_poo'"));
    }
    @Test void sessaoExpiradaRevogadaLogoutEReuso()throws Exception {
        var d=cadastro("participantes");cadastrar("participantes",d);String s=login("participantes",d.email());
        assertEquals(204,pedir("POST","/api/auth/logout",null,s).statusCode());
        assertEquals(401,pedir("GET","/api/auth/me",null,s).statusCode());assertEquals(401,pedir("POST","/api/auth/logout",null,s).statusCode());
        s=login("participantes",d.email());executar("UPDATE sessoes SET expira_em=UTC_TIMESTAMP() WHERE hash_token=?",GerenciadorSessao.hashToken(token(s)));
        assertEquals(401,pedir("GET","/api/me/perfil",null,s).statusCode());
        s=login("participantes",d.email());executar("UPDATE sessoes SET revogada_em=UTC_TIMESTAMP() WHERE hash_token=?",GerenciadorSessao.hashToken(token(s)));
        assertEquals(401,pedir("GET","/api/auth/me",null,s).statusCode());
        assertEquals(401,pedir("GET","/api/auth/me",null,"joinup_sessao="+"x".repeat(43)).statusCode());
        assertEquals(401,pedir("GET","/api/auth/me",null,null).statusCode());
    }
    @Test void perfilProprioIdorEAtualizacaoDosCamposHerdados()throws Exception {
        var a=cadastro("participantes");var b=cadastro("participantes");long ia=id(cadastrar("participantes",a)),ib=id(cadastrar("participantes",b));
        String sa=login("participantes",a.email()),sb=login("participantes",b.email());
        assertEquals(ia,id(pedir("GET","/api/me/perfil",null,sa)));assertEquals(ib,id(pedir("GET","/api/me/perfil",null,sb)));
        for(String campo:List.of("id","id_participante","cpf","perfil"))
            assertEquals(400,pedir("PATCH","/api/me/perfil","{\""+campo+"\":\""+ib+"\",\"nome\":\"Ataque\"}",sa).statusCode());
        assertEquals(404,pedir("PATCH","/api/me/perfil/"+ib,Map.of("nome","Ataque"),sa).statusCode());
        assertEquals(400,pedir("POST","/api/me/desativacao",Map.of("id",ib),sa).statusCode());
        var edit=pedir("PATCH","/api/me/perfil",new PerfilDados("Nome editado",null,"nova senha",null),sa);assertEquals(200,edit.statusCode());
        assertEquals("Nome editado",dados(edit).get("nome"));assertEquals(a.cpf(),dados(edit).get("cpf"));assertEquals("1990-02-10",dados(edit).get("data_nascimento"));
        assertEquals(401,pedir("POST","/api/auth/participantes/login",new Credenciais(a.email(),SENHA),null).statusCode());
        assertEquals(200,pedir("POST","/api/auth/participantes/login",new Credenciais(a.email(),"nova senha"),null).statusCode());
        assertEquals(b.nome(),dados(pedir("GET","/api/me/perfil",null,sb)).get("nome"));
        assertEquals(409,pedir("PATCH","/api/me/perfil",Map.of("email",b.email()),sa).statusCode());
        var o=cadastro("organizadores");long io=id(cadastrar("organizadores",o));String so=login("organizadores",o.email());
        String novo="000"+DOCUMENTO.incrementAndGet();assertEquals(200,pedir("PATCH","/api/me/perfil",Map.of("cnpj",novo),so).statusCode());
        assertEquals(io,id(pedir("GET","/api/me/perfil",null,so)));assertEquals(novo,dados(pedir("GET","/api/me/perfil",null,so)).get("cnpj"));
    }
    @Test void csrfNegadoEmTodasAsMutacoesIncluindoLoginECadastro()throws Exception {
        var d=cadastro("participantes");long id=id(cadastrar("participantes",d));String p=login("participantes",d.email()),s=login("suporte","suporte1.demo@joinup.test");
        String[] paths={"/api/participantes","/api/organizadores","/api/auth/participantes/login","/api/auth/organizadores/login","/api/auth/suporte/login","/api/me/perfil","/api/me/desativacao","/api/auth/logout","/api/suporte/usuarios","/api/suporte/contas/participante/"+id+"/reativacao"};
        for(String path:paths)for(String origin:new String[]{null,"https://evil.test","null"}) {
            String metodo=path.equals("/api/me/perfil")?"PATCH":"POST";
            String ck=path.startsWith("/api/suporte")?s:p;
            assertEquals(403,pedir(metodo,path,Map.of(),ck,origin).statusCode(),path);
        }
        assertEquals(200,pedir("GET","/api/me/perfil",null,p).statusCode());
    }
    @Test void desativacaoCancelaTodosAnunciosRevogaTodasSessoesPreservaHistoricoEReativacaoAudita()throws Exception {
        var d=cadastro("participantes");long id=id(cadastrar("participantes",d));long ad1=anuncio(id),ad2=anuncio(id),ad3=anuncio(id);
        executar("UPDATE revenda_anuncios SET status='vendido' WHERE id_revenda_anuncios=?",ad3);
        String a=login("participantes",d.email()),b=login("participantes",d.email());
        assertEquals(200,pedir("POST","/api/me/desativacao",null,a).statusCode());
        assertEquals(0,valor("SELECT ativo FROM participantes WHERE id_participante="+id));
        assertEquals(2,valor("SELECT COUNT(*) FROM revenda_anuncios WHERE id_participante="+id+" AND status='cancelado' AND data_cancelamento IS NOT NULL"));
        assertEquals(1,valor("SELECT COUNT(*) FROM revenda_anuncios WHERE id_revenda_anuncios="+ad3+" AND status='vendido'"));
        assertEquals(3,valor("SELECT COUNT(*) FROM ingressos WHERE id_participante="+id));assertEquals(3,valor("SELECT COUNT(*) FROM compras WHERE id_participante="+id));
        assertEquals(2,valor("SELECT COUNT(*) FROM sessoes WHERE id_participante="+id+" AND revogada_em IS NOT NULL"));
        assertEquals(401,pedir("GET","/api/me/perfil",null,a).statusCode());assertEquals(401,pedir("GET","/api/auth/me",null,b).statusCode());
        assertEquals(403,pedir("POST","/api/auth/participantes/login",new Credenciais(d.email(),SENHA),null).statusCode());
        String suporte=login("suporte","suporte2.demo@joinup.test");
        var list=pedir("GET","/api/suporte/contas?tipo=participante&situacao=inativa",null,suporte);assertEquals(200,list.statusCode());assertFalse(list.body().contains("cpf"));assertFalse(list.body().contains("senha"));
        var react=pedir("POST","/api/suporte/contas/participante/"+id+"/reativacao",null,suporte);assertEquals(200,react.statusCode());assertEquals(id,id(react));
        assertEquals(1,valor("SELECT COUNT(*) FROM auditoria_reativacoes WHERE id_participante="+id+" AND id_usuario_suporte=2 AND data_reativacao IS NOT NULL"));
        assertEquals(409,pedir("POST","/api/suporte/contas/participante/"+id+"/reativacao",null,suporte).statusCode());
        assertEquals(2,valor("SELECT COUNT(*) FROM revenda_anuncios WHERE id_revenda_anuncios IN("+ad1+","+ad2+") AND status='cancelado'"));
        assertEquals(401,pedir("GET","/api/auth/me",null,b).statusCode());login("participantes",d.email());
    }
    @Test void organizadorDesativadoMantemEventosComunidadesAnuncios()throws Exception {
        var d=cadastro("organizadores");long id=id(cadastrar("organizadores",d));String s=login("organizadores",d.email());
        long evento,comunidade,tipo;
        try(var c=admin()) {
            evento=inserir(c,"INSERT INTO eventos(nome,data,descricao,localizacao,cidade,estado,cep,id_organizador,id_genero_musical) VALUES('Evento teste',TIMESTAMPADD(DAY,30,UTC_TIMESTAMP()),'Fixture','Arena','Curitiba','PR','80000000',?,1)",id);
            comunidade=inserir(c,"INSERT INTO comunidades(nome,id_evento) VALUES('Comunidade teste',?)",evento);
            tipo=inserir(c,"INSERT INTO tipos_ingressos(nome_tipo,valor,quantidade_disponivel,id_evento) VALUES('Pista',100,10,?)",evento);
        }
        var dp=cadastro("participantes");long ip=id(cadastrar("participantes",dp));long anuncio=anuncio(ip,evento,tipo);
        long eventos=valor("SELECT COUNT(*) FROM eventos"),comunidades=valor("SELECT COUNT(*) FROM comunidades"),anuncios=valor("SELECT COUNT(*) FROM revenda_anuncios");
        assertEquals(200,pedir("POST","/api/me/desativacao",null,s).statusCode());assertEquals(401,pedir("GET","/api/auth/me",null,s).statusCode());
        assertEquals(1,valor("SELECT COUNT(*) FROM eventos WHERE id_evento="+evento+" AND id_organizador="+id));
        assertEquals(1,valor("SELECT COUNT(*) FROM comunidades WHERE id_comunidade="+comunidade+" AND id_evento="+evento));
        assertEquals(1,valor("SELECT COUNT(*) FROM revenda_anuncios WHERE id_revenda_anuncios="+anuncio+" AND status='disponivel'"));
        String suporte=login("suporte","suporte1.demo@joinup.test");
        assertEquals(200,pedir("POST","/api/suporte/contas/organizador/"+id+"/reativacao",null,suporte).statusCode());login("organizadores",d.email());
        assertEquals(eventos,valor("SELECT COUNT(*) FROM eventos"));assertEquals(comunidades,valor("SELECT COUNT(*) FROM comunidades"));assertEquals(anuncios,valor("SELECT COUNT(*) FROM revenda_anuncios"));
    }
    @Test void usuarioAplicacaoNaoPossuiDdlEConexoesSaoIndependentes()throws Exception {
        var factory=new ConnectionFactory(config);
        try(var a=factory.abrirConexao();var b=factory.abrirConexao()) {
            assertNotEquals(escalar(a,"SELECT CONNECTION_ID()"),escalar(b,"SELECT CONNECTION_ID()"));
            try(var st=a.createStatement()) {
                assertEquals(1142,assertThrows(SQLException.class,()->st.execute("CREATE TABLE it_ddl_proibido(id INT)")).getErrorCode());
            }
        }
    }
    @Test void listagemReativacao401403IdsInvalidosETipoSuporte()throws Exception {
        String path="/api/suporte/contas/participante/4/reativacao";
        assertEquals(401,pedir("GET","/api/suporte/contas?tipo=participante&situacao=inativa",null,null).statusCode());assertEquals(401,pedir("POST",path,null,null).statusCode());
        for(String ck:List.of(login("participantes","ana.demo@joinup.test"),login("organizadores","aurora.demo@joinup.test"))) {
            assertEquals(403,pedir("GET","/api/suporte/contas?tipo=participante&situacao=inativa",null,ck).statusCode());assertEquals(403,pedir("POST",path,null,ck).statusCode());
        }
        String s=login("suporte","suporte1.demo@joinup.test");
        for(String id:List.of("0","-1","abc","01","2147483648"))assertEquals(400,pedir("POST","/api/suporte/contas/participante/"+id+"/reativacao",null,s).statusCode());
        assertEquals(400,pedir("POST","/api/suporte/contas/suporte/1/reativacao",null,s).statusCode());
        assertEquals(404,pedir("POST","/api/suporte/contas/participante/2147483647/reativacao",null,s).statusCode());
        assertEquals(405,pedir("GET",path,null,s).statusCode());assertEquals(404,pedir("POST",path+"/extra",null,s).statusCode());
        for(String query:List.of("tipo=suporte&situacao=inativa","tipo=participante&situacao=ativa","tipo=participante&situacao=inativa&pagina=0","tipo=participante&situacao=inativa&tamanho=101"))
            assertEquals(400,pedir("GET","/api/suporte/contas?"+query,null,s).statusCode());
    }
    @Test void falhaIntermediariaDesativacaoProvocaRollbackCompleto()throws Exception {
        var d=cadastro("participantes");long id=id(cadastrar("participantes",d));long ad=anuncio(id);String s=login("participantes",d.email());
        executar("CREATE TRIGGER it_falha_revogacao BEFORE UPDATE ON sessoes FOR EACH ROW BEGIN IF NEW.id_participante="+id+" THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='falha de teste'; END IF; END");
        try {
            assertEquals(503,pedir("POST","/api/me/desativacao",null,s).statusCode());
            assertEquals(1,valor("SELECT ativo FROM participantes WHERE id_participante="+id));
            assertEquals(1,valor("SELECT COUNT(*) FROM revenda_anuncios WHERE id_revenda_anuncios="+ad+" AND status='disponivel' AND data_cancelamento IS NULL"));
            assertEquals(1,valor("SELECT COUNT(*) FROM sessoes WHERE id_participante="+id+" AND revogada_em IS NULL"));
            assertEquals(200,pedir("GET","/api/me/perfil",null,s).statusCode());
        } finally { executar("DROP TRIGGER it_falha_revogacao"); }
    }
    @Test void falhaNaAuditoriaProvocaRollbackDaReativacao()throws Exception {
        var d=cadastro("participantes");long id=id(cadastrar("participantes",d));String s=login("participantes",d.email());
        pedir("POST","/api/me/desativacao",null,s);String suporte=login("suporte","suporte1.demo@joinup.test");
        executar("CREATE TRIGGER it_falha_auditoria BEFORE INSERT ON auditoria_reativacoes FOR EACH ROW BEGIN IF NEW.id_participante="+id+" THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='falha de teste'; END IF; END");
        try {
            assertEquals(503,pedir("POST","/api/suporte/contas/participante/"+id+"/reativacao",null,suporte).statusCode());
            assertEquals(0,valor("SELECT ativo FROM participantes WHERE id_participante="+id));
            assertEquals(0,valor("SELECT COUNT(*) FROM auditoria_reativacoes WHERE id_participante="+id));
        }finally{executar("DROP TRIGGER it_falha_auditoria");}
    }
    @Test void concorrenciaLoginDesativacaoNuncaDeixaSessaoValidaEmContaInativa()throws Exception {
        var d=cadastro("participantes");long id=id(cadastrar("participantes",d));String s=login("participantes",d.email());anuncio(id);
        try(var executor=Executors.newFixedThreadPool(2)) {
            var inicio=new CountDownLatch(1);
            var login=executor.submit(()->{inicio.await();return pedir("POST","/api/auth/participantes/login",new Credenciais(d.email(),SENHA),null);});
            var off=executor.submit(()->{inicio.await();return pedir("POST","/api/me/desativacao",null,s);});inicio.countDown();
            var rl=login.get(30,TimeUnit.SECONDS);assertTrue(Set.of(200,403).contains(rl.statusCode()),rl.body());assertEquals(200,off.get(30,TimeUnit.SECONDS).statusCode());
            assertEquals(0,valor("SELECT COUNT(*) FROM sessoes WHERE id_participante="+id+" AND revogada_em IS NULL"));
            if(rl.statusCode()==200)assertEquals(401,pedir("GET","/api/auth/me",null,cookie(rl)).statusCode());
        }
    }
    @Test void concorrenciaReativacaoProduzUmaAuditoria()throws Exception {
        var d=cadastro("participantes");long id=id(cadastrar("participantes",d));String p=login("participantes",d.email());pedir("POST","/api/me/desativacao",null,p);
        String a=login("suporte","suporte1.demo@joinup.test"),b=login("suporte","suporte2.demo@joinup.test");
        try(var executor=Executors.newFixedThreadPool(2)) {
            var inicio=new CountDownLatch(1);String path="/api/suporte/contas/participante/"+id+"/reativacao";
            var x=executor.submit(()->{inicio.await();return pedir("POST",path,null,a).statusCode();});
            var y=executor.submit(()->{inicio.await();return pedir("POST",path,null,b).statusCode();});inicio.countDown();
            var status=new ArrayList<>(List.of(x.get(30,TimeUnit.SECONDS),y.get(30,TimeUnit.SECONDS)));Collections.sort(status);assertEquals(List.of(200,409),status);
        }
        assertEquals(1,valor("SELECT COUNT(*) FROM auditoria_reativacoes WHERE id_participante="+id));
    }
}
