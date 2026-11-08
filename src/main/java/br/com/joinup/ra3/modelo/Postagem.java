package br.com.joinup.ra3.modelo;

import jakarta.persistence.*;
import br.com.joinup.ra3.persistencia.DataHoraUtcConverter;
import java.time.LocalDateTime;

@Entity
@Table(name = "postagens", catalog = "joinup_poo_ra3")
public class Postagem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_postagem", nullable = false)
    private Integer id;
    @Column(name = "descricao", length = 500)
    private String descricao;
    @Column(name = "imagem", length = 255)
    private String imagem;
    @Column(name = "curtidas")
    private Integer curtidas = 0;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "id_participante", nullable = false)
    private Participante participante;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "id_comunidade", nullable = false)
    private Comunidade comunidade;
    @Column(name = "data_postagem", nullable = false)
    @Convert(converter = DataHoraUtcConverter.class)
    private LocalDateTime dataPostagem;

    protected Postagem() { }

    public Postagem(String descricao, String imagem, Participante participante, Comunidade comunidade, LocalDateTime postagemUtc) {
        alterarConteudo(descricao, imagem);
        this.participante = ValidacaoModelo.obrigatorio(participante, "Participante");
        this.comunidade = ValidacaoModelo.obrigatorio(comunidade, "Comunidade");
        this.dataPostagem = ValidacaoModelo.instanteUtc(postagemUtc, "Postagem UTC");
    }

    public void alterarConteudo(String descricao, String imagem) {
        String texto = ValidacaoModelo.texto(descricao, "Descrição", 500, false);
        String referencia = ValidacaoModelo.texto(imagem, "Imagem", 255, false);
        this.descricao = texto;
        this.imagem = referencia;
    }

    public Integer getId() { return id; }
    public String getDescricao() { return descricao; }
    public String getImagem() { return imagem; }
    public Integer getCurtidas() { return curtidas; }
    public Participante getParticipante() { return participante; }
    public Comunidade getComunidade() { return comunidade; }
    public LocalDateTime getDataPostagem() { return dataPostagem; }
}
