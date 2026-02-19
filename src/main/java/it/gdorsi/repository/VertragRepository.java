package it.gdorsi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

import it.gdorsi.repository.model.Vertrag;

public interface VertragRepository extends JpaRepository<Vertrag, Long> {

    // Findet einen Vertrag über die eindeutige Nummer (wichtig für Updates/Tools)
    Optional<Vertrag> findByVertragsNummer(String vertragsNummer);

    // KI-Suche: Findet die ähnlichsten Verträge basierend auf dem Vektor (pgvector)
    // <=> ist der Cosinus-Ähnlichkeits-Operator von pgvector
    @Query(value = "SELECT * FROM vertrag ORDER BY vertrag_embedding <=> cast(:vector as vector) LIMIT :limit", nativeQuery = true)
    List<Vertrag> findSimilarContracts(@Param("vector") float[] vector, @Param("limit") int limit);

    // Sortierte Liste für dein HTMX-Dashboard
    List<Vertrag> findAllByOrderByStartDatumDesc();
}

