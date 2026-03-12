package it.gdorsi.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.gdorsi.repository.AuthorRepository;
import it.gdorsi.repository.XmlDokumentRepository;
import it.gdorsi.repository.model.Autor;
import it.gdorsi.repository.model.XmlDokument;
import it.gdorsi.service.response.XmlListResponse;
import it.gdorsi.service.response.XmlResponse;

@Service
public class XmlDokumentService implements XmlOperations {

    private final XmlDokumentRepository xmlDokumentRepository;
    private final AuthorRepository authorRepository;
    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    public XmlDokumentService(XmlDokumentRepository xmlDokumentRepository, AuthorRepository authorRepository,
                              EmbeddingModel embeddingModel, VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        this.xmlDokumentRepository = xmlDokumentRepository;
        this.authorRepository = authorRepository;
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    public XmlDokument saveXml(Long autorId, String dateiname, String inhalt) {
        Autor autor = authorRepository.findById(autorId).orElseThrow();
        float[] vector = embeddingModel.embed(inhalt);
        XmlDokument xmlDokument = new XmlDokument(dateiname, inhalt, vector, autor);
        xmlDokument = xmlDokumentRepository.save(xmlDokument);
        
        // XML-Dokument auch im Vector Store speichern für RAG
        storeXmlInVectorStore(xmlDokument, autor.getName());
        
        return xmlDokument;
    }

    public Optional<XmlDokument> updateXml(Long autorId, Long xmlId, String dateiname, String inhalt) {
        return xmlDokumentRepository.findById(xmlId)
                .filter(d -> d.getAutor().getId().equals(autorId))
                .map(xmlDokument -> {
                    xmlDokument.setDateiname(dateiname);
                    xmlDokument.setInhalt(inhalt);
                    
                    float[] vector = embeddingModel.embed(inhalt);
                    xmlDokument.setXmlEmbedding(vector);
                    
                    xmlDokument = xmlDokumentRepository.save(xmlDokument);
                    
                    // Aktualisiere auch im Vector Store
                    storeXmlInVectorStore(xmlDokument, xmlDokument.getAutor().getName());
                    
                    return xmlDokument;
                });
    }

    public Optional<XmlDokument> findById(Long id) {
        return xmlDokumentRepository.findById(id);
    }

    public void deleteXmlById(Long id) {
        xmlDokumentRepository.findById(id).ifPresent(xmlDokument -> {
            // Lösche zuerst aus Vector Store
            deleteXmlFromVectorStore(xmlDokument.getId(), xmlDokument.getAutor().getName());
            // Dann aus Datenbank
            xmlDokumentRepository.delete(xmlDokument);
        });
    }

    public List<Autor> findAllAutoren() {
        return authorRepository.findAll();
    }

    public List<XmlDokument> findAllXmlDokumente() {
        List<Autor> autoren = authorRepository.findAll();
        return autoren.stream()
                .flatMap(a -> xmlDokumentRepository.findByAutorId(a.getId()).stream())
                .toList();
    }

    public String getAutorNameById(Long autorId) {
        return authorRepository.findById(autorId)
                .map(Autor::getName)
                .orElse(null);
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

        // Lösche zuerst aus Vector Store
        for (XmlDokument dokument : dokumente) {
            deleteXmlFromVectorStore(dokument.getId(), autorName);
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
                    
                    xmlDokument = xmlDokumentRepository.save(xmlDokument);
                    
                    // Aktualisiere auch im Vector Store
                    storeXmlInVectorStore(xmlDokument, autorName);
                    
                    return XmlResponse.success(xmlDokument.getId(), xmlDokument.getDateiname(), xmlDokument.getInhalt(), autorName);
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

    @Override
    public String analyzeXml(String autorName, Long xmlId) {
        try {
            // Finde zuerst den Autor, dann das XML-Dokument
            Autor autor = authorRepository.findByName(autorName);
            if (autor == null) {
                throw new IllegalArgumentException("Autor nicht gefunden: " + autorName);
            }
            
            XmlDokument xml = xmlDokumentRepository.findById(xmlId)
                .filter(x -> x.getAutor().getId().equals(autor.getId()))
                .orElseThrow(() -> new IllegalArgumentException("XML-Dokument nicht gefunden für Autor: " + autorName + ", ID: " + xmlId));
            
            String inhalt = xml.getInhalt();
            int charCount = inhalt.length();
            int lineCount = inhalt.split("\n").length;
            int elementCount = countXmlElements(inhalt);
            
            return String.format("""
                📊 XML-Analyse für '%s' (ID: %d):
                
                • Dateiname: %s
                • Autor: %s
                • Größe: %d Zeichen, %d Zeilen
                • Geschätzte Elemente: %d
                • Erstellt am: %s
                
                Struktur-Übersicht:
                %s
                """, 
                xml.getDateiname(), xmlId,
                xml.getDateiname(),
                autorName,
                charCount, lineCount,
                elementCount,
                xml.getCreatedAt() != null ? xml.getCreatedAt().toString() : "Unbekannt",
                getXmlStructureSummary(inhalt));
        } catch (Exception e) {
            return "❌ Fehler bei der XML-Analyse: " + e.getMessage();
        }
    }

    @Override
    public String compareXml(String autorName, Long xmlId1, Long xmlId2) {
        try {
            // Finde zuerst den Autor
            Autor autor = authorRepository.findByName(autorName);
            if (autor == null) {
                throw new IllegalArgumentException("Autor nicht gefunden: " + autorName);
            }
            
            // Finde die XML-Dokumente
            XmlDokument xml1 = xmlDokumentRepository.findById(xmlId1)
                .filter(x -> x.getAutor().getId().equals(autor.getId()))
                .orElseThrow(() -> new IllegalArgumentException("XML-Dokument 1 nicht gefunden"));
            XmlDokument xml2 = xmlDokumentRepository.findById(xmlId2)
                .filter(x -> x.getAutor().getId().equals(autor.getId()))
                .orElseThrow(() -> new IllegalArgumentException("XML-Dokument 2 nicht gefunden"));
            
            String inhalt1 = xml1.getInhalt();
            String inhalt2 = xml2.getInhalt();
            
            return String.format("""
                🔍 XML-Vergleich für Autor '%s':
                
                Dokument 1: %s (ID: %d)
                • Größe: %d Zeichen, %d Zeilen
                • Elemente: %d
                
                Dokument 2: %s (ID: %d)
                • Größe: %d Zeichen, %d Zeilen
                • Elemente: %d
                
                Unterschiede:
                • Größenunterschied: %d Zeichen
                • Zeilenunterschied: %d Zeilen
                • Gleicher Inhalt: %s
                
                Empfehlung: %s
                """,
                autorName,
                xml1.getDateiname(), xmlId1,
                inhalt1.length(), inhalt1.split("\n").length, countXmlElements(inhalt1),
                xml2.getDateiname(), xmlId2,
                inhalt2.length(), inhalt2.split("\n").length, countXmlElements(inhalt2),
                Math.abs(inhalt1.length() - inhalt2.length()),
                Math.abs(inhalt1.split("\n").length - inhalt2.split("\n").length),
                inhalt1.equals(inhalt2) ? "Ja" : "Nein",
                inhalt1.equals(inhalt2) ? "Dokumente sind identisch" : "Dokumente unterscheiden sich");
        } catch (Exception e) {
            return "❌ Fehler beim XML-Vergleich: " + e.getMessage();
        }
    }

    @Override
    public String validateXml(String autorName, Long xmlId) {
        try {
            // Finde zuerst den Autor
            Autor autor = authorRepository.findByName(autorName);
            if (autor == null) {
                throw new IllegalArgumentException("Autor nicht gefunden: " + autorName);
            }
            
            // Finde das XML-Dokument
            XmlDokument xml = xmlDokumentRepository.findById(xmlId)
                .filter(x -> x.getAutor().getId().equals(autor.getId()))
                .orElseThrow(() -> new IllegalArgumentException("XML-Dokument nicht gefunden"));
            
            String inhalt = xml.getInhalt();
            boolean hasXmlDeclaration = inhalt.trim().startsWith("<?xml");
            boolean hasRootElement = inhalt.contains("<") && inhalt.contains(">");
            boolean isWellFormed = checkXmlWellFormed(inhalt);
            
            return String.format("""
                ✅ XML-Validierung für '%s' (ID: %d):
                
                • XML-Deklaration vorhanden: %s
                • Root-Element vorhanden: %s
                • Wohlgeformt: %s
                • Größe: %d Zeichen
                
                Validierungs-Ergebnis: %s
                
                %s
                """,
                xml.getDateiname(), xmlId,
                hasXmlDeclaration ? "✅" : "❌",
                hasRootElement ? "✅" : "❌",
                isWellFormed ? "✅" : "❌",
                inhalt.length(),
                isWellFormed ? "✅ XML ist gültig" : "❌ XML hat Probleme",
                getValidationSuggestions(hasXmlDeclaration, hasRootElement, isWellFormed));
        } catch (Exception e) {
            return "❌ Fehler bei der XML-Validierung: " + e.getMessage();
        }
    }

    @Override
    public String extractXmlElements(String autorName, Long xmlId, String xpath) {
        try {
            // Finde zuerst den Autor
            Autor autor = authorRepository.findByName(autorName);
            if (autor == null) {
                throw new IllegalArgumentException("Autor nicht gefunden: " + autorName);
            }
            
            // Finde das XML-Dokument
            XmlDokument xml = xmlDokumentRepository.findById(xmlId)
                .filter(x -> x.getAutor().getId().equals(autor.getId()))
                .orElseThrow(() -> new IllegalArgumentException("XML-Dokument nicht gefunden"));
            
            String inhalt = xml.getInhalt();
            String extracted = extractWithSimpleXPath(inhalt, xpath);
            
            return String.format("""
                📋 XPath-Extraktion für '%s' (ID: %d):
                
                XPath: %s
                
                Gefundene Elemente: %d
                
                Extrahiert:
                ```
                %s
                ```
                
                Hinweis: Dies ist eine einfache XPath-Implementierung. Für komplexe XPath-Ausdrücke verwenden Sie eine dedizierte XML-Bibliothek.
                """,
                xml.getDateiname(), xmlId,
                xpath,
                extracted.split("\n").length,
                extracted);
        } catch (Exception e) {
            return "❌ Fehler bei der XPath-Extraktion: " + e.getMessage();
        }
    }

    @Override
    public String transformXml(String autorName, Long xmlId, String transformationRules) {
        try {
            // Finde zuerst den Autor
            Autor autor = authorRepository.findByName(autorName);
            if (autor == null) {
                throw new IllegalArgumentException("Autor nicht gefunden: " + autorName);
            }
            
            // Finde das XML-Dokument
            XmlDokument xml = xmlDokumentRepository.findById(xmlId)
                .filter(x -> x.getAutor().getId().equals(autor.getId()))
                .orElseThrow(() -> new IllegalArgumentException("XML-Dokument nicht gefunden"));
            
            String inhalt = xml.getInhalt();
            String transformed = applySimpleTransformation(inhalt, transformationRules);
            
            return String.format("""
                🔄 XML-Transformation für '%s' (ID: %d):
                
                Transformations-Regeln: %s
                
                Original-Größe: %d Zeichen
                Transformierte Größe: %d Zeichen
                
                Transformiertes XML:
                ```
                %s
                ```
                
                Hinweis: Dies ist eine einfache Transformation. Für komplexe XSLT-Transformationen verwenden Sie eine dedizierte XML-Bibliothek.
                """,
                xml.getDateiname(), xmlId,
                transformationRules,
                inhalt.length(),
                transformed.length(),
                transformed);
        } catch (Exception e) {
            return "❌ Fehler bei der XML-Transformation: " + e.getMessage();
        }
    }

    private int countXmlElements(String xml) {
        // Einfache Zählung von XML-Elementen durch Zählen der öffnenden Tags
        int count = 0;
        int index = 0;
        while ((index = xml.indexOf('<', index)) != -1) {
            if (index + 1 < xml.length() && xml.charAt(index + 1) != '!' && xml.charAt(index + 1) != '?') {
                count++;
            }
            index++;
        }
        return count;
    }

    private String getXmlStructureSummary(String xml) {
        // Extrahiere erste paar Elemente für Struktur-Übersicht
        String[] lines = xml.split("\n");
        StringBuilder summary = new StringBuilder();
        int elementCount = 0;
        
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("<") && !trimmed.startsWith("<?") && !trimmed.startsWith("<!--")) {
                if (elementCount < 5) {
                    summary.append("  • ").append(trimmed.substring(0, Math.min(50, trimmed.length())));
                    if (trimmed.length() > 50) summary.append("...");
                    summary.append("\n");
                }
                elementCount++;
            }
        }
        
        if (elementCount > 5) {
            summary.append("  • ... und ").append(elementCount - 5).append(" weitere Elemente\n");
        }
        
        return summary.toString();
    }

    private boolean checkXmlWellFormed(String xml) {
        // Einfache Prüfung auf wohlgeformtes XML
        try {
            // Prüfe auf geschlossene Tags (einfache Implementierung)
            int openTags = 0;
            int closeTags = 0;
            String[] lines = xml.split("\n");
            
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("<") && !trimmed.startsWith("<?") && !trimmed.startsWith("<!--")) {
                    if (!trimmed.contains("/>")) {
                        if (!trimmed.startsWith("</")) {
                            openTags++;
                        } else {
                            closeTags++;
                        }
                    }
                }
            }
            
            return openTags == closeTags;
        } catch (Exception e) {
            return false;
        }
    }

    private String getValidationSuggestions(boolean hasXmlDeclaration, boolean hasRootElement, boolean isWellFormed) {
        StringBuilder suggestions = new StringBuilder();
        
        if (!hasXmlDeclaration) {
            suggestions.append("• Fügen Sie eine XML-Deklaration hinzu: <?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        }
        if (!hasRootElement) {
            suggestions.append("• Stellen Sie sicher, dass ein Root-Element vorhanden ist\n");
        }
        if (!isWellFormed) {
            suggestions.append("• Überprüfen Sie, ob alle Tags korrekt geschlossen sind\n");
        }
        
        if (suggestions.length() == 0) {
            suggestions.append("• XML ist gut strukturiert\n");
        }
        
        return suggestions.toString();
    }

    private String extractWithSimpleXPath(String xml, String xpath) {
        // Einfache XPath-Implementierung für grundlegende Pfade
        if (xpath.equals("/") || xpath.equals("/*")) {
            return xml;
        }
        
        if (xpath.startsWith("//")) {
            // Suche nach Elementnamen
            String elementName = xpath.substring(2);
            return extractElementsByName(xml, elementName);
        }
        
        return "XPath-Ausdruck wird nicht unterstützt: " + xpath + "\nUnterstützt: '/', '/*', '//elementName'";
    }

    private String extractElementsByName(String xml, String elementName) {
        StringBuilder result = new StringBuilder();
        String[] lines = xml.split("\n");
        
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("<" + elementName + ">") || 
                trimmed.startsWith("<" + elementName + " ") ||
                trimmed.contains("</" + elementName + ">")) {
                result.append(line).append("\n");
            }
        }
        
        return result.length() > 0 ? result.toString() : "Keine Elemente mit Namen '" + elementName + "' gefunden";
    }

    private String applySimpleTransformation(String xml, String rules) {
        // Einfache Transformationen
        if (rules.contains("toUpperCase")) {
            return xml.toUpperCase();
        }
        if (rules.contains("toLowerCase")) {
            return xml.toLowerCase();
        }
        if (rules.contains("removeComments")) {
            return xml.replaceAll("<!--.*?-->", "");
        }
        if (rules.contains("removeWhitespace")) {
            return xml.replaceAll("\\s+", " ").trim();
        }
        
        return "Transformations-Regel nicht erkannt: " + rules + "\nUnterstützt: 'toUpperCase', 'toLowerCase', 'removeComments', 'removeWhitespace'";
    }
}
