package br.com.joinup.ra3.modelo;

import jakarta.persistence.*;
import br.com.joinup.ra3.persistencia.DataHoraUtcConverter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "eventos", catalog = "joinup_poo_ra3")
public class Evento {
    private static final Set<String> UFS = Set.of("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT",
            "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento", nullable = false)
    private Integer id;
    @Column(name = "nome", nullable = false, length = 45)
    private String nome;
    @Column(name = "data", nullable = false)
    @Convert(converter = DataHoraUtcConverter.class)
    private LocalDateTime data;
    @Column(name = "descricao", nullable = false, length = 450)
    private String descricao;
    @Column(name = "imagem", length = 255)
    private String imagem;
    @Column(name = "localizacao", nullable = false, length = 45)
    private String localizacao;
    @Column(name = "cidade", nullable = false, length = 60)
    private String cidade;
    @Column(name = "estado", nullable = false, length = 2)
    private String estado;
    @Column(name = "cep", nullable = false, length = 8)
    private String cep;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "id_organizador", nullable = false)
    private Organizador organizador;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "id_genero_musical", nullable = false)
    private GeneroMusical generoMusical;
    @OneToMany(mappedBy = "evento", fetch = FetchType.EAGER)
    private List<TipoIngresso> tiposIngressos = new ArrayList<>();

    protected Evento() { }

    public Evento(String nome, LocalDateTime dataUtc, String descricao, String localizacao,
                  String cidade, String estado, String cep, Organizador organizador, GeneroMusical generoMusical) {
        renomear(nome);
        this.data = ValidacaoModelo.instanteUtc(dataUtc, "Data UTC");
        this.descricao = ValidacaoModelo.texto(descricao, "Descrição", 450, true);
        this.localizacao = ValidacaoModelo.texto(localizacao, "Localização", 45, true);
        this.cidade = ValidacaoModelo.texto(cidade, "Cidade", 60, true);
        if (!UFS.contains(ValidacaoModelo.obrigatorio(estado, "UF"))) throw new IllegalArgumentException("UF inválida.");
        this.estado = estado;
        this.cep = ValidacaoModelo.texto(cep, "CEP", 8, true);
        this.organizador = ValidacaoModelo.obrigatorio(organizador, "Organizador");
        this.generoMusical = ValidacaoModelo.obrigatorio(generoMusical, "Gênero musical");
    }

    void associarTipo(TipoIngresso tipo) {
        if (tipo.getEvento() != this) throw new IllegalArgumentException("Tipo pertence a outro evento.");
        if (!tiposIngressos.contains(tipo)) tiposIngressos.add(tipo);
    }

    public void renomear(String nome) { this.nome = ValidacaoModelo.texto(nome, "Nome", 45, true); }
    public void definirImagem(String imagem) { this.imagem = ValidacaoModelo.texto(imagem, "Imagem", 255, false); }
    public Integer getId() { return id; }
    public String getNome() { return nome; }
    public LocalDateTime getData() { return data; }
    public String getDescricao() { return descricao; }
    public String getImagem() { return imagem; }
    public String getLocalizacao() { return localizacao; }
    public String getCidade() { return cidade; }
    public String getEstado() { return estado; }
    public String getCep() { return cep; }
    public Organizador getOrganizador() { return organizador; }
    public GeneroMusical getGeneroMusical() { return generoMusical; }
    public List<TipoIngresso> getTiposIngressos() { return Collections.unmodifiableList(tiposIngressos); }
}
