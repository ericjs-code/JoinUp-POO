package br.com.joinup.eventos.service;

import br.com.joinup.eventos.domain.*;
import br.com.joinup.eventos.repository.ComunidadeRepositoryJdbc;
import java.sql.*;

public final class ComunidadeService {
    private final ComunidadeRepositoryJdbc comunidades;
    public ComunidadeService(ComunidadeRepositoryJdbc comunidades) { this.comunidades=comunidades; }
    public long criarParaEvento(Connection c,Evento evento) throws SQLException { return comunidades.criar(c,evento); }
    public Comunidade buscar(Connection c,long evento) throws SQLException { return comunidades.buscar(c,evento); }
}
