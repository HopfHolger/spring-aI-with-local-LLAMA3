package it.gdorsi.service;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import it.gdorsi.dao.DocumentOverview;

@Service
public class DocumentManagementService {

    private final JdbcTemplate jdbcTemplate;

    private final VectorStore vectorStore;

    public DocumentManagementService(JdbcTemplate jdbcTemplate, VectorStore vectorStore) {
        this.jdbcTemplate = jdbcTemplate;
        this.vectorStore = vectorStore;
    }

    public List<DocumentOverview> getAllDocuments() {
        // Wir gruppieren nach dem file_name in den Metadaten
        String sql = """
                SELECT metadata->>'file_name' as fileName,
                    count(*) as chunks,
                    max(metadata->>'ingested_at') as lastUpdate
                FROM vector_store
                GROUP BY metadata->>'file_name'
                ORDER BY lastUpdate DESC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new DocumentOverview(
                rs.getString("fileName"),
                rs.getLong("chunks"),
                rs.getString("lastUpdate")
        ));
    }

    /**
     * Kein query Deprecation: Wir nutzen den modernen SearchRequest.builder().
     * Metadaten-Fokus: Wir nutzen die JSONB-Power von Postgres. Die Suche nach file_name in den Metadaten ist extrem schnell.
     * Vollständigkeit: Ein PDF wird in viele Chunks zerlegt. Dieser Weg findet sie alle, egal wie viele es sind.
     *
     * @param fileName es wird nach Name gesucht
     */
    public void deleteByFileName(final String fileName) {
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
            List<String> ids
                    = docsToDelete.stream()
                    .map(Document::getId)
                    .toList();

            vectorStore.delete(ids);
            System.out.println(ids.size() + " Chunks für " + fileName + " gelöscht.");


        }
    }
}

