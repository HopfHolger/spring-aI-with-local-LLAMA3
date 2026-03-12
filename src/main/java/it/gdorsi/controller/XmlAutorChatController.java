package it.gdorsi.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.HtmlUtils;

import it.gdorsi.service.XmlDokumentService;
import it.gdorsi.service.response.XmlListResponse;
import it.gdorsi.service.response.XmlResponse;

@RestController
@RequestMapping("/api/autoren/{autorId}/xml")
public class XmlAutorChatController {

    private final ChatClient chatClient;
    private final XmlDokumentService xmlDokumentService;

    public XmlAutorChatController(ChatClient chatClient,
                                 XmlDokumentService xmlDokumentService) {
        this.chatClient = chatClient;
        this.xmlDokumentService = xmlDokumentService;
    }

    @PostMapping
    public ResponseEntity<String> createXml(
            @PathVariable Long autorId,
            @RequestParam("file") MultipartFile file) throws IOException {

        String inhalt = new String(file.getBytes());
        String dateiname = file.getOriginalFilename();

        xmlDokumentService.saveXml(autorId, dateiname, inhalt);

        return ResponseEntity.status(HttpStatus.CREATED).body("XML gespeichert: " + HtmlUtils.htmlEscape(dateiname));
    }

    @GetMapping
    public ResponseEntity<?> getAllXmlForAutor(@PathVariable Long autorId) {
        String autorName = getAutorNameById(autorId);
        if (autorName == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Autor mit ID " + HtmlUtils.htmlEscape(String.valueOf(autorId)) + " nicht gefunden");
        }

        try {
            XmlListResponse xmlListResponse = xmlDokumentService.getXmlByAutor(autorName);
            
            if (xmlListResponse.status().startsWith("FEHLER")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(HtmlUtils.htmlEscape(xmlListResponse.status()));
            } else if (xmlListResponse.status().startsWith("KEINE_DOKUMENTE")) {
                return ResponseEntity.ok(List.of());
            }
            
            return ResponseEntity.ok(xmlListResponse.dokumente());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler beim Abrufen der XML-Dokumente: " + HtmlUtils.htmlEscape(e.getMessage()));
        }
    }

    @GetMapping("/{xmlId}")
    public ResponseEntity<?> getXmlById(
            @PathVariable Long autorId,
            @PathVariable Long xmlId) {
        
        String autorName = getAutorNameById(autorId);
        if (autorName == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Autor mit ID " + HtmlUtils.htmlEscape(String.valueOf(autorId)) + " nicht gefunden");
        }

        try {
            XmlResponse xmlResponse = xmlDokumentService.getXmlById(autorName, xmlId);
            
            if (xmlResponse.status().startsWith("FEHLER")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(HtmlUtils.htmlEscape(xmlResponse.status()));
            }
            
            return ResponseEntity.ok(xmlResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler beim Abrufen des XML-Dokuments: " + HtmlUtils.htmlEscape(e.getMessage()));
        }
    }

    @PutMapping("/{xmlId}")
    public ResponseEntity<?> updateXml(
            @PathVariable Long autorId,
            @PathVariable Long xmlId,
            @RequestParam("file") MultipartFile file) throws IOException {
        
        String autorName = getAutorNameById(autorId);
        if (autorName == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Autor mit ID " + HtmlUtils.htmlEscape(String.valueOf(autorId)) + " nicht gefunden");
        }

        String inhalt = new String(file.getBytes());
        String dateiname = file.getOriginalFilename();

        try {
            XmlResponse xmlResponse = xmlDokumentService.updateXml(autorName, xmlId, dateiname, inhalt);
            
            if (xmlResponse.status().startsWith("FEHLER")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(HtmlUtils.htmlEscape(xmlResponse.status()));
            }
            
            return ResponseEntity.ok("XML aktualisiert: " + HtmlUtils.htmlEscape(dateiname));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler beim Aktualisieren des XML-Dokuments: " + HtmlUtils.htmlEscape(e.getMessage()));
        }
    }

    @DeleteMapping("/{xmlId}")
    public ResponseEntity<?> deleteXml(
            @PathVariable Long autorId,
            @PathVariable Long xmlId) {
        
        String autorName = getAutorNameById(autorId);
        if (autorName == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Autor mit ID " + HtmlUtils.htmlEscape(String.valueOf(autorId)) + " nicht gefunden");
        }

        try {
            String response = xmlDokumentService.deleteXmlById(autorName, xmlId);
            
            if (response.startsWith("FEHLER")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(HtmlUtils.htmlEscape(response));
            }
            
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler beim Löschen des XML-Dokuments: " + HtmlUtils.htmlEscape(e.getMessage()));
        }
    }

    private String getAutorNameById(Long autorId) {
        return xmlDokumentService.getAutorNameById(autorId);
    }
}