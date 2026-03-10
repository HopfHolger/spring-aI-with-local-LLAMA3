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
import it.gdorsi.service.response.XmlListResponse;
import it.gdorsi.service.response.XmlResponse;
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
    public XmlListResponse getXmlByAutor(String autorName) {
        Autor autor = authorRepository.findByName(autorName);
        if (autor == null) {
            return XmlListResponse.error("Autor '" + autorName + "' existiert nicht in der Datenbank.");
        }

        List<XmlDokument> dokumente = xmlDokumentRepository.findByAutorId(autor.getId());
        if (dokumente.isEmpty()) {
            return XmlListResponse.empty(autorName);
        }

        List<XmlResponse> xmlResponses = dokumente.stream()
                .map(d -> XmlResponse.success(d.getId(), d.getDateiname(), d.getInhalt(), autorName))
                .collect(Collectors.toList());

        return XmlListResponse.success(xmlResponses);
    }

    @Override
    public XmlResponse getXmlById(String autorName, Long xmlId) {
        Autor autor = authorRepository.findByName(autorName);
        if (autor == null) {
            return XmlResponse.error(autorName, "Autor existiert nicht in der Datenbank.");
        }

        return xmlDokumentRepository.findById(xmlId)
                .filter(d -> d.getAutor().getId().equals(autor.getId()))
                .map(d -> XmlResponse.success(d.getId(), d.getDateiname(), d.getInhalt(), autorName))
                .orElse(XmlResponse.notFound(autorName, xmlId));
    }

    @Override
    @Transactional
    public XmlResponse updateXml(String autorName, Long xmlId, String dateiname, String xmlInhalt) {
        Autor autor = authorRepository.findByName(autorName);
        if (autor == null) {
            return XmlResponse.error(autorName, "Autor existiert nicht in der Datenbank.");
        }

        return xmlDokumentRepository.findById(xmlId)
                .filter(d -> d.getAutor().getId().equals(autor.getId()))
                .map(xmlDokument -> {
                    xmlDokument.setDateiname(dateiname);
                    xmlDokument.setInhalt(xmlInhalt);
                    
                    float[] vector = embeddingModel.embed(xmlInhalt);
                    xmlDokument.setXmlEmbedding(vector);
                    
                    XmlDokument updated = xmlDokumentRepository.save(xmlDokument);
                    return XmlResponse.success(updated.getId(), updated.getDateiname(), updated.getInhalt(), autorName);
                })
                .orElse(XmlResponse.notFound(autorName, xmlId));
    }

    @Override
    @Transactional
    public String deleteXmlById(String autorName, Long xmlId) {
        Autor autor = authorRepository.findByName(autorName);
        if (autor == null) {
            return "FEHLER: Autor '" + autorName + "' existiert nicht in der Datenbank.";
        }

        return xmlDokumentRepository.findById(xmlId)
                .filter(d -> d.getAutor().getId().equals(autor.getId()))
                .map(xmlDokument -> {
                    xmlDokumentRepository.delete(xmlDokument);
                    return "XML-Dokument mit ID " + xmlId + " für Autor '" + autorName + "' wurde gelöscht.";
                })
                .orElse("FEHLER: XML-Dokument mit ID " + xmlId + " für Autor '" + autorName + "' nicht gefunden.");
    }
}
