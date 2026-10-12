package br.com.joinup.integration;

import br.com.joinup.bootstrap.ApplicationBootstrap;
import br.com.joinup.config.ConfiguracaoSistema;
import br.com.joinup.demo.*;
import org.junit.jupiter.api.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/** Segunda instância vazia obrigatória. Processos novos validam o Singleton e bootstrap reais. */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ZDemoMySqlIT {
    static String url;
    @BeforeAll static void preparar() throws Exception {
        assertEquals("instancia-isolada",System.getenv("JOINUP_IT_DATABASE"));
        url=Objects.requireNonNull(System.getenv("JOINUP_DEMO_IT_JDBC_URL"),"Demo IT exige segunda instância isolada");
        ConfiguracaoSistema.validarUrlJdbc(url);
        assertNotEquals(ConfiguracaoSistema.getInstancia().getUrlJdbc(),url);
        try(var c=admin()) {
            assertTrue(escalar(c,"SELECT VERSION()").startsWith("8.4."));
            for(String recurso:List.of("schema_v0.2.sql","migration_06_midia_demo.sql"))try(var in=ZDemoMySqlIT.class.getResourceAsStream("/db/"+recurso)) {
                for(String s:SqlDemo.separar(new String(Objects.requireNonNull(in).readAllBytes(),StandardCharsets.UTF_8)))try(var st=c.createStatement()){st.execute(s);}
            }
            assertEquals("0",escalar(c,"SELECT COUNT(*) FROM participantes"),"Exige segunda instância nova, não apaga dados existentes");
        }
    }
    static Connection admin() throws SQLException{return DriverManager.getConnection(url,System.getenv("JOINUP_IT_ADMIN_USER"),System.getenv("JOINUP_IT_ADMIN_PASSWORD"));}
    static String escalar(Connection c,String sql)throws SQLException{try(var st=c.createStatement();var r=st.executeQuery(sql)){assertTrue(r.next());return r.getString(1);}}
    static void exec(String sql)throws SQLException{try(var c=admin();var s=c.createStatement()){s.executeUpdate(sql);}}
    @Test @Order(1) void falseNaoAbreBancoNemAplicaSeed() throws Exception {
        assertEquals("BOOT_OK",probe(false,"local",true,false));
        try(var c=admin()){assertEquals("0",escalar(c,"SELECT COUNT(*) FROM participantes"));assertEquals("0",escalar(c,"SELECT COUNT(*) FROM demo_execucao"));}
    }
    @Test @Order(2) void trueRecusaBasePopuladaSemProveniencia() throws Exception {
        exec("INSERT INTO generos_musicais(id_genero_musical,nome) VALUES(999,'Base alheia')");
        try{assertEquals("BOOT_RECUSADO",probe(true,"demo",false,false));try(var c=admin()){assertEquals("0",escalar(c,"SELECT COUNT(*) FROM participantes"));assertEquals("0",escalar(c,"SELECT COUNT(*) FROM demo_execucao"));}}
        finally{exec("DELETE FROM generos_musicais WHERE id_genero_musical=999");}
    }
    @Test @Order(3) void erroDuranteSeedReverteTudoEMarcador() throws Exception {
        exec("CREATE TRIGGER it06_demo BEFORE INSERT ON ticket FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture06'");
        try {
            assertEquals("BOOT_RECUSADO",probe(true,"demo",false,false));
            try(var c=admin()){for(String t:DemoRunner.TABELAS)assertEquals("0",escalar(c,"SELECT COUNT(*) FROM "+t),t);assertEquals("0",escalar(c,"SELECT COUNT(*) FROM demo_execucao"));}
        } finally{exec("DROP TRIGGER it06_demo");}
    }
    @Test @Order(4) void trueBootstrapCargaAutomaticaEReinicioIdentico() throws Exception {
        assertEquals("BOOT_OK",probe(true,"demo",false,false));
        Map<String,List<List<String>>> antes;
        try(var c=admin()){assertEquals("4",escalar(c,"SELECT COUNT(*) FROM participantes"));assertEquals("3",escalar(c,"SELECT COUNT(*) FROM organizadores"));assertEquals("2",escalar(c,"SELECT COUNT(*) FROM usuarios_suporte"));assertEquals("seed_v0.2",escalar(c,"SELECT versao FROM demo_execucao"));antes=snapshot(c);}
        assertEquals("BOOT_OK",probe(true,"demo",false,false));
        try(var c=admin()){assertEquals(antes,snapshot(c));}
    }
    @Test @Order(5) void reiniciarPreservaNomeSenhaDesativacaoEDadosRemovidos() throws Exception {
        exec("UPDATE participantes SET nome='Demo editada',senha='hash alterado',ativo=0,data_desativacao=UTC_TIMESTAMP() WHERE id_participante=1");
        exec("UPDATE eventos SET nome='Evento editado' WHERE id_evento=1");exec("DELETE FROM comentarios WHERE id_comentario=1");
        Map<String,List<List<String>>> antes;try(var c=admin()){antes=snapshot(c);}
        assertEquals("BOOT_OK",probe(true,"demo",false,false));try(var c=admin()){assertEquals(antes,snapshot(c));}
        // Restaurar somente fixture alterada para o teste dos três perfis.
        exec("UPDATE participantes p JOIN participantes original ON original.id_participante=2 SET p.senha=original.senha,p.ativo=1,p.data_desativacao=NULL WHERE p.id_participante=1");
    }
    @Test @Order(6) void perfilProdOuLocalSemMarcadorDeAmbienteRecusados() throws Exception {
        assertEquals("BOOT_RECUSADO",probe(true,"production",false,false));assertEquals("BOOT_RECUSADO",probe(true,"local",false,false));
    }
    @Test @Order(7) void tresPerfisAutenticamComCargaDoBootstrapReal() throws Exception {
        assertEquals("LOGIN_OK_3",probe(true,"demo",false,true));
    }
    @Test @Order(8) void dmlNaoPodeMigrarSchema() throws Exception {
        try(var c=DriverManager.getConnection(url,System.getenv("JOINUP_DB_USER"),System.getenv("JOINUP_DEMO_IT_DB_PASSWORD"));var st=c.createStatement()) {
            assertThrows(SQLException.class,()->st.execute("CREATE TABLE indevida06(id INT)"));
            assertThrows(SQLException.class,()->st.execute("ALTER TABLE participantes ADD COLUMN indevida06 INT"));
        }
    }
    static Map<String,List<List<String>>> snapshot(Connection c)throws SQLException {
        var m=new TreeMap<String,List<List<String>>>();var tabelas=new ArrayList<>(DemoRunner.TABELAS);tabelas.add("demo_execucao");
        for(String t:tabelas)try(var st=c.createStatement();var rs=st.executeQuery("SELECT * FROM "+t)) {
            var linhas=new ArrayList<List<String>>();while(rs.next()){var l=new ArrayList<String>();for(int i=1;i<=rs.getMetaData().getColumnCount();i++)l.add(rs.getString(i));linhas.add(l);}
            linhas.sort(Comparator.comparing(Object::toString));m.put(t,linhas);
        }
        return m;
    }
    static String probe(boolean demo,String ambiente,boolean bancoIndisponivel,boolean login)throws Exception {
        Path java=Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"java.exe":"java");
        var b=new ProcessBuilder(java.toString(),"-cp",System.getProperty("java.class.path"),DemoProbe.class.getName()).redirectErrorStream(true);
        var e=b.environment();e.remove("JAVA_TOOL_OPTIONS");e.remove("JDK_JAVA_OPTIONS");e.remove("_JAVA_OPTIONS");
        e.put("JOINUP_DEMO",Boolean.toString(demo));e.put("JOINUP_ENV",ambiente);e.put("JOINUP_HTTP_HOST","127.0.0.1");e.put("JOINUP_PUBLIC_ORIGIN","");
        e.put("JOINUP_JDBC_URL",bancoIndisponivel?"jdbc:mysql://127.0.0.1:1/joinup_poo?connectTimeout=1000":url);
        e.put("JOINUP_DB_PASSWORD",System.getenv("JOINUP_DEMO_IT_DB_PASSWORD"));e.put("JOINUP_TEST_LOGIN06",Boolean.toString(login));
        var p=b.start();try{assertTrue(p.waitFor(40,TimeUnit.SECONDS));assertEquals(0,p.exitValue());return new String(p.getInputStream().readAllBytes(),StandardCharsets.UTF_8).trim();}finally{p.destroyForcibly();}
    }
    public static class DemoProbe {
        public static void main(String[] args)throws Exception {
            try(var app=new ApplicationBootstrap(ConfiguracaoSistema.getInstancia(),new InetSocketAddress("127.0.0.1",0))) {
                app.iniciar();
                if(Boolean.parseBoolean(System.getenv("JOINUP_TEST_LOGIN06"))) {
                    try(var client=HttpClient.newHttpClient()) {
                        String base="http://127.0.0.1:"+app.getPortaHttp();
                        for(var perfil:Map.of("participantes","ana.demo@joinup.test","organizadores","aurora.demo@joinup.test","suporte","suporte1.demo@joinup.test").entrySet()) {
                            var q=HttpRequest.newBuilder(URI.create(base+"/api/auth/"+perfil.getKey()+"/login")).header("Origin",base).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{\"email\":\""+perfil.getValue()+"\",\"senha\":\"JoinUpDemo#2026\"}"));
                            if(client.send(q.build(),HttpResponse.BodyHandlers.discarding()).statusCode()!=200)throw new IllegalStateException("Login fixture falhou");
                        }
                    }
                    System.out.print("LOGIN_OK_3");
                } else System.out.print("BOOT_OK");
            } catch(IllegalStateException ex){System.out.print("BOOT_RECUSADO");}
        }
    }
}
