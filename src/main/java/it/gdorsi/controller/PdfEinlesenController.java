package it.gdorsi.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.gdorsi.service.PdfIngestionService;

@RestController
@RequestMapping("/ingest")
public class PdfEinlesenController {

    private final PdfIngestionService ingestionService;

    @Value("classpath:Lebenslauf Holger Hopf.pdf")
    private Resource defaultPdf;

    public PdfEinlesenController(PdfIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/pdf")
    public ResponseEntity<String> runIngestion() {
        ingestionService.loadPdf(defaultPdf);
        return ResponseEntity.ok("PDF wurde verarbeitet und vektorisiert.");
    }
}
