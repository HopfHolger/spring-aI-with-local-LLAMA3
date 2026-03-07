package it.gdorsi.service;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class XmlAutorService {

    public File createXmlFuerAutor(Long autorId, File xmlFile) {
        return null;
    }

    public Optional<File> getXmlByAutorId(Long autorId) {
        return Optional.empty();
    }

    public List<File> getAllXmlForAutor(Long autorId) {
        return List.of();
    }

    public Optional<File> updateXmlForAutor(Long autorId, File xmlFile) {
        return Optional.empty();
    }

    public boolean deleteXmlForAutor(Long autorId) {
        return false;
    }
}
