package br.com.joinup.usuarios.domain;

/** Identificadores SQL vêm exclusivamente desta enumeração, nunca da requisição. */
public enum Perfil {
    PARTICIPANTE("participantes", "id_participante"),
    ORGANIZADOR("organizadores", "id_organizador"),
    SUPORTE("usuarios_suporte", "id_usuario_suporte");

    private final String tabela;
    private final String colunaId;
    Perfil(String tabela, String colunaId) { this.tabela = tabela; this.colunaId = colunaId; }
    public String tabela() { return tabela; }
    public String colunaId() { return colunaId; }
}
