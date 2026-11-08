package br.com.joinup.ra3.servico;

import br.com.joinup.ra3.modelo.*;

/** Identificadores fechados: nomes de JPQL nunca vêm do texto digitado. */
public enum TipoRegistro {
    GENERO("Gêneros musicais", GeneroMusical.class), ORGANIZADOR("Organizadores", Organizador.class),
    PARTICIPANTE("Participantes", Participante.class), EVENTO("Eventos", Evento.class),
    TIPO_INGRESSO("Tipos de ingresso", TipoIngresso.class), COMUNIDADE("Comunidades", Comunidade.class),
    POSTAGEM("Postagens", Postagem.class);

    private final String titulo;
    private final Class<?> entidade;
    TipoRegistro(String titulo, Class<?> entidade) { this.titulo = titulo; this.entidade = entidade; }
    public String titulo() { return titulo; }
    public Class<?> entidade() { return entidade; }
    public String nomeJpa() { return entidade.getSimpleName(); }
}
