package br.com.joinup.ra3;

import br.com.joinup.ra3.servico.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static br.com.joinup.ra3.servico.DadosRa3.*;
import static org.junit.jupiter.api.Assertions.*;

class DadosCrudRa3Test {
    @ParameterizedTest @ValueSource(strings = {"-1", "1.001", "1.000", "1000000"})
    void precoInvalidoEhRecusadoAntesDeQualquerTransacao(String valor) {
        assertThrows(IllegalArgumentException.class, () -> new NovoTipo("Pista", new BigDecimal(valor), 1, 1));
    }
    @Test void confirmaNegativaNaoAbreBancoEIdsSaoValidados() {
        var servico = new ServicoCrudRa3(null);
        assertFalse(servico.excluir(TipoRegistro.GENERO, 1, "NAO"));
        assertFalse(servico.excluir(TipoRegistro.POSTAGEM, 1, "sim"));
        assertThrows(IllegalArgumentException.class, () -> servico.excluir(TipoRegistro.GENERO, 0, "SIM"));
        assertThrows(IllegalArgumentException.class, () -> servico.listar(TipoRegistro.GENERO, 0));
    }
    @Test void camposInvalidosNaoGeramComandosParciais() {
        assertThrows(IllegalArgumentException.class, () -> new NovoGenero(" "));
        assertThrows(IllegalArgumentException.class, () -> new AlterarGenero(1, "a".repeat(46)));
        assertThrows(IllegalArgumentException.class, () -> new NovoTipo("T", BigDecimal.ONE, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> new AlterarTipo(1, "T", BigDecimal.ONE, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> new NovaComunidade("C", 0, LocalDateTime.now()));
        assertThrows(IllegalArgumentException.class, () -> new NovaPostagem("", null, 1, 1, LocalDateTime.now()));
        assertThrows(IllegalArgumentException.class, () -> new NovoEvento("E", LocalDateTime.now(), "D", "L", "C", "XX", "12345678", 1, 1));
    }
}
