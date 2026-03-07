package it.gdorsi.controller;

import java.io.File;
import java.util.List;

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

import it.gdorsi.service.XmlAutorService;

@RestController
@RequestMapping("/api/autoren/{autorId}/xml")
public class XmlAutorRestController {

    private final XmlAutorService xmlAutorService;

    public XmlAutorRestController(XmlAutorService xmlAutorService) {
        this.xmlAutorService = xmlAutorService;
    }

    @PostMapping
    public ResponseEntity<File> createXml(
            @PathVariable Long autorId,
            @RequestParam("file") MultipartFile file) {
        File tempFile = convertToFile(file);
        File savedFile = xmlAutorService.createXmlFuerAutor(autorId, tempFile);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedFile);
    }

    @GetMapping
    public ResponseEntity<List<File>> getAllXmlForAutor(@PathVariable Long autorId) {
        List<File> xmlFiles = xmlAutorService.getAllXmlForAutor(autorId);
        return ResponseEntity.ok(xmlFiles);
    }

    @GetMapping("/{xmlId}")
    public ResponseEntity<File> getXmlById(
            @PathVariable Long autorId,
            @PathVariable Long xmlId) {
        return xmlAutorService.getXmlByAutorId(autorId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{xmlId}")
    public ResponseEntity<File> updateXml(
            @PathVariable Long autorId,
            @PathVariable Long xmlId,
            @RequestParam("file") MultipartFile file) {
        File tempFile = convertToFile(file);
        return xmlAutorService.updateXmlForAutor(autorId, tempFile)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{xmlId}")
    public ResponseEntity<Void> deleteXml(
            @PathVariable Long autorId,
            @PathVariable Long xmlId) {
        if (xmlAutorService.deleteXmlForAutor(autorId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private File convertToFile(MultipartFile multipartFile) {
        try {
            File tempFile = File.createTempFile("xml-", ".xml");
            multipartFile.transferTo(tempFile);
            return tempFile;
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Konvertieren der Datei", e);
        }
    }
}
