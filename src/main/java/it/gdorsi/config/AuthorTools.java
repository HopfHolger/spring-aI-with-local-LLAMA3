package it.gdorsi.config;

import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.dao.DataIntegrityViolationException;

import it.gdorsi.dao.AutorRequest;
import it.gdorsi.repository.AuthorRepository;
import it.gdorsi.repository.model.Autor;

@Configuration
public class AuthorTools {

    /**
     * Speichert einen Autor in der Datenbank.
     * Diese Bean wird als Function dem ChatClient übergeben, der entscheidet,
     * wann sie aufgerufen wird (z.B. durch einen speziellen Prompt).
     *<p>
     *     Fehlerfall:
     * Gib im catch(Exception e) niemals den kompletten Stacktrace (e.getMessage()) an die KI weiter.
     * Dort könnten Datenbank-Interna stehen, die ein Sicherheitsrisiko darstellen.
     * Präfixe nutzen: Begriffe wie ERFOLG, FEHLER oder ABGEBROCHEN helfen dem Modell (LLM),
     * den Status der Aktion sofort korrekt einzuordnen.
     * KI als Vermittler: Wenn das Tool den String "FEHLER: Name zu lang" zurückgibt,
     * liest die KI das und sagt dem Nutzer: "Entschuldigung, der Name ist zu lang, kannst du ihn kürzen?".
     * </p>
     * @param repo  Repository
     * @param model Ollama-EmbeddingModel
     * @return String
     */
    @Bean
    @Description("Speichert einen Autor und seine Biografie in die Datenbank.")
    public Function<AutorRequest, String> saveAuthor(
            AuthorRepository repo,   // Spring findet das Interface und reicht die Instanz rein
            EmbeddingModel model, // Spring findet das Ollama-EmbeddingModel und reicht es rein
            jakarta.validation.Validator validator) { // Spring AI bei Tool-Calling die @Valid-Annotation an Funktionsparametern nicht immer automatisch auswertet

        return request -> {
            // 2. HTML/Script Stripping (Optional)
            // String cleanBio = HtmlUtils.htmlEscape(request.biografie());
            // Validierung manuell prüfen
            var violations = validator.validate(request);
            if (!violations.isEmpty()) {
                String errorMsg = violations.stream()
                        .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                        .collect(Collectors.joining(", "));
                return "ABGEBROCHEN: Die Daten sind ungültig. Details: " + errorMsg;
            }
            // Hier nutzt du die Parameter direkt.
            // Die Function "merkt" sich diese Referenzen (Closure).
            float[] vector = model.embed(request.biografie());
            // spring recht safe hier - bei Queries natürlich benannte Params
            try {
                repo.save(new Autor(request.name(), request.biografie(), vector));
                return "Autor " + request.name() + " wurde erfolgreich in SQL und pgvector gespeichert.";
            } catch (DataIntegrityViolationException e) {
                // Falls z.B. der Name UNIQUE sein muss und schon existiert
                return "FEHLER: Ein Autor mit diesem Namen existiert bereits in der Datenbank.";
            } catch (Exception e) {
                return "SYSTEMFEHLER: Speichern aktuell nicht möglich. Bitte später versuchen.";
            }
        };
    }

}
