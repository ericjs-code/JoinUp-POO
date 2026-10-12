package br.com.joinup.integration;

import br.com.joinup.arquivos.*;
import br.com.joinup.demo.SqlDemo;
import br.com.joinup.arquivos.ImagemSeguraTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Executada depois da regressão original de 22 tabelas; adiciona e remove só suas duas tabelas. */
class ZArquivosMySqlIT extends MySql05Fixture {
    private static boolean migracaoCriada;
    @BeforeAll static void migrar() throws Exception {
        assertNotNull(System.getenv("JOINUP_UPLOAD_DIR"),"Configure diretório exclusivo e descartável de mídia IT");
        assertEquals(0,quantidadeArquivos(),"Diretório IT deve começar vazio");
        assertEquals(22,valor("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='joinup_poo'"));
        try(var c=admin();var in=ZArquivosMySqlIT.class.getResourceAsStream("/db/migration_06_midia_demo.sql")) {
            String sql=new String(Objects.requireNonNull(in).readAllBytes(),StandardCharsets.UTF_8);
            for(int i=0;i<2;i++)for(String s:SqlDemo.separar(sql))try(var st=c.createStatement()){st.execute(s);}
            migracaoCriada=true;
        }
        assertEquals(24,valor("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='joinup_poo'"));
        assertEquals(6,valor("SELECT COUNT(*) FROM information_schema.referential_constraints WHERE constraint_schema='joinup_poo' AND table_name='arquivos'"));
        try(var c=app.getConnectionFactory().abrirConexao();var s=c.createStatement()) {assertThrows(SQLException.class,()->s.execute("CREATE TABLE nao_permitida06(id INT)"));}
    }
    @AfterEach void limparMidia() throws Exception {
        for(long e:eventos) {
            var ids=new ArrayList<String>();
            try(var c=admin();var p=c.prepareStatement("SELECT id FROM arquivos WHERE id_evento=? OR id_comunidade IN(SELECT id_comunidade FROM comunidades WHERE id_evento=?)")) {
                p.setLong(1,e);p.setLong(2,e);try(var r=p.executeQuery()){while(r.next())ids.add(r.getString(1));}
            }
            exec("DELETE FROM arquivos WHERE id_evento=? OR id_comunidade IN(SELECT id_comunidade FROM comunidades WHERE id_evento=?)",e,e);
            for(String id:ids)Files.deleteIfExists(config.getDiretorioArquivos().resolve(id+".png"));
        }
    }
    @AfterAll static void retirarMigracaoDeTeste() throws Exception {
        if(!migracaoCriada)return;
        assertEquals(0,valor("SELECT COUNT(*) FROM arquivos"));
        // Objetos criados nesta suíte, nunca tabelas v0.2, nenhuma FK desativada.
        exec("DROP TABLE arquivos");exec("DROP TABLE demo_execucao");
    }
    long comunidade(long e) throws Exception{return valor("SELECT id_comunidade FROM comunidades WHERE id_evento="+e);}
    long membro() throws Exception {
        long e=evento(co);comprar(e,1,ca);long com=comunidade(e);status(200,pedir("POST","/api/comunidades/"+com+"/participacao",null,ca));return com;
    }
    static HttpResponse<String> upload(long alvo,String contexto,String cookie) throws Exception {return upload(alvo,contexto,cookie,"a.png","image/png",ImagemSeguraTest.png(),"","self",false);}
    static HttpResponse<String> upload(long alvo,String contexto,String cookie,String nome,String mime,byte[] b,String extra,String origin,boolean chunked) throws Exception {
        byte[] corpo=ImagemSeguraTest.multipart(contexto,alvo,nome,mime,b,extra);
        var pub=chunked?HttpRequest.BodyPublishers.ofInputStream(()->new java.io.ByteArrayInputStream(corpo)):HttpRequest.BodyPublishers.ofByteArray(corpo);
        var q=HttpRequest.newBuilder(URI.create(base+"/api/arquivos")).header("Content-Type","multipart/form-data; boundary=JoinUp06").POST(pub);
        if(cookie!=null)q.header("Cookie",cookie);if(origin!=null)q.header("Origin",origin.equals("self")?base:origin);
        return client.send(q.build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
    String enviar(long com,String contexto,String cookie) throws Exception {var r=upload(com,contexto,cookie);status(201,r);return (String)dados(r).get("imagem");}
    static HttpResponse<byte[]> imagem(String ref,String cookie) throws Exception {
        var q=HttpRequest.newBuilder(URI.create(base+ref));if(cookie!=null)q.header("Cookie",cookie);return client.send(q.GET().build(),HttpResponse.BodyHandlers.ofByteArray());
    }
    HttpResponse<String> post(long com,String ref,String cookie) throws Exception {return pedir("POST","/api/comunidades/"+com+"/postagens",Map.of("descricao","","imagem",ref),cookie);}
    @Test void eventoPublicoCatalogoComunidadeEEdicaoPosterior() throws Exception {
        long e=evento(co);String ref=enviar(e,"evento",co);
        assertEquals(200,imagem(ref,null).statusCode());
        assertEquals(ref,dados(pedir("GET","/api/eventos/"+e,null,null)).get("imagem"));
        assertTrue(pedir("GET","/api/eventos",null,null).body().contains(ref));
        assertEquals(ref,dados(pedir("GET","/api/comunidades/"+comunidade(e),null,null)).get("imagem"));
        status(200,pedir("PATCH","/api/organizador/eventos/"+e,Map.of("nome","Alterado após imagem"),co));
        assertEquals(ref,dados(pedir("GET","/api/eventos/"+e,null,null)).get("imagem"));
        String nova=enviar(e,"evento",co);assertEquals(404,imagem(ref,null).statusCode());assertEquals(200,imagem(nova,null).statusCode());
        status(200,pedir("PATCH","/api/organizador/eventos/"+e,Map.of("imagem",""),co));
        assertNull(dados(pedir("GET","/api/comunidades/"+comunidade(e),null,null)).get("imagem"));assertEquals(404,imagem(nova,null).statusCode());
    }
    @Test void donoEventoFuturoEContaAtivaObrigatorios() throws Exception {
        long e=evento(co);status(403,upload(e,"evento",coB));status(403,upload(e,"evento",ca));status(403,upload(e,"evento",cs));
        exec("UPDATE eventos SET data=UTC_TIMESTAMP() WHERE id_evento=?",e);status(409,upload(e,"evento",co));
        assertEquals(0,valor("SELECT COUNT(*) FROM arquivos"));
    }
    @Test void postagemSomenteImagemNaoLiberaPostVazio() throws Exception {
        long com=membro();String ref=enviar(com,"postagem",ca);
        var r=post(com,ref,ca);status(201,r);assertEquals("",dados(r).get("descricao"));assertEquals(ref,dados(r).get("imagem"));
        status(400,pedir("POST","/api/comunidades/"+com+"/postagens",Map.of("descricao","  "),ca));
        status(404,post(com,ref,ca)); // uso único
        assertEquals(1,valor("SELECT COUNT(*) FROM postagens WHERE id_comunidade="+com));
    }
    @ParameterizedTest @ValueSource(strings={"participante","organizador"}) void mensagemComImagem(String perfil) throws Exception {
        long com=membro();String cookie=perfil.equals("participante")?ca:co;
        String ref=enviar(com,"mensagem",cookie);
        status(201,pedir("POST","/api/comunidades/"+com+"/mensagens",Map.of("mensagem","<img onerror=alert(1)>","imagem",ref),cookie));
        assertTrue(pedir("GET","/api/comunidades/"+com+"/mensagens",null,ca).body().contains(ref));assertEquals(200,imagem(ref,co).statusCode());
        String semTexto=enviar(com,"mensagem",cookie);
        status(201,pedir("POST","/api/comunidades/"+com+"/mensagens",Map.of("mensagem","","imagem",semTexto),cookie));
    }
    @Test void privadoExigeSessaoFiliacaoIngressoDonoEHeaders() throws Exception {
        long com=membro();String ref=enviar(com,"postagem",ca);status(201,post(com,ref,ca));
        assertEquals(401,imagem(ref,null).statusCode());assertEquals(403,imagem(ref,cb).statusCode());assertEquals(403,imagem(ref,coB).statusCode());assertEquals(403,imagem(ref,cs).statusCode());
        var r=imagem(ref,ca);assertEquals(200,r.statusCode());assertEquals("no-store",r.headers().firstValue("Cache-Control").orElseThrow());
        assertEquals("nosniff",r.headers().firstValue("X-Content-Type-Options").orElseThrow());assertEquals("image/png",r.headers().firstValue("Content-Type").orElseThrow());
        assertEquals("inline; filename=\"imagem.png\"",r.headers().firstValue("Content-Disposition").orElseThrow());
        status(200,pedir("DELETE","/api/comunidades/"+com+"/participacao",null,ca));assertEquals(403,imagem(ref,ca).statusCode());
        status(200,pedir("POST","/api/comunidades/"+com+"/participacao",null,ca));exec("UPDATE participantes SET ativo=0 WHERE id_participante=?",ana);assertEquals(403,imagem(ref,ca).statusCode());
    }
    @Test void privadoPosEventoComIngressoUtilizado() throws Exception {
        long com=membro();String ref=enviar(com,"postagem",ca);status(201,post(com,ref,ca));
        long e=eventos.getFirst();exec("UPDATE eventos SET data=UTC_TIMESTAMP()-INTERVAL 1 DAY WHERE id_evento=?",e);exec("UPDATE ingressos SET status='utilizado' WHERE id_evento=?",e);
        assertEquals(200,imagem(ref,ca).statusCode());status(201,upload(com,"mensagem",ca));
        exec("UPDATE ingressos SET status='cancelado' WHERE id_evento=?",e);assertEquals(403,imagem(ref,ca).statusCode());
    }
    @Test void ingressoSemFiliacaoNaoPodeLerOuEnviar() throws Exception {
        long com=membro();String ref=enviar(com,"postagem",ca);status(201,post(com,ref,ca));comprar(eventos.getFirst(),1,cb);
        assertEquals(403,imagem(ref,cb).statusCode());status(403,upload(com,"postagem",cb));
        status(200,pedir("POST","/api/comunidades/"+com+"/participacao",null,cb));assertEquals(200,imagem(ref,cb).statusCode());
    }
    @Test void pendingPreviewApenasDonoEReusoCruzadoRecusado() throws Exception {
        long com=membro();comprar(eventos.getFirst(),1,cb);status(200,pedir("POST","/api/comunidades/"+com+"/participacao",null,cb));
        String ref=enviar(com,"postagem",ca);assertEquals(404,imagem(ref,cb).statusCode());status(404,post(com,ref,cb));
        long comB=membro();status(404,post(comB,ref,ca));
        status(404,pedir("POST","/api/comunidades/"+com+"/mensagens",Map.of("mensagem","x","imagem",ref),ca));
        status(201,post(com,ref,ca));assertEquals(200,imagem(ref,cb).statusCode());
    }
    @ParameterizedTest @ValueSource(strings={"../a.png","file:a.png","data:a.png","a.svg","a.html","a.png.exe"}) void nomesInvalidos(String nome) throws Exception {
        long e=evento(co);status(400,upload(e,"evento",co,nome,"image/png",ImagemSeguraTest.png(),"","self",false));assertEquals(0,valor("SELECT COUNT(*) FROM arquivos"));
    }
    @ParameterizedTest @ValueSource(strings={"image/jpeg","image/webp","image/svg+xml","text/html"}) void mimeForjado(String mime) throws Exception {
        status(400,upload(evento(co),"evento",co,"a.png",mime,ImagemSeguraTest.png(),"","self",false));
    }
    @ParameterizedTest @ValueSource(strings={"abc","<html>x</html>","\u0089PNG\r\n\u001a\ntruncado"}) void bytesInvalidos(String bytes) throws Exception {
        status(400,upload(evento(co),"evento",co,"a.png","image/png",bytes.getBytes(StandardCharsets.ISO_8859_1),"","self",false));
    }
    @ParameterizedTest @ValueSource(strings={"Content-Transfer-Encoding: base64\r\n","X-Owner: 2\r\n","Content-Type: image/png\r\n"}) void headersSuspeitos(String extra) throws Exception {
        status(400,upload(evento(co),"evento",co,"a.png","image/png",ImagemSeguraTest.png(),extra,"self",false));
    }
    @ParameterizedTest @ValueSource(strings={"false","true"}) void limiteMesmoEmChunked(String chunked) throws Exception {
        long e=evento(co);status(413,upload(e,"evento",co,"a.png","image/png",new byte[1048577],"","self",Boolean.parseBoolean(chunked)));
        if(Boolean.parseBoolean(chunked)) status(413,upload(e,"evento",co,"a.png","image/png",new byte[MultipartImagem.MAX_CORPO+1],"","self",true));
        else {
            // Excesso declarado é recusado antes de ler o corpo: verificar resposta sem
            // HttpClient continuar escrevendo 1 MiB depois do 413 e provocar TCP reset.
            try(var socket=new Socket("127.0.0.1",app.getPortaHttp())) {
                socket.setSoTimeout(5000);
                String headers="POST /api/arquivos HTTP/1.1\r\nHost: 127.0.0.1:"+app.getPortaHttp()+"\r\nContent-Type: multipart/form-data; boundary=JoinUp06\r\nContent-Length: "+(MultipartImagem.MAX_CORPO+1)+"\r\nOrigin: "+base+"\r\nCookie: "+co+"\r\nConnection: close\r\n\r\n";
                socket.getOutputStream().write(headers.getBytes(StandardCharsets.US_ASCII));socket.getOutputStream().flush();socket.shutdownOutput();
                var reader=new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream(),StandardCharsets.US_ASCII));
                assertTrue(reader.readLine().contains("413"));
            }
        }
        status(413,pedir("PATCH","/api/organizador/eventos/"+e,Map.of("descricao","x".repeat(65536)),co));
    }
    @Test void csrfAntesDePersistirEAnonimoBloqueado() throws Exception {
        long e=evento(co);status(403,upload(e,"evento",co,"a.png","image/png",ImagemSeguraTest.png(),"","https://alheio.test",false));status(401,upload(e,"evento",null));
        assertEquals(0,valor("SELECT COUNT(*) FROM arquivos"));
    }
    @ParameterizedTest @ValueSource(strings={"file:///tmp/a.png","data:image/png;base64,abc","https://example.test/a.png","../a.png","/api/arquivos/00000000000000000000000000000000"}) void referenciaArbitrariaNaoEntraNoBanco(String ref) throws Exception {
        long com=membro();status(ref.startsWith("/api/arquivos/")?404:400,post(com,ref,ca));assertEquals(0,valor("SELECT COUNT(*) FROM postagens WHERE id_comunidade="+com));
    }
    @Test void falhaSqlLimpaArquivoESemReferenciaEvento() throws Exception {
        long e=evento(co);long antes=quantidadeArquivos();exec("CREATE TRIGGER it06_falha BEFORE INSERT ON arquivos FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture06'");
        try {var r=upload(e,"evento",co);status(503,r);assertFalse(r.body().contains(config.getDiretorioArquivos().toString()));assertEquals(0,valor("SELECT COUNT(*) FROM arquivos"));assertEquals(antes,quantidadeArquivos());assertNull(dados(pedir("GET","/api/eventos/"+e,null,null)).get("imagem"));}
        finally {exec("DROP TRIGGER it06_falha");}
    }
    @Test void falhaVinculacaoRevertePostMantendoPendingCoerente() throws Exception {
        long com=membro();String ref=enviar(com,"postagem",ca);exec("CREATE TRIGGER it06_link BEFORE UPDATE ON arquivos FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture06'");
        try{status(503,post(com,ref,ca));assertEquals(0,valor("SELECT COUNT(*) FROM postagens WHERE id_comunidade="+com));assertEquals(200,imagem(ref,ca).statusCode());}
        finally{exec("DROP TRIGGER it06_link");}
        status(201,post(com,ref,ca));
    }
    @Test void arquivoFaltanteNaoVinculaENaoVazaCaminho() throws Exception {
        long com=membro();String ref=enviar(com,"postagem",ca);Files.delete(config.getDiretorioArquivos().resolve(ArquivoService.idReferencia(ref)+".png"));
        status(404,post(com,ref,ca));assertEquals(0,valor("SELECT COUNT(*) FROM postagens WHERE id_comunidade="+com));assertEquals(404,imagem(ref,ca).statusCode());
    }
    @Test void falhaDeFilesystemNaoCriaMetadata() throws Exception {
        long e=evento(co);Path dir=config.getDiretorioArquivos();Files.createDirectories(dir);Path backup=dir.resolveSibling(dir.getFileName()+"-fixture06");
        Files.move(dir,backup);Files.writeString(dir,"fixture bloqueia diretório");
        try{status(503,upload(e,"evento",co));assertEquals(0,valor("SELECT COUNT(*) FROM arquivos"));}
        finally{Files.delete(dir);Files.move(backup,dir);}
    }
    @Test void perdaElegibilidadeDuranteUploadRevalidadaAposLock() throws Exception {
        long com=membro();long e=eventos.getFirst();
        try(var c=admin();var pool=Executors.newSingleThreadExecutor()) {
            c.setAutoCommit(false);try(var p=c.prepareStatement("SELECT id_evento FROM eventos WHERE id_evento=? FOR UPDATE")){p.setLong(1,e);p.executeQuery().close();}
            var futuro=pool.submit(()->upload(com,"postagem",ca));esperarEsperas(1);
            try(var p=c.prepareStatement("UPDATE ingressos SET status='cancelado' WHERE id_evento=?")){p.setLong(1,e);p.executeUpdate();}c.commit();
            status(403,futuro.get(20,TimeUnit.SECONDS));assertEquals(0,valor("SELECT COUNT(*) FROM arquivos"));
        }
    }
    @Test void ultimaRevendaInvalidaLeituraEVinculacaoDePending() throws Exception {
        long com=membro();String ref=enviar(com,"postagem",ca), publicado=enviar(com,"postagem",ca);status(201,post(com,publicado,ca));
        long i=valor("SELECT id_ingresso FROM ingressos WHERE id_evento="+eventos.getFirst());
        var anuncio=pedir("POST","/api/revendas",Map.of("ingressoIds",List.of(i),"precoUnitario",12),ca);status(201,anuncio);
        long a=valor("SELECT id_revenda_anuncios FROM revenda_anuncios WHERE id_ingresso="+i);
        status(200,pedir("POST","/api/revendas/"+a+"/compra",Map.of(),cb));
        assertEquals(403,imagem(publicado,ca).statusCode());status(403,post(com,ref,ca));
        assertEquals(403,imagem(publicado,cb).statusCode());status(200,pedir("POST","/api/comunidades/"+com+"/participacao",null,cb));assertEquals(200,imagem(publicado,cb).statusCode());
    }
    @Test void jpegWebpUploadHttpNormalizados() throws Exception {
        long e=evento(co);
        for(String tipo:List.of("jpeg","webp")) {
            byte[] b=tipo.equals("webp")?Base64.getDecoder().decode(ImagemSeguraTest.WEBP):ImagemSeguraTest.raster(tipo,2,2);
            var r=upload(e,"evento",co,"a."+tipo,"image/"+tipo,b,"","self",false);status(201,r);assertEquals(200,imagem((String)dados(r).get("imagem"),null).statusCode());
        }
    }
    @ParameterizedTest @ValueSource(strings={"uploadPrimeiro","vendaPrimeiro","vinculoPrimeiro","vendaAntesVinculo"})
    void revendaConcorrenteComUploadEVinculacao(String caso) throws Exception {
        long com=membro(),e=eventos.getFirst();
        boolean vinculo=caso.contains("Vinculo") || caso.startsWith("vinculo"), socialPrimeiro=caso.equals("uploadPrimeiro") || caso.equals("vinculoPrimeiro");
        String ref=vinculo?enviar(com,"postagem",ca):null;
        long i=valor("SELECT id_ingresso FROM ingressos WHERE id_evento="+e);
        status(201,pedir("POST","/api/revendas",Map.of("ingressoIds",List.of(i),"precoUnitario",12),ca));
        long a=valor("SELECT id_revenda_anuncios FROM revenda_anuncios WHERE id_ingresso="+i);
        Callable<HttpResponse<String>> social=()->vinculo?post(com,ref,ca):upload(com,"postagem",ca);
        Callable<HttpResponse<String>> venda=()->pedir("POST","/api/revendas/"+a+"/compra",Map.of(),cb);
        try(var c=admin();var pool=Executors.newFixedThreadPool(2)) {
            c.setAutoCommit(false);try(var p=c.prepareStatement("SELECT id_evento FROM eventos WHERE id_evento=? FOR UPDATE")){p.setLong(1,e);p.executeQuery().close();}
            var primeiro=pool.submit(socialPrimeiro?social:venda);esperarEsperas(1);
            var segundo=pool.submit(socialPrimeiro?venda:social);esperarEsperas(2);c.commit();
            status(socialPrimeiro?201:200,primeiro.get(20,TimeUnit.SECONDS));status(socialPrimeiro?200:403,segundo.get(20,TimeUnit.SECONDS));
        }
        assertEquals(bruno,valor("SELECT id_participante FROM ingressos WHERE id_ingresso="+i));
        assertEquals(0,valor("SELECT ativo FROM participante_comunidades WHERE id_comunidade="+com+" AND id_participante="+ana));
        assertEquals(vinculo && socialPrimeiro?1:0,valor("SELECT COUNT(*) FROM postagens WHERE id_comunidade="+com));
    }
    @Test void arquivoCorrompidoOuPendingExpiradoNaoVincula() throws Exception {
        long com=membro();String ref=enviar(com,"postagem",ca);
        Files.writeString(config.getDiretorioArquivos().resolve(ArquivoService.idReferencia(ref)+".png"),"corrompido");
        status(404,post(com,ref,ca));assertEquals(404,imagem(ref,ca).statusCode());
        String expirado=enviar(com,"postagem",ca);exec("UPDATE arquivos SET criado_em=UTC_TIMESTAMP()-INTERVAL 2 DAY WHERE id=?",ArquivoService.idReferencia(expirado));
        status(404,post(com,expirado,ca));assertEquals(404,imagem(expirado,ca).statusCode());
    }
    static long quantidadeArquivos() throws Exception {
        if(!Files.exists(config.getDiretorioArquivos()))return 0;try(var s=Files.list(config.getDiretorioArquivos())){return s.count();}
    }
}
