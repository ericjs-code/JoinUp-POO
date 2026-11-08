package br.com.joinup.ra3.modelo;

import jakarta.persistence.*;
import br.com.joinup.ra3.persistencia.DataHoraUtcConverter;
import java.time.LocalDateTime;

@Entity
@Table(name = "comunidades", catalog = "joinup_poo_ra3")
public class Comunidade {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comunidade", nullable = false)
    private Integer id;
    @Column(name = "nome", nullable = false, length = 80)
    private String nome;
    @Column(name = "descricao", length = 300)
    private String descricao;
    @Column(name = "imagem", length = 255)
    private String imagem;
    @OneToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "id_evento", nullable = false, unique = true)
    private Evento evento;
    @Column(name = "data_criacao", nullable = false)
    @Convert(converter = DataHoraUtcConverter.class)
    private LocalDateTime dataCriacao;

    protected Comunidade() { }

    public Comunidade(String nome, Evento evento, LocalDateTime criacaoUtc) {
        renomear(nome);
        this.evento = ValidacaoModelo.obrigatorio(evento, "Evento");
        this.dataCriacao = ValidacaoModelo.instanteUtc(criacaoUtc, "Criação UTC");
    }

    public void renomear(String nome) { this.nome = ValidacaoModelo.texto(nome, "Nome", 80, true); }
    public void definirDescricao(String descricao) { this.descricao = ValidacaoModelo.texto(descricao, "Descrição", 300, false); }
    public void definirImagem(String imagem) { this.imagem = ValidacaoModelo.texto(imagem, "Imagem", 255, false); }
    public Integer getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public String getImagem() { return imagem; }
    public Evento getEvento() { return evento; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
}
