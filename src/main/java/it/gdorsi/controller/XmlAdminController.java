package it.gdorsi.controller;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import it.gdorsi.repository.model.Autor;
import it.gdorsi.repository.model.XmlDokument;
import it.gdorsi.service.XmlDokumentService;
import it.gdorsi.service.response.XmlListResponse;

@Controller
public class XmlAdminController {

    private final XmlDokumentService xmlDokumentService;
    private final ChatClient chatClient;

    public XmlAdminController(XmlDokumentService xmlDokumentService, ChatClient chatClient) {
        this.xmlDokumentService = xmlDokumentService;
        this.chatClient = chatClient;
    }

    @GetMapping("/admin/xml")
    public String xmlAdminPage(Model model) {
        List<Autor> autoren = xmlDokumentService.findAllAutoren();
        List<XmlDokument> xmlDokumente = xmlDokumentService.findAllXmlDokumente();
        
        model.addAttribute("autoren", autoren);
        model.addAttribute("xmlDokumente", xmlDokumente);
        return "xml-admin";
    }

    @GetMapping("/admin/xml/list")
    public String getXmlList(Model model) {
        List<XmlDokument> xmlDokumente = xmlDokumentService.findAllXmlDokumente();
        
        model.addAttribute("xmlDokumente", xmlDokumente);
        return "xml-admin :: xmlTable";
    }

    @PostMapping("/admin/xml/upload")
    public ResponseEntity<String> handleXmlUpload(
            @RequestParam("autorId") Long autorId,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("<div class='text-red-600'>❌ Fehler: keine Datei ausgewählt</div>");
        }

