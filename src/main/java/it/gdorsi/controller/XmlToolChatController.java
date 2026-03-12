package it.gdorsi.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import it.gdorsi.service.XmlDokumentService;
import jakarta.servlet.http.HttpSession;

@Controller
public class XmlToolChatController {

    private final ChatClient chatClient;

    public XmlToolChatController(ChatClient.Builder builder, 
                                VectorStore vectorStore, 
                                ChatMemory chatMemory,
                                XmlDokumentService xmlService) {

        QuestionAnswerAdvisor ragAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
            .searchRequest(SearchRequest.builder()
                .topK(3)
                .similarityThreshold(0.4)
                .build())
            .build();
            
        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        
        this.chatClient = builder
            .defaultAdvisors(ragAdvisor, memoryAdvisor)
            .defaultSystem("""
                Du bist ein XML-Experte mit Zugriff auf XML-Tools.
                Nutze die Tools für spezifische XML-Operationen:
                1. Für Analyse: analyzeXml
                2. Für Vergleich: compareXml  
                3. Für Validierung: validateXml
                4. Für Extraktion: extractXmlElements
                5. Für Transformation: transformXml
                
                Antworte auf Deutsch und sei präzise.
                """)
            .defaultTools(xmlService)
            .build();
    }
    
    @PostMapping("/admin/xml/tool-chat")
    public ResponseEntity<String> xmlToolChat(
            @RequestParam("question") String question,
            @RequestParam(value = "contextType", defaultValue = "GENERAL") String contextType,
            HttpSession session) {
        
        String systemPrompt = getSystemPromptForContext(contextType);
        
        String response = chatClient.prompt()
            .system(systemPrompt)
            .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, session.getId()))
            .user(question)
            .call()
            .content();
            
        return formatToolResponse(response, contextType);
    }

    String getSystemPromptForContext(String contextType) {
        String basePrompt = """
            Du bist ein XML-Experte mit Zugriff auf XML-Tools.
            Antworte auf Deutsch und sei präzise.
            """;
            
        return switch (contextType) {
            case "ANALYSIS" -> basePrompt + """
                
                Fokus: XML-Analyse
                Nutze diese Tools: analyzeXml, searchSimilarXml
                Gib strukturierte Analysen mit Metriken zurück.
                """;
            case "COMPARISON" -> basePrompt + """
                
                Fokus: XML-Vergleich
                Nutze diese Tools: compareXml, getXmlById
                Vergleiche Dokumente und zeige Unterschiede.
                """;
            case "VALIDATION" -> basePrompt + """
                
                Fokus: XML-Validierung
                Nutze diese Tools: validateXml
                Prüfe Syntax und Struktur.
                """;
            case "EXTRACTION" -> basePrompt + """
                
                Fokus: XPath-Extraktion
                Nutze diese Tools: extractXmlElements, getXmlById
                Extrahiere spezifische Elemente.
                """;
            case "TRANSFORMATION" -> basePrompt + """
                
                Fokus: XML-Transformation
                Nutze diese Tools: transformXml, getXmlById
                Wende Transformationsregeln an.
                """;
            default -> basePrompt + """
                
                Verfügbare Tools für alle XML-Operationen:
                1. Für Analyse: analyzeXml
                2. Für Vergleich: compareXml  
                3. Für Validierung: validateXml
                4. Für Extraktion: extractXmlElements
                5. Für Transformation: transformXml
                6. Für Suche: searchSimilarXml, getXmlById, getXmlByAutor
                """;
        };
    }

    ResponseEntity<String> formatToolResponse(String response, String contextType) {
        String icon = switch (contextType) {
            case "ANALYSIS" -> "📊";
            case "COMPARISON" -> "🔍";
            case "VALIDATION" -> "✅";
            case "EXTRACTION" -> "📋";
            case "TRANSFORMATION" -> "🔄";
            default -> "🛠️";
        };
        
        String colorClass = switch (contextType) {
            case "ANALYSIS" -> "bg-blue-50 border-blue-200";
            case "COMPARISON" -> "bg-purple-50 border-purple-200";
            case "VALIDATION" -> "bg-green-50 border-green-200";
            case "EXTRACTION" -> "bg-yellow-50 border-yellow-200";
            case "TRANSFORMATION" -> "bg-indigo-50 border-indigo-200";
            default -> "bg-indigo-50 border-indigo-300";
        };
        
        String html = String.format("""
            <div class="p-4 rounded-lg border-l-4 border-indigo-500 %s">
                <div class="flex items-center gap-2 mb-2">
                    <span class="text-lg">%s</span>
                    <span class="text-sm font-medium text-gray-600">%s-Modus</span>
                </div>
                <div class="text-gray-800">%s</div>
            </div>
            """, colorClass, icon, contextType, response);
            
        return ResponseEntity.ok(html);
    }
}