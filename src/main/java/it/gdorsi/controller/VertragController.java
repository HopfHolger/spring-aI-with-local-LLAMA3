package it.gdorsi.controller;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import it.gdorsi.repository.VertragRepository;
import it.gdorsi.repository.model.Vertrag;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.stream.Collectors;


@Controller
@RequestMapping("/admin/vertraege")
public class VertragController {

    private final VertragRepository repository;
    private final ChatModel chatModel;           // KI Modell direkt
    private final VectorStore vectorStore;       // Vektor-DB für RAG
    private final List<ToolCallback> toolCallbacks; // Deine Interface-Tools

    public VertragController(VertragRepository repository,
                             ChatModel chatModel,
                             VectorStore vectorStore,
                             List<ToolCallback> toolCallbacks) {
        this.repository = repository;
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
        this.toolCallbacks = toolCallbacks;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("vertraege", repository.findAll());
        model.addAttribute("vertrag", new Vertrag());
        return "vertrag-form";
    }

    @PostMapping("/chat")
    public String chat(@RequestParam("question") String question, Model model, HttpServletResponse response) {
        try {
            // 1. RAG: Suche in der Vektor-DB
            var docs = vectorStore.similaritySearch(SearchRequest.builder()
                    .query(question)
                    .topK(3)
                    .build());

            String context = docs.stream()
                    .map(org.springframework.ai.document.Document::getText) // .getText() statt .getContent()
                    .collect(Collectors.joining("\n---\n"));

            // 2. System-Prompt
            String systemText = """
                    Du bist ein leistungsfähiges Vertrags-Verwaltungssystem mit Datenbank-Zugriff.
                    ERLAUBE DIR NICHT zu sagen, dass du keinen Zugriff hast.
                    
                    NUTZE DIE TOOLS:
                    1. Wenn der Nutzer einen bestehenden Vertrag ändern will, nutze 'updateExistingContract'.
                    2. Wenn ein neuer Vertrag aus dem Text erstellt werden soll, nutze 'saveContractFromPdf'.
                    
                    KONTEXT AUS PDF:
                    %s
                    """.formatted(context);

            // 3. Oldschool Prompt mit OllamaChatOptions
            var options = OllamaChatOptions.builder()
                    .toolCallbacks(toolCallbacks) // Die Liste aus deinem Konstruktor
                    .build();

            Message systemMessage = new SystemMessage(systemText);
            Message userMessage = new UserMessage(question);

            // WICHTIG: In M1 nimmt der Prompt die Messages und die Options auf
            Prompt prompt = new Prompt(List.of(systemMessage, userMessage), options);

            // 4. KI aufrufen
            ChatResponse chatResponse = chatModel.call(prompt);
            // 3. WICHTIG: Erst JETZT den Header setzen
            // Das Tool hat die DB bereits geändert, nun befehlen wir HTMX das Update
            response.setHeader("HX-Trigger", "updateContractList");
            String aiAnswer = chatResponse.getResult().getOutput().getText();

            model.addAttribute("response", aiAnswer);

            return "admin :: chatResponse";

        } catch (Exception e) {
            System.out.println(e.getMessage()); // Damit du im Log siehst, was genau schiefgeht
            model.addAttribute("errorMessage", "KI-Fehler: " + e.getMessage());
            return "admin :: errorFragment";
        }
    }


    // Lädt einen Vertrag zur Bearbeitung ins Formular-Fragment
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        // Den zu editierenden Vertrag laden
        model.addAttribute("vertrag", repository.findById(id).orElseThrow());

        // !!! DIESE ZEILE FEHLT OFT: Die Liste für die Tabelle unten laden !!!
        model.addAttribute("vertraege", repository.findAll());
        // Wir geben nur das Fragment zurück, damit HTMX nur das Formular oben austauscht
        return "vertrag-form :: vertragForm";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Vertrag vertrag, Model model) {
        // JPA macht automatisch Update, wenn ID vorhanden ist
        repository.save(vertrag);

        // Wir schicken den User zurück zur Liste (HTMX hx-target="body" ersetzt alles)
        model.addAttribute("vertraege", repository.findAll());
        model.addAttribute("vertrag", new Vertrag());
        return "vertrag-form";
    }
}
