package it.gdorsi.service.tool;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import it.gdorsi.repository.AuthorRepository;
import it.gdorsi.repository.XmlDokumentRepository;
import it.gdorsi.repository.model.Autor;
import it.gdorsi.repository.model.XmlDokument;
import it.gdorsi.service.XmlOperations;
import it.gdorsi.service.response.XmlListResponse;
import it.gdorsi.service.response.XmlResponse;
import jakarta.transaction.Transactional;

@Component
public class XmlTool implements XmlOperations {

    private final XmlDokumentRepository xmlDokumentRepository;
    private final AuthorRepository authorRepository;
    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    public XmlTool(XmlDokumentRepository xmlDokumentRepository, AuthorRepository authorRepository,
            EmbeddingModel embeddingModel, VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        this.xmlDokumentRepository = xmlDokumentRepository;
        this.authorRepository = authorRepository;
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public String saveXml(String autorName, String dateiname, String xmlInhalt) {
        Autor autor = authorRepository.findByName(autorName);
        if (autor == null) {
            return "FEHLER: Autor '" + autorName + "' existiert nicht in der Datenbank.";
        }

        float[] vector = embeddingModel.embed(xmlInhalt);

        XmlDokument xmlDokument = new XmlDokument(dateiname, xmlInhalt, vector, autor);
        XmlDokument savedDokument = xmlDokumentRepository.save(xmlDokument);

        // XML-Dokument auch im Vector Store speichern für RAG
        storeXmlInVectorStore(savedDokument, autorName);

        return "XML-Dokument '" + dateiname + "' wurde erfolgreich für Autor '" + autorName + "' gespeichert.";
    }

    @Override
    @Transactional
    public String deleteXmlByAutor(String autorName) {
        Autor autor = authorRepository.findByName(autorName);
        if (autor == null) {
            return "FEHLER: Autor '" + autorName + "' existiert nicht in der Datenbank.";
        }

        List<XmlDokument> dokumente = xmlDokumentRepository.findByAutorId(autor.getId());
        if (dokumente.isEmpty()) {
            return "Keine XML-Dokumente für Autor '" + autorName + "' gefunden.";
        }

        xmlDokumentRepository.deleteByAutorId(autor.getId());
        return dokumente.size() + " XML-Dokument(e) für Autor '" + autorName + "' wurden gelöscht.";
    }

    @Override
    public XmlListResponse getXmlByAutor(String autorName) {
        Autor autor = authorRepository.findByName(autorName);
        if (autor == null) {
            return XmlListResponse.error("Autor '" + autorName + "' existiert nicht in der Datenbank.");
        }

        List<XmlDokument> dokumente = xmlDokumentRepository.findByAutorId(autor.getId());
        if (dokumente.isEmpty()) {
            return XmlListResponse.empty(autorName);
        }

        List<XmlResponse> xmlResponses = dokumente.stream()
                .map(d -> XmlResponse.success(d.getId(), d.getDateiname(), d.getInhalt(), autorName))
                .collect(Collectors.toList());

        return XmlListResponse.success(xmlResponses);
    }

    @Override
    public XmlResponse getXmlById(String autorName, Long xmlId) {
        Autor autor = authorRepository.findByName(autorName);
        if (autor == null) {
            return XmlResponse.error(autorName, "Autor existiert nicht in der Datenbank.");
        }

        return xmlDokumentRepository.findById(xmlId)
                .filter(d -> d.getAutor().getId().equals(autor.getId()))
                .map(d -> XmlResponse.success(d.getId(), d.getDateiname(), d.getInhalt(), autorName))
                .orElse(XmlResponse.notFound(autorName, xmlId));
    }

    @Override
    @Transactional
    public XmlResponse updateXml(String autorName, Long xmlId, String dateiname, String xmlInhalt) {
        Autor autor = authorRepository.findByName(autorName);
        if (autor == null) {
            return XmlResponse.error(autorName, "Autor existiert nicht in der Datenbank.");
        }

        return xmlDokumentRepository.findById(xmlId)
                .filter(d -> d.getAutor().getId().equals(autor.getId()))
                .map(xmlDokument -> {
                    xmlDokument.setDateiname(dateiname);
                    xmlDokument.setInhalt(xmlInhalt);
                    
                    float[] vector = embeddingModel.embed(xmlInhalt);
                    xmlDokument.setXmlEmbedding(vector);
                    
                    XmlDokument updated = xmlDokumentRepository.save(xmlDokument);
                    
                    // Aktualisiere auch im Vector Store
                    storeXmlInVectorStore(updated, autorName);
                    
                    return XmlResponse.success(updated.getId(), updated.getDateiname(), updated.getInhalt(), autorName);
                })
                .orElse(XmlResponse.notFound(autorName, xmlId));
    }

    @Override
    @Transactional
    public String deleteXmlById(String autorName, Long xmlId) {
        Autor autor = authorRepository.findByName(autorName);
        if (autor == null) {
            return "FEHLER: Autor '" + autorName + "' existiert nicht in der Datenbank.";
        }

        return xmlDokumentRepository.findById(xmlId)
                .filter(d -> d.getAutor().getId().equals(autor.getId()))
                .map(xmlDokument -> {
                    // Lösche zuerst aus Vector Store
                    deleteXmlFromVectorStore(xmlDokument.getId(), autorName);
                    
                    // Dann aus Datenbank
                    xmlDokumentRepository.delete(xmlDokument);
                    return "XML-Dokument mit ID " + xmlId + " für Autor '" + autorName + "' wurde gelöscht.";
                })
                .orElse("FEHLER: XML-Dokument mit ID " + xmlId + " für Autor '" + autorName + "' nicht gefunden.");
    }

    @Override
    public XmlListResponse searchSimilarXml(String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return XmlListResponse.error("Suchanfrage darf nicht leer sein.");
        }

        if (limit <= 0 || limit > 20) {
            limit = 5; // Default limit
        }

        try {
            // Similarity Search im Vector Store
            var searchResults = vectorStore.similaritySearch(
                SearchRequest.builder()
                    .query(query)
                    .topK(limit)
                    .similarityThreshold(0.3) // Schwellenwert für Relevanz
                    .build()
            );

            if (searchResults.isEmpty()) {
                return XmlListResponse.empty("Keine ähnlichen XML-Dokumente gefunden für: " + query);
            }

            // Konvertiere Search Results zu XML Responses
            List<XmlResponse> xmlResponses = searchResults.stream()
                .map(document -> {
                    // Extrahiere Metadaten
                    var metadata = document.getMetadata();
                    Long xmlId = null;
                    String autorName = "Unbekannt";
                    String dateiname = "Unbekannt";

                    if (metadata.containsKey("xml_id")) {
                        try {
                            xmlId = Long.parseLong(metadata.get("xml_id").toString());
                        } catch (NumberFormatException e) {
                            // Ignoriere fehlerhafte ID
                        }
                    }

                    if (metadata.containsKey("autor_name")) {
                        autorName = metadata.get("autor_name").toString();
                    }

                    if (metadata.containsKey("dateiname")) {
                        dateiname = metadata.get("dateiname").toString();
                    }

                    // Hole vollständiges XML-Dokument aus der Datenbank
                    if (xmlId != null) {
                        return xmlDokumentRepository.findById(xmlId)
                            .map(xml -> XmlResponse.success(
                                xml.getId(),
                                xml.getDateiname(),
                                xml.getInhalt().length() > 500 
                                    ? xml.getInhalt().substring(0, 500) + "..." 
                                    : xml.getInhalt(),
                                xml.getAutor().getName()
                            ))
                            .orElse(XmlResponse.error(autorName, "XML-Dokument nicht mehr verfügbar"));
                    } else {
                        // Fallback: Verwende Text aus Vector Store
                        return XmlResponse.searchResult(
                            -1L, // Platzhalter ID
                            dateiname,
                            document.getText().length() > 500 
                                ? document.getText().substring(0, 500) + "..." 
                                : document.getText(),
                            autorName,
                            document.getScore() // Similarity Score
                        );
                    }
                })
                .collect(Collectors.toList());

            return XmlListResponse.searchResults(xmlResponses, query);
        } catch (Exception e) {
            return XmlListResponse.error("Fehler bei der semantischen Suche: " + e.getMessage());
        }
    }

