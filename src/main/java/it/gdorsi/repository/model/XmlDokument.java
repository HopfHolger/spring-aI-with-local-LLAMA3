package it.gdorsi.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "xml_dokument")
public class XmlDokument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String dateiname;

    @Column(columnDefinition = "TEXT")
    private String inhalt;

    @Column(name = "xml_embedding", columnDefinition = "vector(1024)")
    @JdbcTypeCode(SqlTypes.VECTOR)
    private float[] xmlEmbedding;

    @ManyToOne
    @JoinColumn(name = "autor_id", nullable = false)
    private Autor autor;

    public XmlDokument() {
    }

    public XmlDokument(String dateiname, String inhalt, float[] xmlEmbedding, Autor autor) {
        this.dateiname = dateiname;
        this.inhalt = inhalt;
        this.xmlEmbedding = xmlEmbedding;
        this.autor = autor;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDateiname() {
        return dateiname;
    }

    public void setDateiname(String dateiname) {
        this.dateiname = dateiname;
    }

    public String getInhalt() {
        return inhalt;
    }

    public void setInhalt(String inhalt) {
        this.inhalt = inhalt;
    }

    public float[] getXmlEmbedding() {
        return xmlEmbedding;
    }

    public void setXmlEmbedding(float[] xmlEmbedding) {
        this.xmlEmbedding = xmlEmbedding;
    }

    public Autor getAutor() {
        return autor;
    }

    public void setAutor(Autor autor) {
        this.autor = autor;
    }
}