        try {
            String inhalt = new String(file.getBytes());
            String dateiname = file.getOriginalFilename();
            
            xmlDokumentService.saveXml(autorId, dateiname, inhalt);

            return ResponseEntity.ok()
                    .header("HX-Trigger", "updateXmlList")
                    .body("<div class='text-green-600'>✅ XML erfolgreich hochgeladen!</div>");
        } catch (IOException e) {
            return ResponseEntity.status(500)
                    .body("<div class='text-red-600'>❌ Fehler: " + e.getMessage() + "</div>");
        }
    }

    @GetMapping("/admin/xml/{id}")
    public String getXmlDetail(@PathVariable Long id, Model model) {
        return xmlDokumentService.findById(id)
                .map(xml -> {
                    model.addAttribute("xml", xml);
                    return "xml-admin :: xmlDetail";
                })
                .orElse("xml-admin :: xmlDetailEmpty");
    }

    @DeleteMapping("/admin/xml/{id}")
    public ResponseEntity<String> deleteXml(@PathVariable Long id) {
        xmlDokumentService.deleteXmlById(id);
        return ResponseEntity.ok()
                .header("HX-Trigger", "updateXmlList")
                .body("");
    }

    @PostMapping("/admin/xml/chat")
    public ResponseEntity<String> analyzeXmlWithAi(
            @RequestParam("xmlId") Long xmlId,
            @RequestParam("question") String question) {
        
        Optional<XmlDokument> xmlOpt = xmlDokumentService.findById(xmlId);
        if (xmlOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("<div class='text-red-600'>❌ Fehler: XML-Dokument nicht gefunden</div>");
        }

        XmlDokument xml = xmlOpt.get();
        String xmlContent = xml.getInhalt();
        String xmlPreview = xmlContent.length() > 500 ? xmlContent.substring(0, 500) + "..." : xmlContent;
        
        try {
            String response = chatClient.prompt()
                .user("Analysiere dieses XML-Dokument: " + xml.getDateiname() + 
                      "\n\nXML-Inhalt (Auszug):\n" + xmlPreview + 
                      "\n\nFrage: " + question + 
                      "\n\nAntworte auf Deutsch und sei präzise.")
                .call()
                .content();

            String formattedResponse = "<div class='bg-purple-50 p-4 rounded-lg border border-purple-200'>" +
                    "<div class='flex items-center gap-2 mb-2'>" +
                    "<span class='text-purple-600'>✨</span>" +
                    "<span class='font-bold text-purple-700'>KI-Analyse für " + xml.getDateiname() + "</span>" +
                    "</div>" +
                    "<div class='text-gray-800'>" + response.replace("\n", "<br>") + "</div>" +
                    "</div>";

            return ResponseEntity.ok(formattedResponse);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("<div class='text-red-600'>❌ Fehler bei der KI-Analyse: " + e.getMessage() + "</div>");
        }
    }

    @PostMapping("/admin/xml/rag/search")
    public ResponseEntity<String> searchSimilarXml(
            @RequestParam("query") String query,
            @RequestParam(value = "limit", defaultValue = "5") int limit) {
        
        try {
            XmlListResponse searchResults = xmlDokumentService.searchSimilarXml(query, limit);
            
            if (searchResults.dokumente().isEmpty()) {
                return ResponseEntity.ok(
                    "<div class='bg-yellow-50 p-4 rounded-lg border border-yellow-200'>" +
                    "<div class='flex items-center gap-2'>" +
                    "<span class='text-yellow-600'>🔍</span>" +
                    "<span class='font-bold text-yellow-700'>Keine ähnlichen XML-Dokumente gefunden</span>" +
                    "</div>" +
                    "<div class='mt-2 text-gray-600'>Für die Suche: \"" + query + "\"</div>" +
                    "</div>"
                );
            }

            StringBuilder html = new StringBuilder();
            html.append("<div class='bg-blue-50 p-4 rounded-lg border border-blue-200 mb-4'>");
            html.append("<div class='flex items-center gap-2 mb-2'>");
            html.append("<span class='text-blue-600'>🔍</span>");
            html.append("<span class='font-bold text-blue-700'>Semantische Suche: ").append(query).append("</span>");
            html.append("</div>");
            html.append("<div class='text-sm text-gray-600 mb-3'>").append(searchResults.dokumente().size()).append(" ähnliche Dokumente gefunden</div>");
            
            html.append("<div class='space-y-3'>");
            for (var doc : searchResults.dokumente()) {
                html.append("<div class='bg-white p-3 rounded border border-gray-200'>");
                html.append("<div class='flex justify-between items-start'>");
                html.append("<div>");
                html.append("<div class='font-medium text-gray-900'>").append(doc.dateiname()).append("</div>");
                html.append("<div class='text-sm text-gray-500'>Autor: ").append(doc.autorName()).append("</div>");
                html.append("</div>");
                if (doc.similarityScore() != null) {
                    html.append("<div class='text-sm font-medium text-blue-600'>");
                    html.append(String.format("%.1f%%", doc.similarityScore() * 100));
                    html.append("</div>");
                }
                html.append("</div>");
                html.append("<div class='mt-2 text-sm text-gray-700'>");
                html.append(doc.inhalt().length() > 300 ? doc.inhalt().substring(0, 300) + "..." : doc.inhalt());
                html.append("</div>");
                html.append("</div>");
            }
            html.append("</div>");
            html.append("</div>");

            return ResponseEntity.ok(html.toString());
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("<div class='text-red-600'>❌ Fehler bei der semantischen Suche: " + e.getMessage() + "</div>");
        }
    }

    @PostMapping("/admin/xml/rag/chat")
    public ResponseEntity<String> chatWithXmlRag(
            @RequestParam("query") String query,
            @RequestParam(value = "limit", defaultValue = "3") int limit) {
        
        try {
            // 1. Ähnliche XML-Dokumente finden
            XmlListResponse searchResults = xmlDokumentService.searchSimilarXml(query, limit);
            
            if (searchResults.dokumente().isEmpty()) {
                return ResponseEntity.ok(
                    "<div class='bg-yellow-50 p-4 rounded-lg border border-yellow-200'>" +
                    "<div class='flex items-center gap-2'>" +
                    "<span class='text-yellow-600'>🤖</span>" +
                    "<span class='font-bold text-yellow-700'>Keine relevanten XML-Dokumente gefunden</span>" +
                    "</div>" +
                    "<div class='mt-2 text-gray-600'>Für die Frage: \"" + query + "\"</div>" +
                    "</div>"
                );
            }

            // 2. Kontext aus relevanten Dokumenten erstellen
            StringBuilder context = new StringBuilder();
            context.append("Relevante XML-Dokumente für die Frage: ").append(query).append("\n\n");
            
            for (var doc : searchResults.dokumente()) {
                context.append("=== Dokument: ").append(doc.dateiname()).append(" (Autor: ").append(doc.autorName()).append(") ===\n");
                context.append(doc.inhalt()).append("\n\n");
            }

            // 3. KI-Antwort mit RAG-Kontext generieren
            String response = chatClient.prompt()
                .user("Du bist ein Experte für XML-Dokumente. Beantworte die Frage basierend auf den folgenden relevanten XML-Dokumenten.\n\n" +
                      "Kontext aus relevanten XML-Dokumenten:\n" + context.toString() + 
                      "\n\nFrage: " + query + 
                      "\n\nAntworte auf Deutsch und sei präzise. Zitiere wenn möglich aus den Dokumenten.")
                .call()
                .content();

            // 4. HTML-Antwort mit Suchresultaten und KI-Antwort erstellen
            StringBuilder html = new StringBuilder();
            
            // Suchresultate anzeigen
            html.append("<div class='bg-blue-50 p-4 rounded-lg border border-blue-200 mb-4'>");
            html.append("<div class='flex items-center gap-2 mb-2'>");
            html.append("<span class='text-blue-600'>🔍</span>");
            html.append("<span class='font-bold text-blue-700'>Gefundene XML-Dokumente</span>");
            html.append("</div>");
            html.append("<div class='text-sm text-gray-600 mb-3'>").append(searchResults.dokumente().size()).append(" relevante Dokumente für: \"").append(query).append("\"</div>");
            
            html.append("<div class='space-y-2'>");
            for (var doc : searchResults.dokumente()) {
                html.append("<div class='bg-white p-2 rounded border border-gray-200 text-sm'>");
                html.append("<div class='flex justify-between'>");
                html.append("<div class='font-medium'>").append(doc.dateiname()).append("</div>");
                if (doc.similarityScore() != null) {
                    html.append("<div class='text-blue-600'>").append(String.format("%.1f%%", doc.similarityScore() * 100)).append("</div>");
                }
                html.append("</div>");
                html.append("<div class='text-gray-500 text-xs'>Autor: ").append(doc.autorName()).append("</div>");
                html.append("</div>");
            }
            html.append("</div>");
            html.append("</div>");

            // KI-Antwort anzeigen
            html.append("<div class='bg-purple-50 p-4 rounded-lg border border-purple-200'>");
            html.append("<div class='flex items-center gap-2 mb-2'>");
            html.append("<span class='text-purple-600'>✨</span>");
            html.append("<span class='font-bold text-purple-700'>KI-Antwort (RAG)</span>");
            html.append("</div>");
            html.append("<div class='text-gray-800'>").append(response.replace("\n", "<br>")).append("</div>");
            html.append("</div>");

            return ResponseEntity.ok(html.toString());
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("<div class='text-red-600'>❌ Fehler bei der RAG-Chat: " + e.getMessage() + "</div>");
        }
    }
}
