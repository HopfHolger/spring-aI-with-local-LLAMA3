package it.gdorsi.repository.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Entity
@Table(name = "authors")
public class Autor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String bio;

    // Das Feld für die semantische Repräsentation der Bio oder Expertise
    @Column(columnDefinition = "vector(1024)") // Dimension abhängig vom Modell - nicht ändern!
    @JdbcTypeCode(SqlTypes.VECTOR) // Dies mappt das float[] korrekt auf pgvector
    private float[] authorEmbedding;

    // 1. Pflicht für JPA: Leerer Konstruktor
    public Autor() {
    }

    // 2. Dein benutzerdefinierter Konstruktor
    public Autor(String name, String bio, float[] authorEmbedding) {
        this.name = name;
        this.bio = bio;
        this.authorEmbedding = authorEmbedding;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBio() {
        return bio;
    }

    public float[] getAuthorEmbedding() {
        return authorEmbedding;
    }

    public void setAuthorEmbedding(float[] authorEmbedding) {
        this.authorEmbedding = authorEmbedding;
    }
}
