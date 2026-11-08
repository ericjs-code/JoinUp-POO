package br.com.joinup.ra3.servico;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/** Comandos imutáveis validados ANTES de abrir a transação. Não são entidades JPA. */
public final class DadosRa3 {
    private DadosRa3() { }
    public sealed interface Cadastro permits NovoGenero, NovoOrganizador, NovoParticipante, NovoEvento, NovoTipo, NovaComunidade, NovaPostagem { }
    public sealed interface Alteracao permits AlterarGenero, AlterarOrganizador, AlterarParticipante, AlterarEvento, AlterarTipo, AlterarComunidade, AlterarPostagem { int id(); }

    public record NovoGenero(String nome) implements Cadastro { public NovoGenero { nome = texto(nome, 45, true); } }
    public record NovoOrganizador(String nome, String email, String cnpj) implements Cadastro {
        public NovoOrganizador { nome = texto(nome, 80, true); email = texto(email, 40, true); cnpj = texto(cnpj, 18, true); }
    }
    public record NovoParticipante(String nome, String cpf, LocalDate nascimento, String email) implements Cadastro {
        public NovoParticipante {
            nome = texto(nome, 60, true); cpf = texto(cpf, 11, true); email = texto(email, 45, true);
            if (nascimento == null || !nascimento.toString().matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw new IllegalArgumentException("Nascimento deve usar yyyy-MM-dd.");
        }
    }
    public record NovoEvento(String nome, LocalDateTime data, String descricao, String localizacao,
                             String cidade, String uf, String cep, int organizadorId, int generoId) implements Cadastro {
        public NovoEvento {
            nome = texto(nome, 45, true); data = dataUtc(data); descricao = texto(descricao, 450, true);
            localizacao = texto(localizacao, 45, true); cidade = texto(cidade, 60, true); cep = texto(cep, 8, true);
            if (uf == null || !Set.of("AC","AL","AP","AM","BA","CE","DF","ES","GO","MA","MT","MS","MG","PA","PB","PR","PE","PI","RJ","RN","RS","RO","RR","SC","SP","SE","TO").contains(uf)) throw new IllegalArgumentException("UF inválida.");
            validarId(organizadorId); validarId(generoId);
        }
    }
    public record NovoTipo(String nome, BigDecimal valor, int quantidade, int eventoId) implements Cadastro {
        public NovoTipo { nome = texto(nome, 45, true); valor = dinheiro(valor); DadosRa3.quantidade(quantidade); validarId(eventoId); }
    }
    public record NovaComunidade(String nome, int eventoId, LocalDateTime data) implements Cadastro {
        public NovaComunidade { nome = texto(nome, 80, true); validarId(eventoId); data = dataUtc(data); }
    }
    public record NovaPostagem(String descricao, String imagem, int participanteId, int comunidadeId, LocalDateTime data) implements Cadastro {
        public NovaPostagem {
            descricao = texto(descricao, 500, false); imagem = texto(imagem, 255, false); conteudo(descricao, imagem);
            validarId(participanteId); validarId(comunidadeId); data = dataUtc(data);
        }
    }
    public record AlterarGenero(int id, String nome) implements Alteracao { public AlterarGenero { validarId(id); nome = texto(nome, 45, true); } }
    public record AlterarOrganizador(int id, String nome) implements Alteracao { public AlterarOrganizador { validarId(id); nome = texto(nome, 80, true); } }
    public record AlterarParticipante(int id, String nome) implements Alteracao { public AlterarParticipante { validarId(id); nome = texto(nome, 60, true); } }
    public record AlterarEvento(int id, String nome, String imagem) implements Alteracao {
        public AlterarEvento { validarId(id); nome = texto(nome, 45, true); imagem = texto(imagem, 255, false); }
    }
    public record AlterarTipo(int id, String nome, BigDecimal valor, int disponivel, int vendida) implements Alteracao {
        public AlterarTipo { validarId(id); nome = texto(nome, 45, true); valor = dinheiro(valor); quantidade(disponivel); quantidade(vendida); }
    }
    public record AlterarComunidade(int id, String nome, String descricao, String imagem) implements Alteracao {
        public AlterarComunidade { validarId(id); nome = texto(nome, 80, true); descricao = texto(descricao, 300, false); imagem = texto(imagem, 255, false); }
    }
    public record AlterarPostagem(int id, String descricao, String imagem) implements Alteracao {
        public AlterarPostagem { validarId(id); descricao = texto(descricao, 500, false); imagem = texto(imagem, 255, false); conteudo(descricao, imagem); }
    }

    public static void validarId(int id) { if (id < 1) throw new IllegalArgumentException("ID deve ser inteiro positivo."); }
    private static String texto(String s, int limite, boolean obrigatorio) {
        if (s == null || s.isBlank()) {
            if (obrigatorio) throw new IllegalArgumentException("Texto obrigatório não pode ficar em branco.");
            return null;
        }
        if (s.codePointCount(0, s.length()) > limite) throw new IllegalArgumentException("Texto excede o limite de " + limite + " caracteres.");
        return s;
    }
    private static LocalDateTime dataUtc(LocalDateTime data) {
        if (data == null || data.getYear() < 1000 || data.getYear() > 9999) throw new IllegalArgumentException("Data UTC fora do intervalo DATETIME.");
        return data.withNano(0);
    }
    private static BigDecimal dinheiro(BigDecimal valor) {
        if (valor == null || valor.scale() > 2 || valor.signum() < 0 || valor.compareTo(new BigDecimal("999999.99")) > 0) throw new IllegalArgumentException("Valor deve estar entre 0.00 e 999999.99, com até duas casas.");
        try { return valor.setScale(2, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Valor deve ter no máximo duas casas decimais."); }
    }
    private static void quantidade(int n) { if (n < 0) throw new IllegalArgumentException("Quantidade não pode ser negativa."); }
    private static void conteudo(String descricao, String imagem) {
        if (descricao == null && imagem == null) throw new IllegalArgumentException("Informe descrição e/ou imagem para a postagem.");
    }
}
