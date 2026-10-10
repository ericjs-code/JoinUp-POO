package br.com.joinup.shared.db;

import br.com.joinup.http.HttpException;
import java.sql.Connection;
import java.sql.SQLException;

/** Uma conexão por unidade de trabalho, fechamento inclusive após rollback. */
public final class ExecutorTransacional {
    private final ConnectionFactory conexoes;
    public ExecutorTransacional(ConnectionFactory conexoes) { this.conexoes = conexoes; }
    @FunctionalInterface public interface Operacao<T> { T executar(Connection conexao) throws SQLException; }
    public <T> T executar(Operacao<T> operacao) {
        try (Connection c = conexoes.abrirConexao()) {
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            c.setAutoCommit(false);
            try {
                T resultado = operacao.executar(c);
                c.commit();
                return resultado;
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) throw new HttpException(409, "CADASTRO_DUPLICADO", "E-mail ou documento já cadastrado");
            // Detalhes JDBC e valores pessoais não são publicados nem registrados.
            throw new HttpException(503, "BANCO_INDISPONIVEL", "Não foi possível concluir a operação no banco");
        }
    }
}
