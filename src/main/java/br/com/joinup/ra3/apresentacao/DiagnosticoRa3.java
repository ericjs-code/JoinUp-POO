package br.com.joinup.ra3.apresentacao;

import br.com.joinup.ra3.persistencia.ConfiguracaoRa3;
import br.com.joinup.ra3.persistencia.PersistenciaRa3;
import java.io.PrintStream;
import java.util.Map;

public final class DiagnosticoRa3 {
    private DiagnosticoRa3() { }

    public static int executar(Map<String, String> ambiente, PrintStream saida, PrintStream erro) {
        saida.println("JoinUp RA3 — Entrega 00: diagnóstico de persistência; sem inserção automática.");
        ConfiguracaoRa3 configuracao;
        try {
            configuracao = ConfiguracaoRa3.doAmbiente(ambiente);
        } catch (IllegalArgumentException e) {
            erro.println("RA3_CONFIGURACAO_INVALIDA: " + e.getMessage());
            return 1;
        }
        try (var fabrica = PersistenciaRa3.abrir(configuracao)) {
            saida.println("RA3_CONEXAO_OK: EclipseLink conectado ao catálogo joinup_poo_ra3; DDL automático desabilitado.");
            saida.println("Conexão encerrada. Smoke test de escrita somente pelo perfil ra3-it.");
            return 0;
        } catch (RuntimeException e) {
            erro.println("RA3_CONEXAO_FALHOU: confira serviço MySQL, credenciais, permissões e schema manual.");
            return 1;
        }
    }
}
