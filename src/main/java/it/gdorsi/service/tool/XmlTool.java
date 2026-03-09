package it.gdorsi.service.tool;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import it.gdorsi.repository.AuthorRepository;
import it.gdorsi.repository.XmlDokumentRepository;
import it.gdorsi.repository.model.Autor;
import it.gdorsi.repository.model.XmlDokument;
import it.gdorsi.service.XmlOperations;
import jakarta.transaction.Transactional;

@Component
public class XmlTool implements XmlOperations {

    private final XmlDokumentRepository xmlDokumentRepository;
    private final AuthorRepository authorRepository;
    private final EmbeddingModel embeddingModel;

    public XmlTool(XmlDokumentRepository xmlDokumentRepository, AuthorRepository authorRepository,
            EmbeddingModel embeddingModel) {
        this.xmlDokumentRepository = xmlDokumentRepository;
        this.authorRepository = authorRepository;
        this.embeddingModel = embeddingModel;
    }

    @Override
    @Transactional
    public String saveXml(String autorName, String dateiname, String xmlInhalt) {
        Autor autor = authorRepository.findByName(autorName);
        if (autor == null) {
            return "FEHLER: Autor '" + autorName + "' existiert nicht in der Datenbank.";
        }

        float[] vector = embeddingModel.embed(xmlInhalt);

        XmlDokument xmlDokument = new XmlDokument(dateiname, xmlInhalt, vector, autor);
        xmlDokumentRepository.save(xmlDokument);

        return "XML-Dokument '" + dateiname + "' wurde erfolgreich für Autor '" + autorName + "' gespeichert.";
    }

    @Override
    @Transactional
    public String deleteXmlByAutor(String autorName) {
        Autor autor = authorRepository.findByName(autorName);
        if (autor == null) {
            return "FEHLER: Autor '" + autorName + "' existiert nicht in der Datenbank.";
        }

        List<XmlDokument> dokumente = xmlDokumentRepository.findByAutorId(autor.getId());
        if (dokumente.isEmpty()) {
            return "Keine XML-Dokumente für Autor '" + autorName + "' gefunden.";
        }

        xmlDokumentRepository.deleteByAutorId(autor.getId());
        return dokumente.size() + " XML-Dokument(e) für Autor '" + autorName + "' wurden gelöscht.";
    }

    @Override
    public String getXmlByAutor(String autorName) {
        Autor autor = authorRepository.findByName(autorName);
        if (autor == null) {
            return "FEHLER: Autor '" + autorName + "' existiert nicht in der Datenbank.";
        }

        List<XmlDokument> dokumente = xmlDokumentRepository.findByAutorId(autor.getId());
        if (dokumente.isEmpty()) {
            return "Keine XML-Dokumente für Autor '" + autorName + "' gefunden.";
        }

        return dokumente.stream()
                .map(d -> "Datei: " + d.getDateiname() + "\nInhalt:\n" + d.getInhalt())
                .collect(Collectors.joining("\n\n---\n\n"));
    }
}
