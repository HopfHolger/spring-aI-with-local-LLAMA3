package it.gdorsi.controller;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import it.gdorsi.dao.AutorRequest;
import it.gdorsi.repository.AuthorRepository;

@Controller
@RequestMapping("/admin")
public class AutorAdminController {

    private final EmbeddingModel embeddingModel;
    private final AuthorRepository repository;

    public AutorAdminController(EmbeddingModel embeddingModel, AuthorRepository repository) {
        this.embeddingModel = embeddingModel;
        this.repository = repository;
    }

    // 1. Liefert die Liste der Autoren (wird von hx-get="/admin/autoren/list" aufgerufen)
    @GetMapping("/autoren/list")
    public String listAutoren(Model model) {
        model.addAttribute("autoren", repository.findAll());
        return "admin :: authorTable";
    }

    // 2. Liefert das Edit-Formular (wird vom Edit-Button aufgerufen)
    @GetMapping("/edit/{name}")
    public String editForm(@PathVariable String name, Model model) {
        var autor = repository.findByName(name);
        // Falls findByName Optional ist: repository.findByName(name).orElse(null)
        model.addAttribute("autor", autor);
        return "admin :: editFragment";
    }

    // 3. Verarbeitet das Update
    @PutMapping("/autor/{name}")
    public String updateAutor(@PathVariable String name,
                              @ModelAttribute AutorRequest request,
                              Model model) {
        try {
            // Vektor berechnen & Speichern
            float[] vector = embeddingModel.embed(request.biografie());
            repository.updateBiografieByName(name, request.biografie(), vector);

            // Nach dem Speichern die aktualisierte Liste zurückgeben
            model.addAttribute("autoren", repository.findAll());
            return "admin :: authorTable";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Fehler beim Update: " + e.getMessage());
            return "admin :: errorFragment";
        }
    }
}



