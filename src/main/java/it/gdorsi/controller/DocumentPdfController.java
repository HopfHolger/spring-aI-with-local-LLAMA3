package it.gdorsi.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import it.gdorsi.dao.DocumentOverview;
import it.gdorsi.service.DocumentManagementService;

@Controller
public class DocumentPdfController {

    private final DocumentManagementService documentService;

    public DocumentPdfController(DocumentManagementService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public List<DocumentOverview> list() {
        return documentService.getAllDocuments();
    }

    @DeleteMapping("/admin/documents/{fileName}")
    @ResponseBody
    public ResponseEntity<Void> deleteDocument(@PathVariable String fileName) {
        // Deine integrierte Logik aufrufen
        documentService.deleteByFileName(fileName);

        // HTTP 200 zurückgeben, damit HTMX weiß: "Alles klar, Zeile entfernen"
        return ResponseEntity.ok().build();
    }
}

