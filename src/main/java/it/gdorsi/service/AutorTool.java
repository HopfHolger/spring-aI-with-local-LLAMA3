package it.gdorsi.service;

import java.util.stream.Collectors;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import it.gdorsi.dao.AutorRequest;
import it.gdorsi.repository.AuthorRepository;
import it.gdorsi.repository.model.Autor;
import jakarta.transaction.Transactional;

/**
 * Eine Möglichkeit Kommunikation Transaktional zu machen.
 * KLassischer Weg - also die Bean ruft dann saveAuthorWithVector
 * NICHT über den Controller!!
 * Transaktion auch git weil:
 * Ollama-Latenz: KI-Modelle sind langsamer als SQL. Eine Transaktion stellt sicher,
 * dass die Datenbankverbindung nicht "halbgar" offen bleibt, während auf die KI gewartet wird.
 */

@Component
public class AutorTool implements AutorOperations {

    private final AuthorRepository repository;
    private final EmbeddingModel embeddingModel;
    private final jakarta.validation.Validator validator;

    public AutorTool(AuthorRepository repository, EmbeddingModel embeddingModel, jakarta.validation.Validator validator) {
        this.repository = repository;
        this.embeddingModel = embeddingModel;
        this.validator = validator;
    }

    /**
     * JSON-Mapping: Stelle sicher, dass dein AutorRequest (ein Record) korrekt von Jackson deserialisiert werden kann.
     * Ollama übergibt die Argumente als JSON-String.
     * Positiv-Test: "Speichere den Autor Sebastian Fitzek, er ist ein bekannter deutscher Psychothriller-Autor." -> Check: Geht er in den Breakpoint?
     * Nachfrage-Test: "Ich möchte einen Autor namens Stephen King anlegen." -> Check: Fragt die KI nach der Biografie, statt das Tool mit leerem Feld zu rufen?
     * @param name Autor
     * @param biografie kurzbiografie
     * @return String dann kann mistral den Aufrufer sagen was passiert ist ...
     */
    @Override
    @Transactional
    public String saveAutor(String name, String biografie) {
        System.out.println("AutorService: " + name);
        AutorRequest request = new AutorRequest(name, biografie);
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String errorMsg = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));
            return "ABGEBROCHEN: Die Daten sind ungültig. Details: " + errorMsg;
        }
        // 1. Vektor erzeugen (wenn das schiefgeht, wird gar nicht erst gespeichert)
        float[] vector = embeddingModel.embed(request.biografie());

        // 2. Entity erstellen
        Autor author = new Autor(request.name(), request.biografie(), vector);

        try {
            // 3. In DB speichern
            repository.save(author);

            return "Autor " + request.name() + " wurde erfolgreich in der Datenbank gespeichert.";

        } catch (
                DataIntegrityViolationException e) {
            // Falls z.B. der Name UNIQUE sein muss und schon existiert
            return "FEHLER: Ein Autor mit diesem Namen existiert bereits in der Datenbank.";
        } catch (Exception e) {
            return "SYSTEMFEHLER: Speichern aktuell nicht möglich. Bitte später versuchen.";
        }
    }

    // UPDATE: Die KI erkennt am Namen/Biografie, was sich ändern soll ohne interface
    @Tool(description = "Aktualisiert die Biografie eines bestehenden Autors")
    public String updateAutor(String name, String neueBiografie) {
        // 1. Neuen Vektor berechnen
        float[] vector = embeddingModel.embed(neueBiografie);

        // direkt an die DB, schneller
        int rows = repository.updateBiografieByName(name, neueBiografie, vector);

        return rows > 0 ? "Update für " + name + " erfolgreich" : "Autor nicht gefunden";
    }

    // DELETE: Einfaches Löschen per Name
    @Override
    @Transactional
    public String deleteAutor(String name) {
        if (repository.existsByName(name)) {
            repository.deleteByName(name);
            return "Autor " + name + " wurde gelöscht.";
        }
        return "Löschen fehlgeschlagen: Autor " + name + " existiert nicht.";
    }
}
