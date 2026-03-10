package it.gdorsi.repository.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.math.BigDecimal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import it.gdorsi.repository.model.type.NullSafeVectorType;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Vertrag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String vertragsNummer;
    private String kundeName;
    private LocalDate startDatum;
    private LocalDate endDatum;
    private BigDecimal betrag;
    private String vertragsTyp;

    @Enumerated(EnumType.STRING) // Speichert "AKTIV", "ENTWURF" etc. als String in der DB
    private VertragStatus status = VertragStatus.ENTWURF; // Default Wert

    @Column(columnDefinition = "TEXT")
    private String bemerkung;

    @Type(NullSafeVectorType.class)
    @Column(name = "vertrag_embedding", columnDefinition = "vector(1024)")
    private float[] vertragEmbedding = new float[0];
}

