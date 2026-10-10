package br.com.joinup.shared.db;

import br.com.joinup.config.ConfiguracaoSistema;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Abre uma conexão nova por operação. O chamador é responsável por fechá-la. */
public final class ConnectionFactory {
    private final String url;
    private final String usuario;
    private final String senha;

    public ConnectionFactory(ConfiguracaoSistema configuracao) {
        this.url = configuracao.getUrlJdbc();
        this.usuario = configuracao.getUsuarioBanco();
        this.senha = configuracao.getSenhaBanco();
    }

    public Connection abrirConexao() throws SQLException {
        if (usuario.isBlank()) {
            throw new SQLException("Configure o usuário JDBC antes de acessar o banco");
        }
        return DriverManager.getConnection(url, usuario, senha);
    }
}
