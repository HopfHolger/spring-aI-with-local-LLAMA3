package it.gdorsi.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import it.gdorsi.service.AutorOperations;
import jakarta.servlet.http.HttpSession;

/**
 * Es gibt zwei Wege, wie du dein PDF-Wissen (Docs) an die KI übergibst:
 * 1. Den "Manuellen" Weg
 * Schritt A: Du suchst händisch in der DB (vectorStore.similaritySearch).
 * Schritt B: Du baust dir einen String aus den Texten zusammen.
 * Schritt C: Du fügst diesen String manuell in deinen Prompt ein.
 * Vorteil: Du hast volle Kontrolle über jedes Wort im Prompt.
 * <p>
 * // 1. Suche die relevantesten Schnipsel aus deinem Wissen (Postgres)
 * List<Document> similarDocs = vectorStore.similaritySearch(
 * SearchRequest.query(message).withTopK(3));
 * <p>
 * // 2. Erstelle den Kontext-String aus den Fundstücken
 * String context = similarDocs.stream()
 * .map(Document::getContent)
 * .collect(Collectors.joining("\n"));
 * <p>
 * // 3. Baue den Prompt: Fakten + Frage
 * String prompt = """
 * Nutze den folgenden Kontext, um die Frage zu beantworten.
 * Wenn du die Antwort nicht im Kontext findest, sage: "Das weiß ich leider nicht."
 * <p>
 * KONTEXT:
 * %s
 * <p>
 * FRAGE:
 * %s
 * """.formatted(context, message);
 * <p>
 * 2. Der "Automatische" Weg (Mit dem QuestionAnswerAdvisor)
 * ist der neue Weg, den die aktuelle Spring AI Version (M5) ermöglicht.
 * Du sagst dem ChatClient einfach: "Hier ist mein vectorStore, kümmere dich um den Rest."
 * Wenn du jetzt chatClient.prompt().user("Frage").call() aufrufst, macht der Advisor im Hintergrund genau die Schritte A, B und C für dich,
 * ohne dass du den Code dafür schreiben musst.
 */

@Controller
public class ChatController {

    private final ChatClient chatClient;

    private final AutorOperations autorOperations;

    /**
     * Der ChatClient wird im Konstruktor (oder einer @Bean-Methode) über den
     * builder mit dem defaultAdvisor initialisierst, es wird jeder Aufruf
     * dieses Clients automatisch die RAG-Logik (VectorStore-Abfrage) ausführen.
     * <p>
     * Falls du mal einen Call ohne Dokumenten-Kontext machen möchtest, kannst du einfach:
     * java
     * chatClient.prompt()
     * .advisors(a -> a.clear()) // Entfernt die Default-Advisoren für diesen einen Call
     * .user("Hallo, wer bist du?")
     * .call();
     *
     * @param builder     chatClient Builder
     * @param vectorStore vectorStore Postgres
     */
    public ChatController(ChatClient.Builder builder, VectorStore vectorStore, ChatMemory chatMemory, AutorOperations autorOperations) {
        this.autorOperations = autorOperations;
        // Immutability: Der Advisor ist nach dem .build() unveränderlich,
        // was ihn Thread-sicher für den ChatClient macht.
        // Wenn der RAG-Advisor (QuestionAnswerAdvisor) aktiv ist, schreibt er den Prompt massiv um, um die Dokumente aus der Vector-Datenbank einzufügen.
        QuestionAnswerAdvisor advisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .topK(2) // 2 ähnlichsten Textpassagen zurück default 4
                        .similarityThreshold(0.4) // Treffer zulassen, aber keinen Müll
                        .build())
                .build();
        MessageChatMemoryAdvisor chatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        this.chatClient = builder
                .defaultAdvisors(advisor, chatMemoryAdvisor)
                //.defaultTools(autorOperations)
                .build();
    }

    /**
     *
     * Bei zustandslosen APIs (z. B. REST für Mobile Apps)
     * sendet der Client oft selbst eine ID mit (z. B. eine UUID),
     * um einen Chat-Thread zu identifizieren. So bleibst du unabhängig von Server-Sessions.
     *
     * @param question usewr prompt/question
     * @param session  HttpSession
     * @return Antwort vom ChatClient
     * @PostMapping("/ask") public String ask(@RequestBody ChatRequest request) {
     * // Der Client schickt z.B. eine chatId: "user-123-thread-456"
     * String conversationId = request.getChatId();
     * ...
     * }
     */
    @PostMapping("/admin/chat")
    public String chat(@RequestParam("question") String question,
                       @RequestParam(value = "lang", defaultValue = "Deutsch") String lang, // Default-Wert schützt vor Fehlern
                       HttpSession session,
                       Model model) {

        // Wir setzen die Sprache als absolut höchste Priorität
        // Wir nutzen "Respond only in [Language]" als finales Kommando
        String instruction = String.format(" [MANDATORY: Use ONLY the language '%s' for your response, even if the context is German.]", lang);
        try {
            String answer = chatClient.prompt()
                    .system("Du bist ein Assistent. Nutze die Fakten aus der Datenbank.")
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, session.getId()))
                    .user(question + instruction)
                    .call()
                    .content();

            model.addAttribute("response", answer);
            return "admin :: chatResponse"; // Jetzt rendert Thymeleaf das Fragment
        } catch (Exception e) {
            model.addAttribute("errorMessage", "KI-Fehler: " + e.getMessage());
            return "admin :: errorFragment";
        }
    }

    @PostMapping("/admin/autor/chat")
    public String chatAutor(@RequestParam("question") String question,
                            HttpSession session,
                            Model model) {

        try {
            String answer = chatClient.prompt()
                    // Im /admin/autor/chat Endpunkt:
                    .system("Speichert einen neuen Autor in der Datenbank")
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, session.getId()))
                    .tools(autorOperations)
                    .user(question)
                    .call()
                    .content();

            model.addAttribute("response", "✅ KI-Aktion ausgeführt: " + answer);
            return "admin :: chatResponse";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Fehler: " + e.getMessage());
            return "admin :: errorFragment";
        }
    }

    @PostMapping("/admin/chat/clear")
    public String clearChat(HttpSession session, Model model) {
        // Option A: Komplette Session killen (radikal, aber sicher)
        session.invalidate();

        // Option B: Nur die Nachrichten im ChatMemory löschen (falls du eine Map nutzt)
        // chatMemory.clear(session.getId());

        model.addAttribute("response", "Gedächtnis gelöscht. Worüber wollen wir sprechen?");
        return "admin :: chatResponse";
    }

}
