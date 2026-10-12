package br.com.joinup.integration;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** RF-SUP-01..04: HTTP, cookies, CSRF e JDBC reais, sem mocks. */
class SuporteMySqlIT extends MySql05Fixture {
    @ParameterizedTest @ValueSource(strings={"participante","organizador"})
    void aberturaAutoriaVinculoUnicoEHistorico(String perfil) throws Exception {
        String ck=perfil.equals("participante")?ca:co;long id=abrir(ck);
        assertEquals("aberto",estado(id));assertEquals(1,mensagens(id));
        String coluna=perfil.equals("participante")?"id_participante":"id_organizador";
        long autor=perfil.equals("participante")?ana:orgA;
        assertEquals(autor,valor("SELECT "+coluna+" FROM ticket WHERE id_ticket="+id));
        assertEquals(autor,valor("SELECT "+coluna+" FROM ticket_mensagens WHERE id_ticket="+id));
        var r=pedir("GET",me(id,""),null,ck);status(200,r);assertTrue(r.body().contains("Solicitação privada 05"));
        assertFalse(r.body().contains("senha"));assertFalse(r.body().contains("email"));
    }
    @ParameterizedTest @CsvSource({
        "aberto,responder,200,aguardando_usuario", "em_atendimento,responder,200,aguardando_usuario", "aguardando_usuario,responder,200,aguardando_usuario", "fechado,responder,409,fechado",
        "aberto,encerrar,200,fechado", "em_atendimento,encerrar,200,fechado", "aguardando_usuario,encerrar,409,aguardando_usuario", "fechado,encerrar,409,fechado",
        "aberto,resolver,409,aberto", "em_atendimento,resolver,409,em_atendimento", "aguardando_usuario,resolver,200,fechado", "fechado,resolver,409,fechado",
        "aberto,retornar,409,aberto", "em_atendimento,retornar,409,em_atendimento", "aguardando_usuario,retornar,200,em_atendimento", "fechado,retornar,409,fechado"})
    void matrizCompleta(String origem,String acao,int http,String destino) throws Exception {
        for(String ck:List.of(ca,co)) {
            long id=ticket(origem,ck);
            boolean suporte=acao.equals("responder")||acao.equals("encerrar"), msg=acao.equals("responder")||acao.equals("retornar");
            String path=suporte?sup(id,acao.equals("responder")?"mensagens":"encerramento"):me(id,acao.equals("retornar")?"mensagens":"resolucao");
            var r=pedir("POST",path,msg?Map.of("mensagem","Nova mensagem"):Map.of(),suporte?cs:ck);status(http,r);
            assertEquals(destino,estado(id));assertEquals(http==200&&msg?2:1,mensagens(id));
            assertEquals(destino.equals("fechado")&&http==200?1:0,valor("SELECT COUNT(*) FROM ticket WHERE id_ticket="+id+" AND data_fechamento IS NOT NULL"));
            if(http==200&&msg)assertEquals(1,valor("SELECT COUNT(*) FROM ticket_mensagens WHERE id_ticket="+id+" AND autor_tipo='"+(suporte?"suporte":ck.equals(ca)?"participante":"organizador")+"' AND mensagem='Nova mensagem'"));
        }
    }
    @Test void doisSuportesAutoriaRealAssociacaoNaoRestringeENaoMudaEstado() throws Exception {
        long id=abrir(ca);status(200,pedir("PATCH",sup(id,"associacao"),Map.of(),cs));assertEquals("aberto",estado(id));
        assertEquals(supA,valor("SELECT id_usuario_suporte FROM ticket WHERE id_ticket="+id));
        status(200,pedir("POST",sup(id,"mensagens"),Map.of("mensagem","Resposta B"),csB));
        status(200,pedir("POST",sup(id,"mensagens"),Map.of("mensagem","Resposta A"),cs));
        assertEquals(1,valor("SELECT COUNT(*) FROM ticket_mensagens WHERE id_ticket="+id+" AND id_usuario_suporte="+supB+" AND mensagem='Resposta B'"));
        assertEquals(supA,valor("SELECT id_usuario_suporte FROM ticket WHERE id_ticket="+id));
        status(409,pedir("POST",sup(id,"encerramento"),Map.of(),csB));
        status(200,pedir("POST",me(id,"mensagens"),Map.of("mensagem","Ainda preciso"),ca));
        status(200,pedir("POST",sup(id,"encerramento"),Map.of(),csB));assertEquals(4,mensagens(id));
        status(200,pedir("PATCH",sup(id,"associacao"),Map.of(),csB));assertEquals("fechado",estado(id));
        assertEquals(supB,valor("SELECT id_usuario_suporte FROM ticket WHERE id_ticket="+id));assertEquals(4,mensagens(id));
    }
    @Test void fechadoTerminalSemRotasDeReaberturaEdicaoOuExclusao() throws Exception {
        long id=abrir(ca);status(200,pedir("POST",sup(id,"mensagens"),Map.of("mensagem","Orientação"),cs));
        status(200,pedir("POST",me(id,"resolucao"),Map.of(),ca));
        for(String ck:List.of(ca,cs,csB))status(409,pedir("POST",ck.equals(ca)?me(id,"mensagens"):sup(id,"mensagens"),Map.of("mensagem","Reabrir"),ck));
        status(409,pedir("POST",me(id,"resolucao"),Map.of(),ca));status(404,pedir("POST",me(id,"reabertura"),Map.of(),ca));
        for(String verbo:List.of("PATCH","PUT","DELETE"))status(405,pedir(verbo,me(id,""),Map.of(),ca));
        assertEquals(2,mensagens(id));assertEquals("fechado",estado(id));
    }
    @ParameterizedTest @ValueSource(strings={"visitante","participanteAlheio","organizadorAlheio","perfilCruzado","suporte"})
    void titularidadeDetalheRetornoResolucaoIDOR(String perfil) throws Exception {
        long id=ticket("aguardando_usuario",ca);
        String ck=switch(perfil){case "visitante"->null;case "participanteAlheio"->cb;case "organizadorAlheio"->coB;case "perfilCruzado"->co;default->cs;};
        int http=perfil.equals("visitante")?401:perfil.equals("suporte")?403:404;
        for(String acao:List.of("","mensagens","resolucao")) {
            var r=pedir(acao.isEmpty()?"GET":"POST",me(id,acao),acao.equals("mensagens")?Map.of("mensagem","IDOR"):Map.of(),ck);status(http,r);assertFalse(r.body().contains("Solicitação privada"));
        }
        assertEquals(1,mensagens(id));assertEquals("aguardando_usuario",estado(id));
        if(ck!=null&&!perfil.equals("suporte")) {var r=pedir("GET","/api/me/chamados",null,ck);status(200,r);assertEquals(0,num(envelope(r),"total"));}
    }
    @ParameterizedTest @ValueSource(strings={"visitante","participante","organizador"})
    void rotasSuporteExigemPerfil(String perfil) throws Exception {
        long id=abrir(ca);String ck=perfil.equals("visitante")?null:perfil.equals("participante")?ca:co;int n=ck==null?401:403;
        status(n,pedir("GET","/api/suporte/chamados",null,ck));status(n,pedir("GET",sup(id,""),null,ck));
        status(n,pedir("PATCH",sup(id,"associacao"),Map.of(),ck));status(n,pedir("POST",sup(id,"mensagens"),Map.of("mensagem","Teste"),ck));status(n,pedir("POST",sup(id,"encerramento"),Map.of(),ck));
        assertEquals(1,mensagens(id));assertEquals("aberto",estado(id));
    }
    @Test void suporteNaoAbreChamadoEInexistenteIndistinguivel() throws Exception {
        status(403,pedir("POST","/api/chamados",Map.of("titulo","x","descricao","x"),cs));
        for(String path:List.of(me(Integer.MAX_VALUE,""),sup(Integer.MAX_VALUE,"")))status(404,pedir("GET",path,null,path.contains("suporte")?cs:ca));
        for(String acao:List.of("mensagens","resolucao"))status(404,pedir("POST",me(Integer.MAX_VALUE,acao),acao.equals("mensagens")?Map.of("mensagem","x"):Map.of(),ca));
        for(String acao:List.of("mensagens","encerramento","associacao"))status(404,pedir(acao.equals("associacao")?"PATCH":"POST",sup(Integer.MAX_VALUE,acao),acao.equals("mensagens")?Map.of("mensagem","x"):Map.of(),cs));
    }
    @ParameterizedTest @ValueSource(strings={"abrir","retornar","resolver","responder","encerrar","associar"})
    void csrfEmCadaMutacao(String acao) throws Exception {
        long id=ticket(acao.equals("encerrar")?"aberto":"aguardando_usuario",ca);
        String path=switch(acao){case "abrir"->"/api/chamados";case "retornar"->me(id,"mensagens");case "resolver"->me(id,"resolucao");case "responder"->sup(id,"mensagens");case "encerrar"->sup(id,"encerramento");default->sup(id,"associacao");};
        String ck=List.of("responder","encerrar","associar").contains(acao)?cs:ca;
        for(String origin:Arrays.asList(null,"https://evil.test","null")) {
            var r=pedir(acao.equals("associar")?"PATCH":"POST",path,Map.of(),ck,origin);status(403,r);assertTrue(r.body().contains("CSRF_NEGADO"));
        }
        assertEquals(1,mensagens(id));
    }
    @ParameterizedTest @ValueSource(strings={"null","[]","{}","{","{\"mensagem\":3}","{\"mensagem\":\"  \"}","{\"mensagem\":\"a\",\"autorId\":1}","{\"mensagem\":\"a\",\"mensagem\":\"b\"}"})
    void payloadMensagemInvalido(String body) throws Exception {
        long id=ticket("aguardando_usuario",ca);status(400,pedir("POST",me(id,"mensagens"),body,ca));status(400,pedir("POST",sup(id,"mensagens"),body,cs));
        assertEquals(1,mensagens(id));assertEquals("aguardando_usuario",estado(id));
    }
    @Test void limitesUnicodeETextoObrigatorio() throws Exception {
        var r=pedir("POST","/api/chamados",Map.of("titulo","a".repeat(100),"descricao","😀".repeat(500)),ca);status(201,r);long id=num(dados(r),"id");
        assertEquals(500,valor("SELECT CHAR_LENGTH(descricao) FROM ticket WHERE id_ticket="+id));
        for(Object body:List.of(Map.of("titulo","a".repeat(101),"descricao","x"),Map.of("titulo","x","descricao","x".repeat(501)),Map.of("titulo"," ","descricao","x"),Map.of("titulo","x"),Map.of("titulo","x","descricao","x","id_participante",bruno)))status(400,pedir("POST","/api/chamados",body,ca));
        status(200,pedir("POST",sup(id,"mensagens"),Map.of("mensagem","x".repeat(500)),cs));
        status(400,pedir("POST",sup(id,"mensagens"),Map.of("mensagem","x".repeat(501)),cs));
        status(400,pedir("POST",me(id,"mensagens"),Map.of("mensagem",""),ca));
        status(400,pedir("POST",me(id,"resolucao"),Map.of("status","aberto"),ca));status(400,pedir("PATCH",sup(id,"associacao"),Map.of("suporteId",supB),cs));
    }
    @ParameterizedTest @ValueSource(strings={"0","-1","abc","1.5","2147483648"})
    void idsInvalidos(String id) throws Exception {status(400,pedir("GET","/api/me/chamados/"+id,null,ca));status(400,pedir("POST","/api/suporte/chamados/"+id+"/mensagens",Map.of("mensagem","x"),cs));}
    @Test void listasFiltrosPaginacaoMensagensEIsolamento() throws Exception {
        long id=abrir(ca);for(int i=0;i<4;i++)abrir(ca);long outro=ticket("fechado",cb);abrir(co);
        var r=pedir("GET","/api/me/chamados?tamanho=2",null,ca);status(200,r);assertEquals(5,num(envelope(r),"total"));assertEquals(2,((List<?>)envelope(r).get("dados")).size());
        assertEquals(1,((List<?>)envelope(pedir("GET","/api/me/chamados?tamanho=2&pagina=3",null,ca)).get("dados")).size());
        assertEquals(0,num(envelope(pedir("GET","/api/me/chamados?status=fechado",null,ca)),"total"));
        var fechados=pedir("GET","/api/suporte/chamados?status=fechado&tamanho=100",null,cs);status(200,fechados);assertTrue(fechados.body().contains("\"id\":"+outro));
        for(String query:List.of("status=invalid","status=aberto%27%20OR%201=1","pagina=0","pagina=10001","tamanho=101","pagina=1.5","tamanho=x","status=aberto&status=fechado"))status(400,pedir("GET","/api/suporte/chamados?"+query,null,cs));
        for(int i=0;i<55;i++)exec("INSERT INTO ticket_mensagens(id_ticket,autor_tipo,id_usuario_suporte,mensagem) VALUES(?,'suporte',?,'Página')",id,supA);
        var detalhe=dados(pedir("GET",me(id,"")+"?pagina=2&tamanho=50",null,ca));assertEquals(56,num(detalhe,"totalMensagens"));assertEquals(6,((List<?>)detalhe.get("mensagens")).size());
        status(400,pedir("GET",me(id,"")+"?tamanho=101",null,ca));
    }
    @ParameterizedTest @ValueSource(strings={"criar","responder","retornar"})
    void rollbackTriggerDepoisDeCriarTicketOuTransicionar(String acao) throws Exception {
        long id=acao.equals("criar")?0:ticket(acao.equals("retornar")?"aguardando_usuario":"aberto",ca);
        long antes=valor("SELECT COUNT(*) FROM ticket WHERE id_participante="+ana);
        exec("CREATE TRIGGER it05_mensagem BEFORE INSERT ON ticket_mensagens FOR EACH ROW BEGIN IF NEW.id_participante="+ana+" OR NEW.id_usuario_suporte="+supA+" THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='falha fixture05'; END IF; END");
        try {
            String path=acao.equals("criar")?"/api/chamados":acao.equals("responder")?sup(id,"mensagens"):me(id,"mensagens");
            Object body=acao.equals("criar")?Map.of("titulo","Falha","descricao","Rollback"):Map.of("mensagem","Rollback");
            var r=pedir("POST",path,body,acao.equals("responder")?cs:ca);status(503,r);assertFalse(r.body().contains("fixture05"));
            assertEquals(antes,valor("SELECT COUNT(*) FROM ticket WHERE id_participante="+ana));
            if(id>0){assertEquals(1,mensagens(id));assertEquals(acao.equals("retornar")?"aguardando_usuario":"aberto",estado(id));assertEquals(1,valor("SELECT COUNT(*) FROM ticket WHERE id_ticket="+id+" AND resposta IS NULL AND retorno_usuario IS NULL"));}
        } finally {exec("DROP TRIGGER it05_mensagem");}
    }
    @Test void duasRespostasConcorrentesNaoPerdemMensagem() throws Exception {
        long id=abrir(ca);
        try(var c=admin();var pool=Executors.newFixedThreadPool(2)) {
            bloquear(c,id);
            var a=pool.submit(()->pedir("POST",sup(id,"mensagens"),Map.of("mensagem","Concorrente A"),cs));esperarEsperas(1);
            var b=pool.submit(()->pedir("POST",sup(id,"mensagens"),Map.of("mensagem","Concorrente B"),csB));esperarEsperas(2);c.commit();
            status(200,a.get(20,TimeUnit.SECONDS));status(200,b.get(20,TimeUnit.SECONDS));
        }
        assertEquals(3,mensagens(id));assertEquals("aguardando_usuario",estado(id));
        assertEquals(2,valor("SELECT COUNT(DISTINCT id_usuario_suporte) FROM ticket_mensagens WHERE id_ticket="+id+" AND autor_tipo='suporte'"));
    }
    @ParameterizedTest @ValueSource(strings={"resolverPrimeiro","retornarPrimeiro"})
    void resolverVersusRetornarConcorrentesSoUmaTransicao(String ordem) throws Exception {
        long id=ticket("aguardando_usuario",ca);String ca2=login("participantes",ana);
        Callable<java.net.http.HttpResponse<String>> resolver=()->pedir("POST",me(id,"resolucao"),Map.of(),ca),retornar=()->pedir("POST",me(id,"mensagens"),Map.of("mensagem","Retorno concorrente"),ca2);
        boolean resolve=ordem.equals("resolverPrimeiro");
        try(var c=admin();var pool=Executors.newFixedThreadPool(2)) {
            bloquear(c,id);var a=pool.submit(resolve?resolver:retornar);esperarEsperas(1);var b=pool.submit(resolve?retornar:resolver);esperarEsperas(2);c.commit();
            status(200,a.get(20,TimeUnit.SECONDS));status(409,b.get(20,TimeUnit.SECONDS));
        }
        assertEquals(resolve?"fechado":"em_atendimento",estado(id));assertEquals(resolve?1:2,mensagens(id));
    }
    @ParameterizedTest @ValueSource(strings={"respostaPrimeiro","encerramentoPrimeiro","resolucaoPrimeiro","retornoPrimeiro"})
    void disputaSuporteSolicitanteRevalidaEstadoAtual(String ordem) throws Exception {
        boolean fechaSuporte=ordem.equals("encerramentoPrimeiro")||ordem.equals("respostaPrimeiro");
        long id=ticket(fechaSuporte?"aberto":"aguardando_usuario",ca);
        Callable<java.net.http.HttpResponse<String>> resposta=()->pedir("POST",sup(id,"mensagens"),Map.of("mensagem","Concorrente"),cs);
        Callable<java.net.http.HttpResponse<String>> outra=()->pedir("POST",fechaSuporte?sup(id,"encerramento"):me(id,ordem.equals("resolucaoPrimeiro")?"resolucao":"mensagens"),ordem.equals("retornoPrimeiro")?Map.of("mensagem","Retorno"):Map.of(),fechaSuporte?csB:ca);
        boolean respostaPrimeiro=ordem.equals("respostaPrimeiro");
        try(var c=admin();var pool=Executors.newFixedThreadPool(2)) {
            bloquear(c,id);var a=pool.submit(respostaPrimeiro?resposta:outra);esperarEsperas(1);var b=pool.submit(respostaPrimeiro?outra:resposta);esperarEsperas(2);c.commit();
            status(200,a.get(20,TimeUnit.SECONDS));status(ordem.equals("retornoPrimeiro")?200:409,b.get(20,TimeUnit.SECONDS));
        }
        assertEquals(ordem.equals("retornoPrimeiro")?3:respostaPrimeiro?2:1,mensagens(id));
        assertEquals(ordem.equals("retornoPrimeiro")||respostaPrimeiro?"aguardando_usuario":"fechado",estado(id));
    }
    @ParameterizedTest @ValueSource(strings={"participante","organizador"})
    void contaInativaBloqueiaLeituraECriacao(String tipo) throws Exception {
        String ck=tipo.equals("participante")?ca:co;long id=abrir(ck);
        exec("UPDATE "+(tipo.equals("participante")?"participantes SET ativo=0 WHERE id_participante=?":"organizadores SET ativo=0 WHERE id_organizador=?"),tipo.equals("participante")?ana:orgA);
        status(403,pedir("GET",me(id,""),null,ck));status(403,pedir("GET","/api/me/chamados",null,ck));status(403,pedir("POST","/api/chamados",Map.of("titulo","x","descricao","x"),ck));
        // Histórico continua atendível pelo suporte quando o solicitante está inativo.
        status(200,pedir("POST",sup(id,"mensagens"),Map.of("mensagem","Histórico preservado"),cs));assertEquals(2,mensagens(id));
    }
    @Test void inativacaoDuranteEsperaBloqueiaRetorno() throws Exception {
        long id=ticket("aguardando_usuario",ca);
        try(var c=admin();var pool=Executors.newSingleThreadExecutor()) {
            c.setAutoCommit(false);try(var st=c.createStatement();var r=st.executeQuery("SELECT id_participante FROM participantes WHERE id_participante="+ana+" FOR UPDATE")){assertTrue(r.next());}
            var f=pool.submit(()->pedir("POST",me(id,"mensagens"),Map.of("mensagem","Não gravar"),ca));esperarEsperas(1);
            try(var p=c.prepareStatement("UPDATE participantes SET ativo=0 WHERE id_participante=?")){p.setLong(1,ana);p.executeUpdate();}c.commit();status(403,f.get(20,TimeUnit.SECONDS));
        }
        assertEquals(1,mensagens(id));assertEquals("aguardando_usuario",estado(id));
    }
}