    private void storeXmlInVectorStore(XmlDokument xmlDokument, String autorName) {
        try {
            // Erstelle Document mit Metadaten für Vector Store
            Map<String, Object> metadata = Map.of(
                "document_type", "xml",
                "xml_id", xmlDokument.getId().toString(),
                "autor_name", autorName,
                "dateiname", xmlDokument.getDateiname(),
                "ingested_at", System.currentTimeMillis()
            );

            // Erstelle eindeutige ID basierend auf XML-Inhalt und Autor
            String customId = "xml_" + autorName + "_" + xmlDokument.getId() + "_" + xmlDokument.getInhalt().hashCode();
            UUID deterministicId = UUID.nameUUIDFromBytes(customId.getBytes());
            String docId = deterministicId.toString();

            // Erstelle Document für Vector Store
            Document document = new Document(
                docId,
                xmlDokument.getInhalt(),
                metadata
            );

            // Speichere im Vector Store
            vectorStore.accept(List.of(document));
            
        } catch (Exception e) {
            // Logge Fehler aber breche nicht ab, da XML bereits in DB gespeichert ist
            System.err.println("Warnung: XML konnte nicht im Vector Store gespeichert werden: " + e.getMessage());
        }
    }

    private void deleteXmlFromVectorStore(Long xmlId, String autorName) {
        try {
            // Lösche alle Einträge mit dieser XML-ID und Autor
            String sql = "DELETE FROM vector_store WHERE metadata->>'xml_id' = ? AND metadata->>'autor_name' = ?";
            jdbcTemplate.update(sql, xmlId.toString(), autorName);
            
        } catch (Exception e) {
            System.err.println("Warnung: XML konnte nicht aus Vector Store gelöscht werden: " + e.getMessage());
        }
    }
}
