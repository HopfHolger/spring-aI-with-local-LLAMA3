package it.gdorsi.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import it.gdorsi.repository.model.Autor;
import it.gdorsi.repository.model.XmlDokument;

public interface XmlDokumentRepository extends JpaRepository<XmlDokument, Long> {

    List<XmlDokument> findByAutorId(Long autorId);

    List<XmlDokument> findByAutor(Autor autor);

    void deleteByAutorId(Long autorId);
}
