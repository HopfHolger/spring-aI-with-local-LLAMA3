package it.gdorsi.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import it.gdorsi.dao.IngestResult;
import jakarta.annotation.PostConstruct;

/**
 * Ingest KI (oft auch Data Ingestion für KI) bezeichnet den automatisierten
 * Prozess des Sammelns, Bereinigens, Strukturierens und Einspielens
 * von Daten (Dateien, Dokumente, Datenbanken) in ein KI-System oder eine Datenbank.
 */
@Service
public class PdfIngestionService {

    private final VectorStore vectorStore;

    private final JdbcTemplate jdbcTemplate;

    public PdfIngestionService(VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * update und insert
     *
     * @param pdfResource PDF
     */
    public IngestResult loadPdf(Resource pdfResource) {

        long startTime = System.currentTimeMillis();
        int newCount = 0;
        int updateCount = 0;

        // Tika ist wesentlich toleranter gegenüber fehlenden System-Fonts
        final TikaDocumentReader tikaReader = new TikaDocumentReader(pdfResource);

        // 2. Text splitten (wichtig, damit die KI nicht überfordert wird)
        // TokenTextSplitter achtet darauf, dass Sinnzusammenhänge erhalten bleiben
        // In deinem PdfIngestionService
        // vermeiden 500] Internal Server Error - {"error":"the input length exceeds the context length"}
        // Modelle wie mxbai-embed-large haben meist ein Limit von 512 oder 1024 Tokens. Ein typisches PDF-Dokument hat aber tausende.
        final TokenTextSplitter splitter = new TokenTextSplitter(
                300,  // chunkLength: Von 500 auf 300 runter (Sicherheitsmarge!)
                50,   // chunkOverlap: Kleinerer Overlap spart Platz
                5,    // keepAdjustmentThreshold
                5000, // maxNumChunks: Falls das PDF riesig ist
                true  // keepDelimiters
        );

        // 3. Transformation & Speicherung
        // apply() liest das PDF, splittet es und gibt eine Liste von Documents zurück
        List<Document> documents = splitter.apply(tikaReader.get());
        if (documents.isEmpty()) { // zum Sehen ob etwas extrahiert wurde, bei nur Bilder ZB
            System.err.println("WARNUNG: Keine Texte im PDF gefunden!");
        }

        String fileName = pdfResource.getFilename();

        List<String> currentChunkIds = new ArrayList<>();

        // call jetzt idempotent - vorher delete unnötig, gleicher hash kommt nie vor insert wird update
        for (Document doc : documents) {
            doc.getMetadata().put("file_name", pdfResource.getFilename()); // besser zum Löschen
            doc.getMetadata().put("ingested_at", System.currentTimeMillis()); // vermeidet neuanlage
            // Wir erzeugen eine ID aus Dateiname + Inhalt-Hash
            final String customId = pdfResource.getFilename() + (doc.getText() != null ? doc.getText().hashCode() : 0);

            // WICHTIG: UUID aus dem String generieren (Postgres pgvector nutzt meist UUIDs)
            final UUID deterministicId = UUID.nameUUIDFromBytes(customId.getBytes());
            currentChunkIds.add(deterministicId.toString());

            Document docWithFixedId;
            if (doc.getText() != null) {
                docWithFixedId = new Document(
                        deterministicId.toString(),
                        doc.getText(),
                        doc.getMetadata()
                );
            } else {
                continue;
            }

            if (exists(docWithFixedId.getId())) {
                updateCount++;
                long oldTime = (long) docWithFixedId.getMetadata().get("ingested_at");
                System.out.println("Update: Alter Zeitstempel war " + oldTime);

                docWithFixedId.getMetadata().put("ingested_at", System.currentTimeMillis());
                vectorStore.accept(List.of(docWithFixedId));
                System.out.println("Ingestion of " + fileName + " finished. Chunks" + documents.size() + " update.");
            } else {
                newCount++;
                docWithFixedId.getMetadata().put("ingested_at", System.currentTimeMillis());
                vectorStore.accept(List.of(docWithFixedId));
                System.out.println("Ingestion of " + fileName + " finished. Chunks" + documents.size() + " insert.");
            }
        }

        // 3. CLEANUP: Lösche alle Chunks dieses Files, die NICHT in currentChunkIds sind
        // Das entfernt "verwaiste" Chunks, wenn das PDF gekürzt wurde.
        if (!currentChunkIds.isEmpty()) {
            // Wir bauen ein Array aus den UUIDs für den Postgres-Operator '= ANY' oder 'NOT IN'
            String[] idArray = currentChunkIds.toArray(new String[0]);

            final String sqlCleanup = """
                    DELETE FROM vector_store WHERE metadata->>'file_name' = ? 
                    AND NOT (id = ANY(?::uuid[]))
                    """;

            int deletedLeichen = jdbcTemplate.update(sqlCleanup, fileName, idArray);
            System.out.println("Cleanup: " + deletedLeichen + " verwaiste Chunks entfernt.");
        }

        return new IngestResult(
                pdfResource.getFilename(),
                documents.size(),
                newCount,
                updateCount,
                System.currentTimeMillis() - startTime
        );
    }

    public boolean exists(final String docId) {
        try {
            // Wir nutzen 'EXISTS' und einen expliziten Cast, das ist performanter
            final String sql = "SELECT EXISTS(SELECT 1 FROM vector_store WHERE id = CAST(? AS uuid))";
            return jdbcTemplate.queryForObject(sql, Boolean.class, docId);
        } catch (Exception e) {
            System.err.println("Fehler beim ID-Check: " + e.getMessage());
            return false;
        }
    }


    @PostConstruct
    public void init() {
        // Verhindert, dass PDFBox bei fehlenden Fonts hart abbricht
        System.setProperty("pdfbox.fontcache.enabled", "false");
    }


}
