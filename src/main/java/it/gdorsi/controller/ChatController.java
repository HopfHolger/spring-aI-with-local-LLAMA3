package it.gdorsi.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import reactor.core.publisher.Flux;

/**
 * Es gibt zwei Wege, wie du dein PDF-Wissen (Docs) an die KI übergibst:
 * 1. Den "Manuellen" Weg
 * Schritt A: Du suchst händisch in der DB (vectorStore.similaritySearch).
 * Schritt B: Du baust dir einen String aus den Texten zusammen.
 * Schritt C: Du fügst diesen String manuell in deinen Prompt ein.
 * Vorteil: Du hast volle Kontrolle über jedes Wort im Prompt.
 * <p>
 *         // 1. Suche die relevantesten Schnipsel aus deinem Wissen (Postgres)
 *         List<Document> similarDocs = vectorStore.similaritySearch(
 *                 SearchRequest.query(message).withTopK(3));
 * <p>
 *         // 2. Erstelle den Kontext-String aus den Fundstücken
 *         String context = similarDocs.stream()
 *                 .map(Document::getContent)
 *                 .collect(Collectors.joining("\n"));
 * <p>
 *         // 3. Baue den Prompt: Fakten + Frage
 *         String prompt = """
 *             Nutze den folgenden Kontext, um die Frage zu beantworten.
 *             Wenn du die Antwort nicht im Kontext findest, sage: "Das weiß ich leider nicht."
 * <p>
 *             KONTEXT:
 *             %s
 * <p>
 *             FRAGE:
 *             %s
 *             """.formatted(context, message);
 * <p>
 * 2. Der "Automatische" Weg (Mit dem QuestionAnswerAdvisor)
 *  ist der neue Weg, den die aktuelle Spring AI Version (M5) ermöglicht.
 * Du sagst dem ChatClient einfach: "Hier ist mein vectorStore, kümmere dich um den Rest."
 * Wenn du jetzt chatClient.prompt().user("Frage").call() aufrufst, macht der Advisor im Hintergrund genau die Schritte A, B und C für dich,
 * ohne dass du den Code dafür schreiben musst.
 */

@Controller
public class ChatController {

    // Hier kannst du dein eigenes Chat-Modell/Client einbinden (z.B. Mistral)
    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder, VectorStore vectorStore) {
        // Wir "registrieren" das Wissen einmalig im Client
        this.chatClient = builder
                .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))
                .build();
    }

    @PostMapping("/admin/chat")
    @ResponseBody
    public String chat(@RequestParam("question") String question) {
        try {
            // Der Advisor übernimmt: Vektorsuche in PG + Kontext-Prompt + Ollama Call
            return chatClient.prompt()
                    .user(question)
                    .call()
                    .content();
        } catch (Exception e) {
            return "❌ Fehler bei der KI-Anfrage: " + e.getMessage();
        }
    }


    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestParam String question) {
        return chatClient.prompt()
                .user(question)
                .stream()
                .content(); // Liefert einen Flux<String>, der Wort für Wort im Frontend ankommt
    }

}
