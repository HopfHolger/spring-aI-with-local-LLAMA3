package it.gdorsi.integration.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import it.gdorsi.repository.AuthorRepository;
import it.gdorsi.repository.XmlDokumentRepository;
import it.gdorsi.repository.model.Autor;
import it.gdorsi.repository.model.XmlDokument;
import it.gdorsi.service.XmlDokumentService;

import java.util.List;
import java.util.Optional;

/**
 * Aud H2-Ebene, ohne Controller, da es Probleme mit Spring Boot 4
 * und Multipart upload gibt.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class XmlAutorRestControllerIdempotenzTest {

    @Autowired
    private XmlDokumentService xmlDokumentService;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private XmlDokumentRepository xmlDokumentRepository;

    private Autor autor;

    @BeforeEach
    void setUp() {
        xmlDokumentRepository.deleteAll();
        authorRepository.deleteAll();

        autor = new Autor();
        autor.setName("Test Autor");
        autor = authorRepository.save(autor);
    }

    @AfterEach
    void tearDown() {
        xmlDokumentRepository.deleteAll();
        authorRepository.deleteAll();
    }

    @Test
    @DisplayName("PUT ist idempotent - zweimaliges PUT erstellt nur ein Dokument")
    void putXml_idempotent_twiceCreatesOnlyOneDocument() {
        XmlDokument created = xmlDokumentService.saveXml(autor.getId(), "test.xml", "<test>daten</test>");
        
        Optional<XmlDokument> firstUpdate = xmlDokumentService.updateXml(autor.getId(), created.getId(), "test.xml", "<test>daten</test>");
        assertThat(firstUpdate).isPresent();
        
        Optional<XmlDokument> secondUpdate = xmlDokumentService.updateXml(autor.getId(), created.getId(), "test.xml", "<test>daten</test>");
        assertThat(secondUpdate).isPresent();

        List<XmlDokument> allXml = xmlDokumentRepository.findAll();
        assertThat(allXml).hasSize(1);
    }

    @Test
    @DisplayName("PUT mit unterschiedlichen Inhalten aktualisiert bestehendes Dokument")
    void putXml_differentContent_updatesExistingDocument() {
        XmlDokument created = xmlDokumentService.saveXml(autor.getId(), "test.xml", "<test>daten</test>");
        
        Optional<XmlDokument> update = xmlDokumentService.updateXml(autor.getId(), created.getId(), "updated.xml", "<updated>neue daten</updated>");
        assertThat(update).isPresent();

        List<XmlDokument> allXml = xmlDokumentRepository.findAll();
        assertThat(allXml).hasSize(1);
        assertThat(allXml.get(0).getDateiname()).isEqualTo("updated.xml");
        assertThat(allXml.get(0).getInhalt()).isEqualTo("<updated>neue daten</updated>");
    }
}
