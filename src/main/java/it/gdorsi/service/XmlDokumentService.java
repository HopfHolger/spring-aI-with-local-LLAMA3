package it.gdorsi.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import it.gdorsi.repository.AuthorRepository;
import it.gdorsi.repository.XmlDokumentRepository;
import it.gdorsi.repository.model.Autor;
import it.gdorsi.repository.model.XmlDokument;

@Service
public class XmlDokumentService {

    private final XmlDokumentRepository xmlDokumentRepository;
    private final AuthorRepository authorRepository;

    public XmlDokumentService(XmlDokumentRepository xmlDokumentRepository, AuthorRepository authorRepository) {
        this.xmlDokumentRepository = xmlDokumentRepository;
        this.authorRepository = authorRepository;
    }

    public XmlDokument saveXml(Long autorId, String dateiname, String inhalt) {
        Autor autor = authorRepository.findById(autorId).orElseThrow();
        XmlDokument xmlDokument = new XmlDokument(dateiname, inhalt, null, autor);
        return xmlDokumentRepository.save(xmlDokument);
    }

    public List<XmlDokument> findByAutorId(Long autorId) {
        return xmlDokumentRepository.findByAutorId(autorId);
    }

    public Optional<XmlDokument> findByIdAndAutorId(Long xmlId, Long autorId) {
        return xmlDokumentRepository.findById(xmlId)
                .filter(d -> d.getAutor().getId().equals(autorId));
    }

    public Optional<XmlDokument> updateXml(Long autorId, Long xmlId, String dateiname, String inhalt) {
        return xmlDokumentRepository.findById(xmlId)
                .filter(d -> d.getAutor().getId().equals(autorId))
                .map(xmlDokument -> {
                    xmlDokument.setDateiname(dateiname);
                    xmlDokument.setInhalt(inhalt);
                    return xmlDokumentRepository.save(xmlDokument);
                });
    }

    public boolean deleteXml(Long autorId, Long xmlId) {
        return xmlDokumentRepository.findById(xmlId)
                .filter(d -> d.getAutor().getId().equals(autorId))
                .map(xmlDokument -> {
                    xmlDokumentRepository.delete(xmlDokument);
                    return true;
                })
                .orElse(false);
    }

    public Optional<XmlDokument> findById(Long id) {
        return xmlDokumentRepository.findById(id);
    }

    public void deleteXmlById(Long id) {
        xmlDokumentRepository.findById(id).ifPresent(xmlDokumentRepository::delete);
    }

    public List<Autor> findAllAutoren() {
        return authorRepository.findAll();
    }

    public List<XmlDokument> findAllXmlDokumente() {
        List<Autor> autoren = authorRepository.findAll();
        return autoren.stream()
                .flatMap(a -> xmlDokumentRepository.findByAutorId(a.getId()).stream())
                .toList();
    }
}
