package it.gdorsi.service;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

@Component
public class PdfSanitizer {

    public List<Document> sanitize(List<Document> documents) {

        // Sanitize: Ungültige Unicode-Zeichen entfernen
        return documents.stream()
                .map(doc -> new Document(
                        doc.getId(),
                        sanitizeText(doc.getText()),
                        doc.getMetadata()))
                .toList();
    }

    private String sanitizeText(String text) {
        if (text == null) return null;
        // Entfernt alle Zeichen, die nicht in die Basic Multilingual Plane passen
        // und SQL-Probleme verursachen könnten (Surrogates)
        return text.replaceAll("[\\uD800-\\uDFFF]", "");
    }
}
