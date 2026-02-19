package it.gdorsi.service;

import java.math.BigDecimal;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import it.gdorsi.dao.VertragRequest;
import it.gdorsi.repository.VertragRepository;
import it.gdorsi.repository.model.Vertrag;
import it.gdorsi.repository.model.VertragStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VertragTool implements VertragOperations {

    private final VertragRepository repository;
    private final EmbeddingModel embeddingModel;

    @Override
    @Transactional // Stellt Datenkonsistenz sicher
    public String saveContractFromPdf(VertragRequest req) {
        // 1. Bestehenden Vertrag suchen (Idempotenz) oder neuen erstellen
        Vertrag vertrag = repository.findByVertragsNummer(req.vertragsNummer())
                .orElse(new Vertrag());

        // 2. Felder mappen (8 Felder aus deinem JPA Model)
        vertrag.setVertragsNummer(req.vertragsNummer());
        vertrag.setKundeName(req.kundeName());
        vertrag.setStartDatum(req.startDatum());
        vertrag.setEndDatum(req.endDatum());
        vertrag.setBetrag(BigDecimal.valueOf(req.betrag()));
        vertrag.setVertragsTyp(req.vertragsTyp());
        vertrag.setStatus(req.status());
        vertrag.setBemerkung(req.bemerkung());

        // 3. Vektor erzeugen (Verhindert den Hibernate Null-Fehler!)
        // Wir nehmen wichtige Textfelder für das Embedding
        String textForAi = String.format("%s - %s: %s",
                req.kundeName(), req.vertragsTyp(), req.bemerkung());
        float[] vector = embeddingModel.embed(textForAi);
        vertrag.setVertragEmbedding(vector);

        // 4. Speichern
        repository.save(vertrag);

        return String.format("Vertrag %s für Kunde %s erfolgreich in der DB erfasst.",
                req.vertragsNummer(), req.kundeName());
    }

    @Override
    public String updateContractStatusOrAmount(String vertragsNummer, Double betrag, VertragStatus status) {
        return repository.findByVertragsNummer(vertragsNummer)
                .map(vertrag -> {
                    if (betrag != null) vertrag.setBetrag(BigDecimal.valueOf(betrag));
                    if (status != null) {
                        VertragStatus validatedStatus = VertragStatus.fromString(status.name());

                        // Nur wenn die KI einen ECHTEN Status (Aktiv/Entwurf/Gekündigt)
                        // gesendet hat, wird das Feld in der DB angefasst.
                        if (validatedStatus != null) {
                            vertrag.setStatus(validatedStatus);
                        }
                        // Falls validatedStatus null ist (z.B. bei "<keine Änderung>"),
                        // passiert hier NICHTS. Der alte Status bleibt erhalten.
                    }
                    repository.save(vertrag);
                    return "Vertrag " + vertragsNummer + " wurde erfolgreich aktualisiert.";
                })
                .orElse("Fehler: Vertrag mit Nummer " + vertragsNummer + " nicht gefunden.");
    }
}
