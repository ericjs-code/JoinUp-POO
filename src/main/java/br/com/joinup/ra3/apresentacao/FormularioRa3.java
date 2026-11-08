package br.com.joinup.ra3.apresentacao;

import br.com.joinup.ra3.servico.TipoRegistro;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import static br.com.joinup.ra3.servico.DadosRa3.*;

/** Descrição e conversão dos formulários, sem dependência do runtime gráfico. */
public final class FormularioRa3 {
    private FormularioRa3() { }
    public enum Campo {
        NOME("Nome"), EMAIL("E-mail fictício"), CNPJ("CNPJ fictício (até 18 caracteres)"),
        CPF("CPF fictício (até 11 caracteres)"), NASCIMENTO("Nascimento (yyyy-MM-dd)"),
        DATA("Data UTC (yyyy-MM-ddTHH:mm:ss)"), DESCRICAO("Descrição", true),
        LOCAL("Localização"), CIDADE("Cidade"), UF("UF (ex.: SP)"), CEP("CEP fictício (até 8 caracteres)"),
        ORGANIZADOR("ID do Organizador"), GENERO("ID do GeneroMusical"), EVENTO("ID do Evento"),
        PARTICIPANTE("ID do Participante"), COMUNIDADE("ID da Comunidade"),
        VALOR("Valor (ponto, até 2 casas)"), DISPONIVEL("Quantidade disponível"), VENDIDA("Quantidade vendida"),
        IMAGEM("Imagem (referência opcional)");
        private final String rotulo;
        private final boolean longo;
        Campo(String rotulo) { this(rotulo, false); }
        Campo(String rotulo, boolean longo) { this.rotulo = rotulo; this.longo = longo; }
        public String rotulo() { return rotulo; }
        public boolean longo() { return longo; }
    }

    public static List<Campo> campos(TipoRegistro tipo, boolean alterar) {
        if (alterar) return switch (tipo) {
            case GENERO, ORGANIZADOR, PARTICIPANTE -> List.of(Campo.NOME);
            case EVENTO -> List.of(Campo.NOME, Campo.IMAGEM);
            case TIPO_INGRESSO -> List.of(Campo.NOME, Campo.VALOR, Campo.DISPONIVEL, Campo.VENDIDA);
            case COMUNIDADE -> List.of(Campo.NOME, Campo.DESCRICAO, Campo.IMAGEM);
            case POSTAGEM -> List.of(Campo.DESCRICAO, Campo.IMAGEM);
        };
        return switch (tipo) {
            case GENERO -> List.of(Campo.NOME);
            case ORGANIZADOR -> List.of(Campo.NOME, Campo.EMAIL, Campo.CNPJ);
            case PARTICIPANTE -> List.of(Campo.NOME, Campo.CPF, Campo.NASCIMENTO, Campo.EMAIL);
            case EVENTO -> List.of(Campo.NOME, Campo.DATA, Campo.DESCRICAO, Campo.LOCAL, Campo.CIDADE,
                    Campo.UF, Campo.CEP, Campo.ORGANIZADOR, Campo.GENERO);
            case TIPO_INGRESSO -> List.of(Campo.NOME, Campo.VALOR, Campo.DISPONIVEL, Campo.EVENTO);
            case COMUNIDADE -> List.of(Campo.NOME, Campo.EVENTO, Campo.DATA);
            case POSTAGEM -> List.of(Campo.DESCRICAO, Campo.IMAGEM, Campo.PARTICIPANTE, Campo.COMUNIDADE, Campo.DATA);
        };
    }

