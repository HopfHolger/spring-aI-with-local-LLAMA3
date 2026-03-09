package it.gdorsi.controller;

import java.io.IOException;
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

import it.gdorsi.repository.model.XmlDokument;
import it.gdorsi.service.XmlDokumentService;

@RestController
@RequestMapping("/api/autoren/{autorId}/xml")
public class XmlAutorRestController {

    private final XmlDokumentService xmlDokumentService;

    public XmlAutorRestController(XmlDokumentService xmlDokumentService) {
        this.xmlDokumentService = xmlDokumentService;
    }

    @PostMapping
    public ResponseEntity<String> createXml(
            @PathVariable Long autorId,
            @RequestParam("file") MultipartFile file) throws IOException {

        String inhalt = new String(file.getBytes());
        String dateiname = file.getOriginalFilename();

        xmlDokumentService.saveXml(autorId, dateiname, inhalt);

        return ResponseEntity.status(HttpStatus.CREATED).body("XML gespeichert: " + dateiname);
    }

    @GetMapping
    public ResponseEntity<List<XmlDokument>> getAllXmlForAutor(@PathVariable Long autorId) {
        List<XmlDokument> xmlDokumente = xmlDokumentService.findByAutorId(autorId);
        return ResponseEntity.ok(xmlDokumente);
    }

    @GetMapping("/{xmlId}")
    public ResponseEntity<XmlDokument> getXmlById(
            @PathVariable Long autorId,
            @PathVariable Long xmlId) {
        return xmlDokumentService.findByIdAndAutorId(xmlId, autorId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{xmlId}")
    public ResponseEntity<String> updateXml(
            @PathVariable Long autorId,
            @PathVariable Long xmlId,
            @RequestParam("file") MultipartFile file) throws IOException {

        String inhalt = new String(file.getBytes());
        String dateiname = file.getOriginalFilename();

        return xmlDokumentService.updateXml(autorId, xmlId, dateiname, inhalt)
                .map(updated -> ResponseEntity.ok("XML aktualisiert: " + dateiname))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{xmlId}")
    public ResponseEntity<Void> deleteXml(
            @PathVariable Long autorId,
            @PathVariable Long xmlId) {
        boolean deleted = xmlDokumentService.deleteXml(autorId, xmlId);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
