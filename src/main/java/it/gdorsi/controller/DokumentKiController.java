package it.gdorsi.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import it.gdorsi.dao.AutorRequest;
import it.gdorsi.dao.Dokument;
import it.gdorsi.repository.AuthorRepository;

@RestController
@RequestMapping("/api/dokumente")
public class DokumentKiController {

    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;
    private final AuthorRepository repository;

    public DokumentKiController(ChatClient.Builder builder, EmbeddingModel embeddingModel, AuthorRepository repository) {
        this.chatClient = builder.build();
        this.embeddingModel = embeddingModel;
        this.repository = repository;
    }

    @PostMapping("/edit")
    public Dokument editiereDokument(@RequestBody Dokument input) {

        // Die KI erhält den Inhalt und eine Anweisung
        String editierterInhalt = chatClient.prompt()
                .system("Du bist ein Lektor. Korrigiere Grammatik und Stil, bewahre aber den Kern.")
                .user(input.inhalt())
                .call()
                .content();

        // Neues Objekt mit dem KI-Inhalt zurückgeben
        return new Dokument(input.titel(), editierterInhalt, "Korrekturgelesen");
    }

    @GetMapping("/admin/edit/{name}")
    public String editForm(@PathVariable String name, Model model) {
        var autor = repository.findByName(name);
        if (autor == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Autor nicht gefunden");
        model.addAttribute("autor", autor);
        return "admin :: editFragment"; // Wir nutzen ein Thymeleaf Fragment
    }

    @PutMapping("/admin/autor/{name}")
    public String updateAutor(@PathVariable String name, @ModelAttribute
    AutorRequest request, Model model) {
        try {
            // 1. Embedding API Call (kann fehlschlagen/timeout)
            float[] vector = embeddingModel.embed(request.biografie());

            // 2. DB Update
            repository.updateBiografieByName(name, request.biografie(), vector);

            model.addAttribute("successMessage", "Autor erfolgreich aktualisiert!");
            model.addAttribute("autoren", repository.findAll());
            return "admin :: autorListe";

        } catch (RuntimeException e) {
            // Fehler bei der KI-Schnittstelle
            model.addAttribute("errorMessage", "KI-Service nicht erreichbar. Biografie wurde nicht gespeichert.");
            return "admin :: errorFragment";
        } catch (Exception e) {
            // Fehler in der Datenbank
            model.addAttribute("errorMessage", "Datenbankfehler beim Speichern.");
            return "admin :: errorFragment";
        }
    }
}
