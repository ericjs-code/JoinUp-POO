package br.com.joinup.ra3.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "tipos_ingressos", catalog = "joinup_poo_ra3")
public class TipoIngresso {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipos_ingressos", nullable = false)
    private Integer id;
    @Column(name = "nome_tipo", nullable = false, length = 45)
    private String nome;
    @Column(name = "valor", nullable = false, precision = 8, scale = 2)
    private BigDecimal valor;
    @Column(name = "quantidade_disponivel", nullable = false)
    private int quantidadeDisponivel;
    @Column(name = "quantidade_vendida", nullable = false)
    private int quantidadeVendida;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "id_evento", nullable = false)
    private Evento evento;

    protected TipoIngresso() { }

    public TipoIngresso(String nome, BigDecimal valor, int quantidadeDisponivel, Evento evento) {
        renomear(nome);
        definirValor(valor);
        definirQuantidades(quantidadeDisponivel, 0);
        this.evento = ValidacaoModelo.obrigatorio(evento, "Evento");
        evento.associarTipo(this);
    }

    public void renomear(String nome) { this.nome = ValidacaoModelo.texto(nome, "Nome do tipo", 45, true); }

    public void definirValor(BigDecimal valor) {
        ValidacaoModelo.obrigatorio(valor, "Valor");
        if (valor.signum() < 0 || valor.compareTo(new BigDecimal("999999.99")) > 0) {
            throw new IllegalArgumentException("Valor fora do intervalo DECIMAL(8,2).");
        }
        try { this.valor = valor.setScale(2, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Valor exige no máximo duas casas decimais."); }
    }

    /** Somente limites estruturais; não implementa venda ou controle concorrente de estoque. */
    public void definirQuantidades(int disponivel, int vendida) {
        if (disponivel < 0 || vendida < 0) throw new IllegalArgumentException("Quantidades não podem ser negativas.");
        this.quantidadeDisponivel = disponivel;
        this.quantidadeVendida = vendida;
    }

    public Integer getId() { return id; }
    public String getNome() { return nome; }
    public BigDecimal getValor() { return valor; }
    public int getQuantidadeDisponivel() { return quantidadeDisponivel; }
    public int getQuantidadeVendida() { return quantidadeVendida; }
    public Evento getEvento() { return evento; }
}
