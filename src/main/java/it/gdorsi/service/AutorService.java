package it.gdorsi.service;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import it.gdorsi.dao.AutorRequest;
import it.gdorsi.repository.AuthorRepository;
import it.gdorsi.repository.model.Autor;
import jakarta.transaction.Transactional;

/**
 * Eine Möglichkeit Kommunikation Transaktional zu machen.
 *  KLassischer Weg - also die Bean ruft dann saveAuthorWithVector
 *  NICHT über den Controller!!
 *  Transaktion auch git weil:
 *  Ollama-Latenz: KI-Modelle sind langsamer als SQL. Eine Transaktion stellt sicher,
 *  dass die Datenbankverbindung nicht "halbgar" offen bleibt, während auf die KI gewartet wird.
 */
@Service
public class AutorService {

    private final AuthorRepository repository;
    private final EmbeddingModel embeddingModel;

    public AutorService(AuthorRepository repository, EmbeddingModel embeddingModel) {
        this.repository = repository;
        this.embeddingModel = embeddingModel;
    }

    @Transactional // Alles oder nichts!
    public void saveAuthorWithVector(AutorRequest request) {
        // 1. Vektor erzeugen (wenn das schiefgeht, wird gar nicht erst gespeichert)
        float[] vector = embeddingModel.embed(request.biografie());

        // 2. Entity erstellen
        Autor author = new Autor(request.name(), request.biografie(), vector);

        // 3. In DB speichern
        repository.save(author);
    }
}
