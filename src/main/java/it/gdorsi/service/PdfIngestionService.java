package it.gdorsi.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.gdorsi.dao.PdfIngestResult;
import jakarta.annotation.PostConstruct;

/**
 * Ingest KI (oft auch Data Ingestion für KI) bezeichnet den automatisierten
 * Prozess des Sammelns, Bereinigens, Strukturierens und Einspielens
 * von Daten (Dateien, Dokumente, Datenbanken) in ein KI-System oder eine Datenbank.
 */
@Service
public class PdfIngestionService {

    private static final Logger log = LoggerFactory.getLogger(PdfIngestionService.class);

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    public PdfIngestionService(VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public PdfIngestResult loadPdf(Resource pdfResource) {
        long startTime = System.currentTimeMillis();

        if (pdfResource == null || pdfResource.getFilename() == null) {
            throw new IllegalArgumentException("PDF Resource oder Dateiname darf nicht null sein.");
        }

        String fileName = pdfResource.getFilename();

        try {
            final TikaDocumentReader tikaReader = new TikaDocumentReader(pdfResource);

            final TokenTextSplitter splitter = TokenTextSplitter.builder()
                    .withChunkSize(300)
                    .withMaxNumChunks(5000)
                    .withKeepSeparator(true)
                    .build();

            List<Document> documents = splitter.apply(tikaReader.get());
            if (documents.isEmpty()) {
                log.warn("Keine Texte im PDF gefunden: {}", fileName);
            }

            List<String> currentChunkIds = new ArrayList<>();
            List<Document> newDocuments = new ArrayList<>();
            List<Document> updateDocuments = new ArrayList<>();

            Set<String> existingIds = fetchExistingIds(documents);

            for (Document doc : documents) {
                doc.getMetadata().put("file_name", fileName);
                doc.getMetadata().put("ingested_at", System.currentTimeMillis());

                String customId = fileName + (doc.getText() != null ? doc.getText().hashCode() : 0);
                final UUID deterministicId = UUID.nameUUIDFromBytes(customId.getBytes());
                String docId = deterministicId.toString();

                currentChunkIds.add(docId);

                if (doc.getText() == null) {
                    continue;
                }

                Document docWithFixedId = new Document(docId, doc.getText(), doc.getMetadata());

                if (existingIds.contains(docId)) {
                    updateDocuments.add(docWithFixedId);
                } else {
                    newDocuments.add(docWithFixedId);
                }
            }

            int newCount = newDocuments.size();
            int updateCount = updateDocuments.size();

            if (!newDocuments.isEmpty()) {
                vectorStore.accept(newDocuments);
                log.info("Ingestion of {} finished. {} new chunks inserted.", fileName, newCount);
            }

            if (!updateDocuments.isEmpty()) {
                vectorStore.accept(updateDocuments);
                log.info("Ingestion of {} finished. {} chunks updated.", fileName, updateCount);
            }

            cleanupOrphanedChunks(fileName, currentChunkIds);

            return new PdfIngestResult(
                    fileName,
                    documents.size(),
                    newCount,
                    updateCount,
                    System.currentTimeMillis() - startTime
            );

        } catch (Exception e) {
            log.error("Fehler beim Laden des PDFs {}: {}", fileName, e.getMessage(), e);
            throw new RuntimeException("PDF-Verarbeitung fehlgeschlagen: " + fileName, e);
        }
    }

    private Set<String> fetchExistingIds(List<Document> documents) {
        if (documents.isEmpty()) {
            return new HashSet<>();
        }

        List<String> potentialIds = new ArrayList<>();
        for (Document doc : documents) {
            String fileName = (String) doc.getMetadata().getOrDefault("file_name", "");
            String customId = fileName + (doc.getText() != null ? doc.getText().hashCode() : 0);
            potentialIds.add(UUID.nameUUIDFromBytes(customId.getBytes()).toString());
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(potentialIds.size(), "?"));
        String sql = "SELECT id FROM vector_store WHERE id IN (" + placeholders + ")";

        String[] idArray = new String[potentialIds.size()];
        String[] paramArray = potentialIds.toArray(idArray);
        return new HashSet<>(jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("id"),
                (Object) paramArray));
    }

    private void cleanupOrphanedChunks(String fileName, List<String> currentChunkIds) {
        if (currentChunkIds.isEmpty()) {
            return;
        }

        String[] idArray = new String[currentChunkIds.size()];
        String[] paramArray = currentChunkIds.toArray(idArray);

        final String sqlCleanup = """
                DELETE FROM vector_store WHERE metadata->>'file_name' = ? AND NOT (id = ANY(?::uuid[]))
                """;

        int deletedLeichen = jdbcTemplate.update(sqlCleanup, fileName, (Object) paramArray);
        if (deletedLeichen > 0) {
            log.info("Cleanup: {} verwaiste Chunks entfernt.", deletedLeichen);
        }
    }

    @PostConstruct
    public void init() {
        // Verhindert, dass PDFBox bei fehlenden Fonts hart abbricht
        System.setProperty("pdfbox.fontcache.enabled", "false");
    }
}
