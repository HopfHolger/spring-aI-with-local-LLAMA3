package it.gdorsi.service;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class PdfIngestionService {

    private final VectorStore vectorStore;

    private final JdbcTemplate jdbcTemplate;

    // Wir injizieren die PDF via Resource-Loader (z.B. aus classpath oder file)
    public PdfIngestionService(VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * update und insert
     * @param pdfResource
     */
    public void loadPdf(Resource pdfResource) {

        // Tika ist wesentlich toleranter gegenüber fehlenden System-Fonts
        TikaDocumentReader tikaReader = new TikaDocumentReader(pdfResource);

        // 2. Text splitten (wichtig, damit die KI nicht überfordert wird)
        // TokenTextSplitter achtet darauf, dass Sinnzusammenhänge erhalten bleiben
        // In deinem PdfIngestionService
        // vermeiden 500] Internal Server Error - {"error":"the input length exceeds the context length"}
        // Modelle wie mxbai-embed-large haben meist ein Limit von 512 oder 1024 Tokens. Ein typisches PDF-Dokument hat aber tausende.
        TokenTextSplitter splitter = new TokenTextSplitter(
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

        // Sanitize: Ungültige Unicode-Zeichen entfernen
        List<Document> cleanDocuments = documents.stream()
                .map(doc -> {
                    String cleanContent = sanitizeText(doc.getContent());
                    return new Document(doc.getId(), cleanContent, doc.getMetadata());
                })
                .toList();

        String fileName = pdfResource.getFilename();

        // call jetzt idempotent - vorher delete unnötig, gleicher hash kommt nie vor insert wird update
        for (Document doc : documents) {
            doc.getMetadata().put("file_name", pdfResource.getFilename()); // besser zum Löschen
            doc.getMetadata().put("ingested_at", System.currentTimeMillis()); // vermeidet neuanlage
            // Wir erzeugen eine ID aus Dateiname + Inhalt-Hash
            String customId = pdfResource.getFilename() + doc.getContent().hashCode();

            // WICHTIG: UUID aus dem String generieren (Postgres pgvector nutzt meist UUIDs)
            UUID deterministicId = UUID.nameUUIDFromBytes(customId.getBytes());

            Document docWithFixedId = new Document(
                    deterministicId.toString(),
                    doc.getContent(),
                    doc.getMetadata()
            );

            if (exists(docWithFixedId.getId())) {
                long oldTime = (long) docWithFixedId.getMetadata().get("ingested_at");
                System.out.println("Update: Alter Zeitstempel war " + oldTime);

                docWithFixedId.getMetadata().put("ingested_at", System.currentTimeMillis());
                vectorStore.accept(List.of(docWithFixedId));
                System.out.println("Ingestion of " + fileName + " finished. Chunks" + documents.size() + " update.");
            } else {
                docWithFixedId.getMetadata().put("ingested_at", System.currentTimeMillis());
                vectorStore.accept(List.of(docWithFixedId));
                System.out.println("Ingestion of " + fileName + " finished. Chunks" + documents.size() + " insert.");
            }

        }
    }

    public boolean exists(String docId) {
        try {
            // Wir nutzen 'EXISTS' und einen expliziten Cast, das ist performanter
            String sql = "SELECT EXISTS(SELECT 1 FROM vector_store WHERE id = CAST(? AS uuid))";
            return jdbcTemplate.queryForObject(sql, Boolean.class, docId);
        } catch (Exception e) {
            System.err.println("Fehler beim ID-Check: " + e.getMessage());
            return false;
        }
    }

    /**
     * Kein query Deprecation: Wir nutzen den modernen SearchRequest.builder().
     * Metadaten-Fokus: Wir nutzen die JSONB-Power von Postgres. Die Suche nach file_name in den Metadaten ist extrem schnell.
     * Vollständigkeit: Ein PDF wird in viele Chunks zerlegt. Dieser Weg findet sie alle, egal wie viele es sind.
     * @param fileName
     */
    public void deleteByFileName(String fileName) {
        // 1. Erstelle eine Filter-Expression (keine semantische Suche!)
        var filterExpression = new FilterExpressionBuilder()
                .eq("file_name", fileName) // Name muss exakt mit Metadata-Key übereinstimmen
                .build();

        // 2. Nutze einen SearchRequest NUR mit Filter (TopK sehr hoch ansetzen)
        SearchRequest searchRequest = SearchRequest.builder()
                .query("") // Leerer Query, da wir nur filtern wollen
                .filterExpression(filterExpression)
                .topK(10000) // Sicherstellen, dass wir alle Chunks erwischen
                .build();

        List<Document> docsToDelete = vectorStore.similaritySearch(searchRequest);

        // 3. Löschen, falls etwas gefunden wurde
        if (!docsToDelete.isEmpty()) {
            List<String> ids = docsToDelete.stream()
                    .map(Document::getId)
                    .toList();
            vectorStore.delete(ids);
            System.out.println(ids.size() + " Chunks für " + fileName + " gelöscht.");
        }
    }

    private String sanitizeText(String text) {
        if (text == null) return null;
        // Entfernt alle Zeichen, die nicht in die Basic Multilingual Plane passen
        // und SQL-Probleme verursachen könnten (Surrogates)
        return text.replaceAll("[\\uD800-\\uDFFF]", "");
    }

    @PostConstruct
    public void init() {
        // Verhindert, dass PDFBox bei fehlenden Fonts hart abbricht
        System.setProperty("pdfbox.fontcache.enabled", "false");
    }


}
