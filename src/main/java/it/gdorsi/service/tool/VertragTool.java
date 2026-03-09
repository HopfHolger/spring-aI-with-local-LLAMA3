package it.gdorsi.service.tool;

import java.math.BigDecimal;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import it.gdorsi.dao.VertragRequest;
import it.gdorsi.repository.VertragRepository;
import it.gdorsi.repository.model.Vertrag;
import it.gdorsi.repository.model.VertragStatus;
import it.gdorsi.service.VertragOperations;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class VertragTool implements VertragOperations {

    private final VertragRepository repository;
    private final EmbeddingModel embeddingModel;

    @Override
    @Transactional
    public String saveContractFromPdf(VertragRequest req) {
        if (req == null) {
            return "Fehler: VertragRequest ist null.";
        }
        if (req.vertragsNummer() == null || req.vertragsNummer().isBlank()) {
            return "Fehler: Vertragsnummer darf nicht leer sein.";
        }
        
        try {
            Vertrag vertrag = repository.findByVertragsNummer(req.vertragsNummer())
                    .orElse(new Vertrag());

            vertrag.setVertragsNummer(req.vertragsNummer());
            vertrag.setKundeName(req.kundeName());
            vertrag.setStartDatum(req.startDatum());
            vertrag.setEndDatum(req.endDatum());
            vertrag.setBetrag(req.betrag() != null ? BigDecimal.valueOf(req.betrag()) : null);
            vertrag.setVertragsTyp(req.vertragsTyp());
            vertrag.setStatus(req.status());
            vertrag.setBemerkung(req.bemerkung());

            String textForAi = String.format("%s - %s: %s",
                    req.kundeName() != null ? req.kundeName() : "",
                    req.vertragsTyp() != null ? req.vertragsTyp() : "",
                    req.bemerkung() != null ? req.bemerkung() : "");
            float[] vector = embeddingModel.embed(textForAi);
            vertrag.setVertragEmbedding(vector);

            repository.save(vertrag);

            return String.format("Vertrag %s für Kunde %s erfolgreich in der DB erfasst.",
                    req.vertragsNummer(), req.kundeName());
        } catch (Exception e) {
            log.error("Fehler beim Speichern des Vertrags {}: {}", req.vertragsNummer(), e.getMessage(), e);
            return "Fehler beim Speichern des Vertrags: " + e.getMessage();
        }
    }

    @Override
    @Transactional
    public String updateContractStatusOrAmount(String vertragsNummer, Double betrag, VertragStatus status) {
        if (vertragsNummer == null || vertragsNummer.isBlank()) {
            return "Fehler: Vertragsnummer darf nicht leer sein.";
        }
        
        try {
            return repository.findByVertragsNummer(vertragsNummer)
                    .map(vertrag -> {
                        if (betrag != null) vertrag.setBetrag(BigDecimal.valueOf(betrag));
                        if (status != null) {
                            VertragStatus validatedStatus = VertragStatus.fromString(status.name());

                            if (validatedStatus != null) {
                                vertrag.setStatus(validatedStatus);
                            }
                        }
                        repository.save(vertrag);
                        return "Vertrag " + vertragsNummer + " wurde erfolgreich aktualisiert.";
                    })
                    .orElse("Fehler: Vertrag mit Nummer " + vertragsNummer + " nicht gefunden.");
        } catch (Exception e) {
            log.error("Fehler beim Aktualisieren des Vertrags {}: {}", vertragsNummer, e.getMessage(), e);
            return "Fehler beim Aktualisieren des Vertrags: " + e.getMessage();
        }
    }
}
