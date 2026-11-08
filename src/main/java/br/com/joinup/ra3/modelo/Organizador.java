package br.com.joinup.ra3.modelo;

import jakarta.persistence.*;
import br.com.joinup.ra3.persistencia.DataHoraUtcConverter;
import java.time.LocalDateTime;

@Entity
@Table(name = "organizadores", catalog = "joinup_poo_ra3")
public class Organizador {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_organizador", nullable = false)
    private Integer id;
    @Column(name = "nome", nullable = false, length = 80)
    private String nome;
    @Column(name = "email", nullable = false, unique = true, length = 40)
    private String email;
    @Column(name = "senha", nullable = false, length = 255)
    private String senhaHash;
    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;
    @Column(name = "data_desativacao")
    @Convert(converter = DataHoraUtcConverter.class)
    private LocalDateTime dataDesativacao;
    @Column(name = "cnpj", nullable = false, unique = true, length = 18, columnDefinition = "CHAR(18)")
    private String cnpj;

    protected Organizador() { }

    /** Recebe hash já produzido fora do modelo; esta entrega não implementa autenticação. */
    public Organizador(String nome, String email, String senhaHash, String cnpj) {
        renomear(nome);
        this.email = ValidacaoModelo.texto(email, "E-mail", 40, true);
        this.senhaHash = ValidacaoModelo.texto(senhaHash, "Hash de senha", 255, true);
        this.cnpj = ValidacaoModelo.texto(cnpj, "CNPJ", 18, true);
    }

    public void renomear(String nome) { this.nome = ValidacaoModelo.texto(nome, "Nome", 80, true); }
    public Integer getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getCnpj() { return cnpj; }
    public boolean isAtivo() { return ativo; }
    public LocalDateTime getDataDesativacao() { return dataDesativacao; }
    // Sem getter público para hash, setter de ID ou toString com informações pessoais.
}
