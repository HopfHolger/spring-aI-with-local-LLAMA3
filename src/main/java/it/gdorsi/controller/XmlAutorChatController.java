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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.gdorsi.service.XmlDokumentService;
import it.gdorsi.service.XmlOperations;
import it.gdorsi.service.response.XmlListResponse;
import it.gdorsi.service.response.XmlResponse;

@RestController
@RequestMapping("/api/autoren/{autorId}/xml")
public class XmlAutorChatController {

    private final ChatClient chatClient;
    private final XmlDokumentService xmlDokumentService;
    private final ObjectMapper objectMapper;

    public XmlAutorChatController(ChatClient chatClient,
                                 XmlOperations xmlOperations,
                                 XmlDokumentService xmlDokumentService) {
        this.chatClient = chatClient;
        this.xmlDokumentService = xmlDokumentService;
        this.objectMapper = new ObjectMapper();
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
            String response = chatClient.prompt()
                .user("Hole alle XML-Dokumente für Autor " + autorName)
                .call()
                .content();

            XmlListResponse xmlListResponse = objectMapper.readValue(response, XmlListResponse.class);
            
            if (xmlListResponse.status().startsWith("FEHLER")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(HtmlUtils.htmlEscape(xmlListResponse.status()));
            } else if (xmlListResponse.status().startsWith("KEINE_DOKUMENTE")) {
                return ResponseEntity.ok(List.of());
            }
            
            return ResponseEntity.ok(xmlListResponse.dokumente());
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler beim Parsen der Response: " + HtmlUtils.htmlEscape(e.getMessage()));
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
            String response = chatClient.prompt()
                .user("Hole XML-Dokument mit ID " + xmlId + " für Autor " + autorName)
                .call()
                .content();

            XmlResponse xmlResponse = objectMapper.readValue(response, XmlResponse.class);
            
            if (xmlResponse.status().startsWith("FEHLER")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(HtmlUtils.htmlEscape(xmlResponse.status()));
            }
            
            return ResponseEntity.ok(xmlResponse);
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler beim Parsen der Response: " + HtmlUtils.htmlEscape(e.getMessage()));
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
            String response = chatClient.prompt()
                .user("Aktualisiere XML-Dokument mit ID " + xmlId + " für Autor " + autorName + 
                      " mit Dateiname " + dateiname + " und Inhalt: " + inhalt.substring(0, Math.min(100, inhalt.length())) + "...")
                .call()
                .content();

            XmlResponse xmlResponse = objectMapper.readValue(response, XmlResponse.class);
            
            if (xmlResponse.status().startsWith("FEHLER")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(HtmlUtils.htmlEscape(xmlResponse.status()));
            }
            
            return ResponseEntity.ok("XML aktualisiert: " + HtmlUtils.htmlEscape(dateiname));
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler beim Parsen der Response: " + HtmlUtils.htmlEscape(e.getMessage()));
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
            String response = chatClient.prompt()
                .user("Lösche XML-Dokument mit ID " + xmlId + " für Autor " + autorName)
                .call()
                .content();

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