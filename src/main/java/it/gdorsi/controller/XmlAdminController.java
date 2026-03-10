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
}