    public static Cadastro cadastro(TipoRegistro tipo, Map<Campo, String> v) {
        return switch (tipo) {
            case GENERO -> new NovoGenero(texto(v, Campo.NOME));
            case ORGANIZADOR -> new NovoOrganizador(texto(v, Campo.NOME), texto(v, Campo.EMAIL), texto(v, Campo.CNPJ));
            case PARTICIPANTE -> new NovoParticipante(texto(v, Campo.NOME), texto(v, Campo.CPF), nascimento(texto(v, Campo.NASCIMENTO)), texto(v, Campo.EMAIL));
            case EVENTO -> new NovoEvento(texto(v, Campo.NOME), data(texto(v, Campo.DATA)), texto(v, Campo.DESCRICAO),
                    texto(v, Campo.LOCAL), texto(v, Campo.CIDADE), texto(v, Campo.UF), texto(v, Campo.CEP),
                    id(texto(v, Campo.ORGANIZADOR)), id(texto(v, Campo.GENERO)));
            case TIPO_INGRESSO -> new NovoTipo(texto(v, Campo.NOME), preco(texto(v, Campo.VALOR)), inteiro(texto(v, Campo.DISPONIVEL)), id(texto(v, Campo.EVENTO)));
            case COMUNIDADE -> new NovaComunidade(texto(v, Campo.NOME), id(texto(v, Campo.EVENTO)), data(texto(v, Campo.DATA)));
            case POSTAGEM -> new NovaPostagem(texto(v, Campo.DESCRICAO), texto(v, Campo.IMAGEM), id(texto(v, Campo.PARTICIPANTE)), id(texto(v, Campo.COMUNIDADE)), data(texto(v, Campo.DATA)));
        };
    }
    public static Alteracao alteracao(TipoRegistro tipo, int id, Map<Campo, String> v) {
        return switch (tipo) {
            case GENERO -> new AlterarGenero(id, texto(v, Campo.NOME));
            case ORGANIZADOR -> new AlterarOrganizador(id, texto(v, Campo.NOME));
            case PARTICIPANTE -> new AlterarParticipante(id, texto(v, Campo.NOME));
            case EVENTO -> new AlterarEvento(id, texto(v, Campo.NOME), texto(v, Campo.IMAGEM));
            case TIPO_INGRESSO -> new AlterarTipo(id, texto(v, Campo.NOME), preco(texto(v, Campo.VALOR)), inteiro(texto(v, Campo.DISPONIVEL)), inteiro(texto(v, Campo.VENDIDA)));
            case COMUNIDADE -> new AlterarComunidade(id, texto(v, Campo.NOME), texto(v, Campo.DESCRICAO), texto(v, Campo.IMAGEM));
            case POSTAGEM -> new AlterarPostagem(id, texto(v, Campo.DESCRICAO), texto(v, Campo.IMAGEM));
        };
    }
    private static String texto(Map<Campo, String> v, Campo c) { return v.getOrDefault(c, "").strip(); }
    public static int inteiro(String s) {
        try {
            if (s == null || !s.strip().matches("[0-9]+")) throw new NumberFormatException();
            return Integer.parseInt(s.strip());
        } catch (NumberFormatException e) { throw new IllegalArgumentException("Informe inteiro entre 0 e 2147483647."); }
    }
    public static int id(String s) { int id = inteiro(s); validarId(id); return id; }
    public static BigDecimal preco(String s) {
        if (s == null || !s.matches("[0-9]+(?:\\.[0-9]{1,2})?")) throw new IllegalArgumentException("Valor inválido. Use ponto e até duas casas decimais.");
        var valor = new BigDecimal(s);
        if (valor.compareTo(new BigDecimal("999999.99")) > 0) throw new IllegalArgumentException("Valor máximo: 999999.99.");
        return valor;
    }
    public static LocalDate nascimento(String s) {
        try {
            if (s == null || !s.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw new IllegalArgumentException();
            return LocalDate.parse(s);
        } catch (IllegalArgumentException | DateTimeParseException e) { throw new IllegalArgumentException("Nascimento inválido. Use yyyy-MM-dd."); }
    }
    public static LocalDateTime data(String s) {
        try {
            if (s == null || !s.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}")) throw new IllegalArgumentException();
            var data = LocalDateTime.parse(s);
            if (data.getYear() < 1000) throw new IllegalArgumentException();
            return data;
        } catch (IllegalArgumentException | DateTimeParseException e) { throw new IllegalArgumentException("Data UTC inválida. Exemplo: 2030-11-12T18:30:00."); }
    }
    public static String confirmacao(boolean sim) { return sim ? "SIM" : "NAO"; }
}
