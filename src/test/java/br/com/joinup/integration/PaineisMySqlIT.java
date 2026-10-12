package br.com.joinup.integration;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PaineisMySqlIT extends MySql05Fixture {
    Map<?,?> painel(String perfil,String ck) throws Exception {var r=pedir("GET","/api/"+perfil+"/painel",null,ck);status(200,r);semSegredos(r.body());return dados(r);}
    static Map<?,?> indicadores(Map<?,?> painel){return (Map<?,?>)painel.get("indicadores");}
    static Map<?,?> estados(Map<?,?> painel){return (Map<?,?>)painel.get("chamadosPorStatus");}
    static void semSegredos(String texto){for(String s:List.of("cpf","cnpj","senha","pbkdf2","email","hash","token","joinup.test","Solicitação privada"))assertFalse(texto.contains(s),texto);}
    static void dinheiro(String esperado,Object real){assertEquals(0,new BigDecimal(esperado).compareTo(new BigDecimal(real.toString())));}
    @ParameterizedTest @ValueSource(strings={"participante","organizador","suporte"})
    void perfilExatoVisitanteESessaoInvalida(String perfil) throws Exception {
        status(401,pedir("GET","/api/"+perfil+"/painel",null,null));status(401,pedir("GET","/api/"+perfil+"/painel",null,"joinup_sessao="+"a".repeat(43)));
        for(String ck:List.of(ca,co,cs)) {boolean certo=perfil.equals("participante")?ck.equals(ca):perfil.equals("organizador")?ck.equals(co):ck.equals(cs);status(certo?200:403,pedir("GET","/api/"+perfil+"/painel",null,ck));}
    }
    @Test void participanteVazioContaPropriaSemInfluenciaDeDemo() throws Exception {
        var p=painel("participante",ca);for(Object n:indicadores(p).values())assertEquals(0,new BigDecimal(n.toString()).signum());
        for(Object n:estados(p).values())assertEquals(0,((Number)n).longValue());assertTrue(((List<?>)p.get("eventos")).isEmpty());
    }
    @Test void participantePosCompraRevendaEComunidadeAcessoReal() throws Exception {
        long e=evento(co),ingresso=comprar(e,2,ca),cm=valor("SELECT id_comunidade FROM comunidades WHERE id_evento="+e);
        long id=abrir(ca);abrir(cb);status(200,pedir("POST","/api/comunidades/"+cm+"/participacao",Map.of(),ca));
        var p=painel("participante",ca);var i=indicadores(p);
        assertEquals(2,num(i,"ingressos"));assertEquals(2,num(i,"proximosIngressos"));assertEquals(1,num(i,"proximosEventos"));assertEquals(1,num(i,"comprasConfirmadas"));dinheiro("20",i.get("valorHistoricoCompras"));assertEquals(1,num(i,"comunidadesAcessiveis"));assertEquals(1,num(estados(p),"aberto"));
        var anuncio=pedir("POST","/api/revendas",Map.of("ingressoIds",List.of(ingresso),"precoUnitario",15),ca);status(201,anuncio);
        long aid=valor("SELECT id_revenda_anuncios FROM revenda_anuncios WHERE id_ingresso="+ingresso+" AND status='disponivel'");
        assertEquals(1,num(indicadores(painel("participante",ca)),"anunciosDisponiveis"));
        status(200,pedir("POST","/api/revendas/"+aid+"/compra",Map.of(),cb));
        i=indicadores(painel("participante",ca));assertEquals(1,num(i,"ingressos"));assertEquals(1,num(i,"anunciosVendidos"));dinheiro("20",i.get("valorHistoricoCompras"));
        var b=indicadores(painel("participante",cb));assertEquals(1,num(b,"ingressos"));assertEquals(0,num(b,"comprasConfirmadas"));assertEquals(0,num(b,"comunidadesAcessiveis"));
        exec("UPDATE ingressos SET status='utilizado' WHERE id_evento=? AND id_participante=?",e,ana);exec("UPDATE eventos SET data=UTC_TIMESTAMP()-INTERVAL 1 DAY WHERE id_evento=?",e);
        i=indicadores(painel("participante",ca));assertEquals(0,num(i,"proximosIngressos"));assertEquals(1,num(i,"comunidadesAcessiveis"));
        exec("UPDATE ingressos SET status='cancelado' WHERE id_evento=? AND id_participante=?",e,ana);assertEquals(0,num(indicadores(painel("participante",ca)),"comunidadesAcessiveis"));
        status(200,pedir("POST",sup(id,"mensagens"),Map.of("mensagem","Orientação"),cs));assertEquals(1,num(estados(painel("participante",ca)),"aguardando_usuario"));
    }
    @Test void organizadoresIsoladosHistoricoSemMultiplicarItensECategorias() throws Exception {
        long a=evento(co),b=evento(coB),i=comprar(a,2,ca);comprar(b,1,cb);
        long tipoVip=valor("SELECT MAX(id_tipos_ingressos) FROM tipos_ingressos WHERE id_evento="+a);
        var compra=pedir("POST","/api/compras",Map.of("eventoId",a,"itens",List.of(Map.of("tipoIngressoId",tipoVip,"quantidade",3))),cb);status(201,compra);
        abrir(co);long bt=ticket("aguardando_usuario",coB);
        var pa=painel("organizador",co);var pb=painel("organizador",coB);var ia=indicadores(pa);var ib=indicadores(pb);
        assertEquals(1,num(ia,"eventos"));assertEquals(2,num(ia,"categorias"));assertEquals(5,num(ia,"ingressosVendidos"));assertEquals(13,num(ia,"estoqueDisponivel"));assertEquals(2,num(ia,"comprasConfirmadas"));dinheiro("80",ia.get("valorHistoricoCompras"));
        assertEquals(1,num(ib,"eventos"));assertEquals(1,num(ib,"ingressosVendidos"));dinheiro("10",ib.get("valorHistoricoCompras"));
        assertEquals(1,num(estados(pa),"aberto"));assertEquals(0,num(estados(pa),"aguardando_usuario"));assertEquals(1,num(estados(pb),"aguardando_usuario"));
        assertEquals(a,num((Map<?,?>)((List<?>)pa.get("eventos")).getFirst(),"id"));assertEquals(b,num((Map<?,?>)((List<?>)pb.get("eventos")).getFirst(),"id"));
        // Mutar preço atual não altera o valor histórico das compras confirmadas.
        exec("UPDATE tipos_ingressos SET valor=999 WHERE id_evento=?",a);dinheiro("80",indicadores(painel("organizador",co)).get("valorHistoricoCompras"));
        exec("UPDATE eventos SET data=UTC_TIMESTAMP()-INTERVAL 1 DAY WHERE id_evento=?",a);ia=indicadores(painel("organizador",co));assertEquals(0,num(ia,"eventosFuturos"));assertEquals(1,num(ia,"eventosIniciados"));
        assertTrue(i>0&&bt>0);
    }
    @Test void metricasSuporteGlobalEAutoriaDiferenteDaAssociacao() throws Exception {
        var antes=painel("suporte",cs);long id=abrir(ca);abrir(co);
        status(200,pedir("PATCH",sup(id,"associacao"),Map.of(),cs));status(200,pedir("POST",sup(id,"mensagens"),Map.of("mensagem","B respondeu"),csB));
        var a=painel("suporte",cs);var b=painel("suporte",csB);
        assertEquals(num(indicadores(antes),"chamados")+2,num(indicadores(a),"chamados"));
        assertEquals(1,num(indicadores(a),"associadosAMim"));assertEquals(0,num(indicadores(a),"minhasRespostas"));
        assertEquals(1,num(indicadores(b),"minhasRespostas"));assertEquals(1,num(indicadores(b),"chamadosRespondidosPorMim"));assertEquals(0,num(indicadores(b),"associadosAMim"));
        assertEquals(num(estados(antes),"aguardando_usuario")+1,num(estados(a),"aguardando_usuario"));
        assertEquals(num(indicadores(antes),"respostasSuporte")+1,num(indicadores(a),"respostasSuporte"));
    }
    @ParameterizedTest @ValueSource(strings={"participante","organizador"})
    void inativaBloqueadaComSessaoExistente(String perfil) throws Exception {
        String ck=perfil.equals("participante")?ca:co;
        exec("UPDATE "+(perfil.equals("participante")?"participantes SET ativo=0 WHERE id_participante=?":"organizadores SET ativo=0 WHERE id_organizador=?"),perfil.equals("participante")?ana:orgA);
        status(403,pedir("GET","/api/"+perfil+"/painel",null,ck));
    }
    @Test void dataEventoEContagemSemRestricaoOrganizadorInativoParaComunidade() throws Exception {
        long e=evento(co);comprar(e,1,ca);long cm=valor("SELECT id_comunidade FROM comunidades WHERE id_evento="+e);
        status(200,pedir("POST","/api/comunidades/"+cm+"/participacao",Map.of(),ca));exec("UPDATE organizadores SET ativo=0 WHERE id_organizador=?",orgA);
        assertEquals(1,num(indicadores(painel("participante",ca)),"comunidadesAcessiveis"));
        status(200,pedir("DELETE","/api/comunidades/"+cm+"/participacao",null,ca));assertEquals(0,num(indicadores(painel("participante",ca)),"comunidadesAcessiveis"));
    }
    @Test void indicadoresGlobaisDerivadosDeTimestampsReais() throws Exception {
        long id=ticket("fechado",ca);
        exec("UPDATE ticket SET data_criacao=UTC_TIMESTAMP()-INTERVAL 1 HOUR,data_fechamento=UTC_TIMESTAMP() WHERE id_ticket=?",id);
        var i=indicadores(painel("suporte",cs));
        assertEquals(valor("SELECT COUNT(*) FROM ticket WHERE id_participante IS NOT NULL"),num(i,"chamadosParticipantes"));
        assertEquals(valor("SELECT COUNT(*) FROM ticket WHERE id_organizador IS NOT NULL"),num(i,"chamadosOrganizadores"));
        assertEquals(valor("SELECT COUNT(*) FROM ticket WHERE data_criacao>=UTC_DATE() AND data_criacao<UTC_DATE()+INTERVAL 1 DAY"),num(i,"criadosHojeUtc"));
        try(var c=admin()) {
            dinheiro(escalar(c,"SELECT AVG(TIMESTAMPDIFF(SECOND,data_criacao,data_fechamento)) FROM ticket WHERE status_ticket='fechado' AND data_fechamento>=data_criacao"),i.get("mediaResolucaoSegundos"));
            dinheiro(escalar(c,"SELECT ROUND(100.0*SUM(status_ticket='fechado')/COUNT(*),2) FROM ticket"),i.get("percentualFechados"));
        }
    }
}
