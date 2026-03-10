package it.gdorsi.integrationtest.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import config.TestConfig;
import it.gdorsi.repository.AuthorRepository;
import it.gdorsi.repository.XmlDokumentRepository;
import it.gdorsi.repository.model.Autor;
import it.gdorsi.repository.model.XmlDokument;
import it.gdorsi.service.XmlDokumentService;

import java.util.List;
import java.util.Optional;

@SpringBootTest
@Import(TestConfig.class)
@ActiveProfiles("test")
class XmlAutorChatControllerIdempotenzTest {

    @Autowired
    private XmlDokumentService xmlDokumentService;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private XmlDokumentRepository xmlDokumentRepository;

    @BeforeEach
    void setUp() {
        xmlDokumentRepository.deleteAll();
        authorRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        xmlDokumentRepository.deleteAll();
        authorRepository.deleteAll();
    }

    @Test
    @DisplayName("PUT ist idempotent - zweimaliges PUT erstellt nur ein Dokument")
    void putXml_idempotent_twiceCreatesOnlyOneDocument() {
        Autor autor = new Autor();
        autor.setName("Test Autor");
        autor.setAuthorEmbedding(new float[1024]);
        autor = authorRepository.save(autor);

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
        Autor autor = new Autor();
        autor.setName("Test Autor");
        autor.setAuthorEmbedding(new float[1024]);
        autor = authorRepository.save(autor);

        XmlDokument created = xmlDokumentService.saveXml(autor.getId(), "test.xml", "<test>daten</test>");
        
        Optional<XmlDokument> update = xmlDokumentService.updateXml(autor.getId(), created.getId(), "updated.xml", "<updated>neue daten</updated>");
        assertThat(update).isPresent();

        List<XmlDokument> allXml = xmlDokumentRepository.findAll();
        assertThat(allXml).hasSize(1);
        assertThat(allXml.getFirst().getDateiname()).isEqualTo("updated.xml");
        assertThat(allXml.getFirst().getInhalt()).isEqualTo("<updated>neue daten</updated>");
    }
}
