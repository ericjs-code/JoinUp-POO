package br.com.joinup.ra3.modelo;

import jakarta.persistence.*;
import br.com.joinup.ra3.persistencia.DataHoraUtcConverter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "participantes", catalog = "joinup_poo_ra3")
public class Participante {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_participante", nullable = false)
    private Integer id;
    @Column(name = "cpf", nullable = false, unique = true, length = 11, columnDefinition = "CHAR(11)")
    private String cpf;
    @Column(name = "nome", nullable = false, length = 60)
    private String nome;
    @Column(name = "data_nascimento", nullable = false, length = 14, columnDefinition = "CHAR(14)")
    private String nascimentoIso;
    @Column(name = "email", nullable = false, unique = true, length = 45)
    private String email;
    @Column(name = "senha", nullable = false, length = 255)
    private String senhaHash;
    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;
    @Column(name = "data_desativacao")
    @Convert(converter = DataHoraUtcConverter.class)
    private LocalDateTime dataDesativacao;

    protected Participante() { }

    public Participante(String nome, String cpf, LocalDate nascimento, String email, String senhaHash) {
        renomear(nome);
        this.cpf = ValidacaoModelo.texto(cpf, "CPF", 11, true);
        String iso = ValidacaoModelo.obrigatorio(nascimento, "Nascimento").toString();
        // Contrato da RA2: ISO yyyy-MM-dd em CHAR(14), sem transformar a coluna em DATE.
        if (!iso.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw new IllegalArgumentException("Nascimento fora do formato yyyy-MM-dd.");
        this.nascimentoIso = iso;
        this.email = ValidacaoModelo.texto(email, "E-mail", 45, true);
        this.senhaHash = ValidacaoModelo.texto(senhaHash, "Hash de senha", 255, true);
    }

    public void renomear(String nome) { this.nome = ValidacaoModelo.texto(nome, "Nome", 60, true); }
    public Integer getId() { return id; }
    public String getNome() { return nome; }
    public String getCpf() { return cpf; }
    public LocalDate getDataNascimento() { return LocalDate.parse(nascimentoIso.stripTrailing()); }
    public String getEmail() { return email; }
    public boolean isAtivo() { return ativo; }
    public LocalDateTime getDataDesativacao() { return dataDesativacao; }
}
