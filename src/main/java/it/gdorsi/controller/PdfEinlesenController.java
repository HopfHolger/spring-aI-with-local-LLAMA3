package it.gdorsi.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import it.gdorsi.dao.PdfIngestResult;
import it.gdorsi.service.DocumentManagementService;
import it.gdorsi.service.PdfIngestionService;

/**
 * @RestController erwartet, dass du Daten (JSON) zurückgibst,
 * während @Controller nach HTML-Dateien im templates-Ordner sucht. - Thymeleaf
 */
@Controller
public class PdfEinlesenController {

    private final PdfIngestionService ingestionService;

    private final DocumentManagementService documentService;


    public PdfEinlesenController(PdfIngestionService ingestionService, DocumentManagementService documentService) {
        this.ingestionService = ingestionService;
        this.documentService = documentService;
    }

    // Die Hauptseite
    @GetMapping("/admin")
    public String adminPage(Model model) {
        model.addAttribute("docs", documentService.getAllDocuments());
        return "admin";
    }

    // Nur das Tabellen-Fragment für HTMX-Updates
    @GetMapping("/admin/list")
    public String getDocumentList(Model model) {
        model.addAttribute("docs", documentService.getAllDocuments());
        return "admin :: docTable"; // Gibt nur das Fragment zurück
    }

    @PostMapping("/admin/upload/trigger")
    @ResponseBody // Antwortet direkt für HTMX
    public String handleFileUploadTrigger(@RequestParam("file") MultipartFile file) {
        ingestionService.loadPdf(file.getResource());
        // Triggert ein Event im Frontend, damit die Liste aktualisiert wird
        return "";
    }

    @PostMapping(value = "/admin/upload", produces = org.springframework.http.MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> handleFileUpload(@RequestParam("file") MultipartFile file,
                                                   RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Bitte wähle eine Datei aus.");
            return ResponseEntity.status(400)
                    .body("<div class='text-red-600'>❌ Fehler: leeres File </div>");
        }

        multipartInResource(file, redirectAttributes);

        try {

            // Wir schicken ein kleines HTML-Fragment zurück, das HTMX anzeigen kann
            return ResponseEntity.ok()
                    .header("HX-Trigger", "updateList") // Triggert das Neuladen der Tabelle
                    .body("<div class='text-green-600'>✅ Datei erfolgreich indiziert!</div>");
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("<div class='text-red-600'>❌ Fehler: " + e.getMessage() + "</div>");
        }
    }

    private void multipartInResource(MultipartFile file, RedirectAttributes redirectAttributes) {
        try {
            // MultipartFile in Resource umwandeln für den Service
            final Resource resource = file.getResource();
            PdfIngestResult result = ingestionService.loadPdf(resource);

            redirectAttributes.addFlashAttribute("message",
                    "Erfolg! " + result.fileName() + " wurde mit " + result.totalChunks() + " Chunks eingelesen.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Fehler: " + e.getMessage());
        }
    }
}
