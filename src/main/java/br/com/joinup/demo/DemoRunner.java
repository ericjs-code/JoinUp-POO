package br.com.joinup.demo;

import br.com.joinup.config.ConfiguracaoSistema;
import br.com.joinup.shared.db.ConnectionFactory;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Seed opt-in sem DDL. Recusa base populada sem marcador administrativo de proveniência. */
public final class DemoRunner {
    public static final List<String> TABELAS=List.of("artistas","organizadores","generos_musicais","eventos","participantes",
            "comunidades","participante_comunidades","comunidade_mensagens","postagens","comentarios","tipos_ingressos",
            "compras","compra_itens","ingressos","line_up","revenda_anuncios","usuarios_suporte","ticket","postagem_curtidas",
            "sessoes","auditoria_reativacoes","ticket_mensagens","arquivos");
    private final ConnectionFactory conexoes;
    public DemoRunner(ConnectionFactory c){conexoes=c;}
    public void executar(ConfiguracaoSistema config) {
        if(!config.isModoDemo())return; // inclusive sem abrir conexão/ler recursos.
        String destino=URI.create(config.getUrlJdbc().substring(5)).getHost();
        if(!config.getAmbiente().equals("demo") || !Set.of("127.0.0.1","localhost","[::1]").contains(destino)
                || !config.getHostHttp().equals("127.0.0.1") || (!config.getOrigemPublica().isEmpty() && !config.getOrigemPublica().startsWith("http://127.0.0.1:")))
            throw new IllegalStateException("Demo exige JOINUP_ENV=demo e execução exclusivamente local");
        try(var c=conexoes.abrirConexao()) { aplicar(c); }
        catch(SQLException e){throw new IllegalStateException("Carga demo recusada; confira schema/migração, conta DML e proveniência da base");}
    }
    public static void aplicar(Connection c) throws SQLException {
        if(!"joinup_poo".equals(escalar(c,"SELECT DATABASE()")))throw new SQLException("Banco demo inválido");
        if(!"1".equals(escalar(c,"SELECT GET_LOCK('joinup_poo.demo_v02',10)")))throw new SQLException("Demo concorrente");
        boolean auto=c.getAutoCommit();
        try {
            c.setAutoCommit(false);
            for(String tabela:TABELAS)escalar(c,"SELECT COUNT(*) FROM "+tabela); // schema existente, nenhuma criação.
            String versao=null;
            try(var s=c.createStatement();var r=s.executeQuery("SELECT versao FROM demo_execucao WHERE id=1 FOR UPDATE")){if(r.next())versao=r.getString(1);}
            if(versao!=null){if(!versao.equals("seed_v0.2"))throw new SQLException("Versão demo incompatível");c.commit();return;}
            for(String tabela:TABELAS)if(!"0".equals(escalar(c,"SELECT COUNT(*) FROM "+tabela)))throw new SQLException("Base não demonstrativa");
            String sql;
            try(var in=DemoRunner.class.getResourceAsStream("/db/seed_demo_v0.2.sql")){sql=new String(Objects.requireNonNull(in).readAllBytes(),StandardCharsets.UTF_8);}
            catch(java.io.IOException e){throw new SQLException("Seed indisponível");}
            for(String statement:SqlDemo.separar(sql)) {
                if(statement.equals("USE `joinup_poo`"))continue; // destino já conferido, USE nunca troca banco.
                var m=java.util.regex.Pattern.compile("INSERT IGNORE INTO `([a-z_]+)`[\\s\\S]*").matcher(statement);
                if(!m.matches() || !TABELAS.contains(m.group(1)))throw new SQLException("Comando inesperado no seed empacotado");
                try(var st=c.createStatement()) {
                    st.executeUpdate(statement);
                    // Base vazia: nenhuma duplicata esperada; IGNORE não pode esconder truncamento/FK.
                    if(st.getWarnings()!=null)throw new SQLException("Warning no seed");
                }
            }
            try(var s=c.createStatement()){s.executeUpdate("INSERT INTO demo_execucao(id,versao) VALUES(1,'seed_v0.2')");}
            c.commit();
        } catch(SQLException | RuntimeException e){c.rollback();throw e;}
        finally {
            c.setAutoCommit(auto);
            try(var s=c.createStatement()){s.executeQuery("SELECT RELEASE_LOCK('joinup_poo.demo_v02')").close();}
        }
    }
    private static String escalar(Connection c,String sql) throws SQLException {
        try(var s=c.createStatement();var r=s.executeQuery(sql)){if(!r.next())throw new SQLException("Validação demo sem resultado");return r.getString(1);}
    }
}
