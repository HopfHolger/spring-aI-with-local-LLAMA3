package it.gdorsi.dao;

import java.time.LocalDate;

import it.gdorsi.repository.model.VertragStatus;

public record VertragRequest(
        String vertragsNummer,
        String kundeName,
        LocalDate startDatum,
        LocalDate endDatum,
        Double betrag,
        String vertragsTyp, // z.B. Service, Wartung, Mietkauf
        VertragStatus status,      // Aktiv, Entwurf, Gekündigt
        String bemerkung
) {}
