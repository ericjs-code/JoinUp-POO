package br.com.joinup.ra3;

import br.com.joinup.ra3.apresentacao.FormularioRa3;
import br.com.joinup.ra3.servico.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import static br.com.joinup.ra3.apresentacao.FormularioRa3.Campo.*;
import static org.junit.jupiter.api.Assertions.*;

class FormularioRa3Test {
    private Map<FormularioRa3.Campo, String> valores() {
        var v = new EnumMap<FormularioRa3.Campo, String>(FormularioRa3.Campo.class);
        for (var c : FormularioRa3.Campo.values()) v.put(c, "1");
        v.put(NOME, " Teste "); v.put(DATA, "2030-11-12T18:30:00"); v.put(NASCIMENTO, "2000-02-29");
        v.put(UF, "SP"); v.put(VALOR, "12.50"); v.put(DESCRICAO, "Descrição"); v.put(IMAGEM, "");
        return v;
    }
    @ParameterizedTest @EnumSource(TipoRegistro.class)
    void seteCadastrosEAlteracoesUsamSomenteCamposExibidos(TipoRegistro tipo) {
        var v = valores();
        var criacao = new EnumMap<FormularioRa3.Campo, String>(FormularioRa3.Campo.class);
        FormularioRa3.campos(tipo, false).forEach(c -> criacao.put(c, v.get(c)));
        var cadastro = FormularioRa3.cadastro(tipo, criacao);
        var edicao = new EnumMap<FormularioRa3.Campo, String>(FormularioRa3.Campo.class);
        FormularioRa3.campos(tipo, true).forEach(c -> edicao.put(c, v.get(c)));
        var alteracao = FormularioRa3.alteracao(tipo, 42, edicao);
        assertEquals(42, alteracao.id());
        Class<?>[] cadastros = {DadosRa3.NovoGenero.class, DadosRa3.NovoOrganizador.class,
                DadosRa3.NovoParticipante.class, DadosRa3.NovoEvento.class, DadosRa3.NovoTipo.class,
                DadosRa3.NovaComunidade.class, DadosRa3.NovaPostagem.class};
        Class<?>[] alteracoes = {DadosRa3.AlterarGenero.class, DadosRa3.AlterarOrganizador.class,
                DadosRa3.AlterarParticipante.class, DadosRa3.AlterarEvento.class, DadosRa3.AlterarTipo.class,
                DadosRa3.AlterarComunidade.class, DadosRa3.AlterarPostagem.class};
        assertInstanceOf(cadastros[tipo.ordinal()], cadastro);
        assertInstanceOf(alteracoes[tipo.ordinal()], alteracao);
        assertFalse(FormularioRa3.campos(tipo, true).contains(EVENTO));
    }
    @ParameterizedTest @EnumSource(TipoRegistro.class)
    void formularioVazioNaoGeraComando(TipoRegistro tipo) {
        assertThrows(IllegalArgumentException.class, () -> FormularioRa3.cadastro(tipo, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> FormularioRa3.alteracao(tipo, 1, Map.of()));
    }
    @Test void conversoesValidasPreservamUtcZerosEReferencias() {
        assertEquals(LocalDateTime.of(2030, 11, 12, 18, 30), FormularioRa3.data("2030-11-12T18:30:00"));
        assertEquals(LocalDate.of(2000, 2, 29), FormularioRa3.nascimento("2000-02-29"));
        assertEquals(BigDecimal.ZERO, FormularioRa3.preco("0"));
        assertEquals(0, FormularioRa3.inteiro("0"));
        var evento = (DadosRa3.NovoEvento) FormularioRa3.cadastro(TipoRegistro.EVENTO, valores());
        assertEquals(1, evento.organizadorId()); assertEquals(1, evento.generoId());
        var comunidade = (DadosRa3.AlterarComunidade) FormularioRa3.alteracao(TipoRegistro.COMUNIDADE, 1, valores());
        assertNull(comunidade.imagem());
        assertEquals("SIM", FormularioRa3.confirmacao(true)); assertEquals("NAO", FormularioRa3.confirmacao(false));
    }
    @ParameterizedTest @ValueSource(strings={"", "-1", "1.0", "abc", "2147483648", "0"})
    void idsInvalidos(String s) { assertThrows(IllegalArgumentException.class, () -> FormularioRa3.id(s)); }
    @ParameterizedTest @ValueSource(strings={"", "1,20", "1.234", "-1", "1e2", "1000000"})
    void precosInvalidos(String s) { assertThrows(IllegalArgumentException.class, () -> FormularioRa3.preco(s)); }
    @ParameterizedTest @ValueSource(strings={"", "2030-02-30T18:30:00", "2030-11-12T18:30", "2030-11-12T18:30:00Z", "0999-11-12T18:30:00"})
    void datasInvalidas(String s) { assertThrows(IllegalArgumentException.class, () -> FormularioRa3.data(s)); }
    @ParameterizedTest @ValueSource(strings={"", "2001-02-29", "2000-2-29", "2000-01-01T00:00:00"})
    void nascimentosInvalidos(String s) { assertThrows(IllegalArgumentException.class, () -> FormularioRa3.nascimento(s)); }
}
