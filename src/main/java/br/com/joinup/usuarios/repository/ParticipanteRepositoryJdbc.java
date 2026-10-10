package br.com.joinup.usuarios.repository;

import br.com.joinup.usuarios.domain.*;
import java.sql.*;
import java.time.LocalDate;

public final class ParticipanteRepositoryJdbc extends UsuarioRepositoryJdbc {
    public ParticipanteRepositoryJdbc() { super(Perfil.PARTICIPANTE); }
    @Override protected Participante mapear(ResultSet rs) throws SQLException {
        return new Participante(rs.getLong("id_participante"), rs.getString("nome"), rs.getString("email"),
                rs.getString("senha"), rs.getBoolean("ativo"), rs.getString("cpf"), LocalDate.parse(rs.getString("data_nascimento")));
    }
    @Override public long salvar(Connection c, Usuario usuario) throws SQLException {
        var p = (Participante) usuario;
        return inserir(c, "INSERT INTO participantes(nome,email,senha,cpf,data_nascimento) VALUES(?,?,?,?,?)",
                p.nome(), p.email(), p.hashSenha(), p.cpf(), p.dataNascimento().toString());
    }
}
