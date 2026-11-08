package br.com.joinup.ra3.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "generos_musicais", catalog = "joinup_poo_ra3")
public class GeneroMusical {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_genero_musical", nullable = false)
    private Integer id;

    @Column(name = "nome", nullable = false, length = 45)
    private String nome;

    protected GeneroMusical() { /* Acesso por campo pelo JPA. */ }

    public GeneroMusical(String nome) { renomear(nome); }

    public Integer getId() { return id; }
    public String getNome() { return nome; }

    public void renomear(String nome) {
        if (nome == null || nome.isBlank() || nome.codePointCount(0, nome.length()) > 45) {
            throw new IllegalArgumentException("Nome do gênero deve conter de 1 a 45 caracteres.");
        }
        this.nome = nome;
    }
}
