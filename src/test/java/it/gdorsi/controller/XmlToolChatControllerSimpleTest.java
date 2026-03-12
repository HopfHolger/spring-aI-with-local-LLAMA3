package it.gdorsi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.ResponseEntity;

import it.gdorsi.service.XmlDokumentService;
import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class XmlToolChatControllerSimpleTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ChatMemory chatMemory;

    @Mock
    private XmlDokumentService xmlService;

    @Mock
    private HttpSession session;

    private XmlToolChatController controller;

    @BeforeEach
    void setUp() {
        // Mock die ChatClient.Builder Kette
        when(chatClientBuilder.defaultAdvisors(any(QuestionAnswerAdvisor.class), any(MessageChatMemoryAdvisor.class)))
                .thenReturn(chatClientBuilder);
        when(chatClientBuilder.defaultSystem(anyString())).thenReturn(chatClientBuilder);
        when(chatClientBuilder.defaultTools(xmlService)).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);

        controller = new XmlToolChatController(chatClientBuilder, vectorStore, chatMemory, xmlService);
    }

    @Test
    @DisplayName("Sollte korrekten System Prompt für ANALYSIS Kontext erstellen")
    void shouldCreateCorrectSystemPromptForAnalysisContext() {
        String prompt = controller.getSystemPromptForContext("ANALYSIS");
        
        assertNotNull(prompt);
        assertTrue(prompt.contains("Fokus: XML-Analyse"));
        assertTrue(prompt.contains("Nutze diese Tools: analyzeXml, searchSimilarXml"));
        assertTrue(prompt.contains("Gib strukturierte Analysen mit Metriken zurück"));
    }

    @Test
    @DisplayName("Sollte korrekten System Prompt für COMPARISON Kontext erstellen")
    void shouldCreateCorrectSystemPromptForComparisonContext() {
        String prompt = controller.getSystemPromptForContext("COMPARISON");
        
        assertNotNull(prompt);
        assertTrue(prompt.contains("Fokus: XML-Vergleich"));
        assertTrue(prompt.contains("Nutze diese Tools: compareXml, getXmlById"));
        assertTrue(prompt.contains("Vergleiche Dokumente und zeige Unterschiede"));
    }

    @Test
    @DisplayName("Sollte korrekten System Prompt für VALIDATION Kontext erstellen")
    void shouldCreateCorrectSystemPromptForValidationContext() {
        String prompt = controller.getSystemPromptForContext("VALIDATION");
        
        assertNotNull(prompt);
        assertTrue(prompt.contains("Fokus: XML-Validierung"));
        assertTrue(prompt.contains("Nutze diese Tools: validateXml"));
        assertTrue(prompt.contains("Prüfe Syntax und Struktur"));
    }

    @Test
    @DisplayName("Sollte korrekten System Prompt für EXTRACTION Kontext erstellen")
    void shouldCreateCorrectSystemPromptForExtractionContext() {
        String prompt = controller.getSystemPromptForContext("EXTRACTION");
        
        assertNotNull(prompt);
        assertTrue(prompt.contains("Fokus: XPath-Extraktion"));
        assertTrue(prompt.contains("Nutze diese Tools: extractXmlElements, getXmlById"));
        assertTrue(prompt.contains("Extrahiere spezifische Elemente"));
    }

    @Test
    @DisplayName("Sollte korrekten System Prompt für TRANSFORMATION Kontext erstellen")
    void shouldCreateCorrectSystemPromptForTransformationContext() {
        String prompt = controller.getSystemPromptForContext("TRANSFORMATION");
        
        assertNotNull(prompt);
        assertTrue(prompt.contains("Fokus: XML-Transformation"));
        assertTrue(prompt.contains("Nutze diese Tools: transformXml, getXmlById"));
        assertTrue(prompt.contains("Wende Transformationsregeln an"));
    }

    @Test
    @DisplayName("Sollte korrekten System Prompt für GENERAL Kontext erstellen")
    void shouldCreateCorrectSystemPromptForGeneralContext() {
        String prompt = controller.getSystemPromptForContext("GENERAL");
        
        assertNotNull(prompt);
        assertTrue(prompt.contains("Verfügbare Tools für alle XML-Operationen"));
        assertTrue(prompt.contains("Für Analyse: analyzeXml"));
        assertTrue(prompt.contains("Für Vergleich: compareXml"));
        assertTrue(prompt.contains("Für Validierung: validateXml"));
        assertTrue(prompt.contains("Für Extraktion: extractXmlElements"));
        assertTrue(prompt.contains("Für Transformation: transformXml"));
    }

    @Test
    @DisplayName("Sollte default GENERAL Kontext für unbekannte Kontext-Typen verwenden")
    void shouldUseDefaultGeneralContextForUnknownContextTypes() {
        String prompt = controller.getSystemPromptForContext("UNKNOWN");
        
        assertNotNull(prompt);
        assertTrue(prompt.contains("Verfügbare Tools für alle XML-Operationen"));
    }

    @Test
    @DisplayName("Sollte HTML-Response mit ANALYSIS Kontext korrekt formatieren")
    void shouldFormatHtmlResponseWithAnalysisContextCorrectly() {
        String response = "Test analysis response";
        ResponseEntity<String> formatted = controller.formatToolResponse(response, "ANALYSIS");
        
        assertNotNull(formatted);
        String html = formatted.getBody();
        assertTrue(html.contains("📊"));
        assertTrue(html.contains("ANALYSIS-Modus"));
        assertTrue(html.contains("Test analysis response"));
        assertTrue(html.contains("bg-blue-50"));
        assertTrue(html.contains("border-blue-200"));
    }

    @Test
    @DisplayName("Sollte HTML-Response mit COMPARISON Kontext korrekt formatieren")
    void shouldFormatHtmlResponseWithComparisonContextCorrectly() {
        String response = "Test comparison response";
        ResponseEntity<String> formatted = controller.formatToolResponse(response, "COMPARISON");
        
        assertNotNull(formatted);
        String html = formatted.getBody();
        assertTrue(html.contains("🔍"));
        assertTrue(html.contains("COMPARISON-Modus"));
        assertTrue(html.contains("Test comparison response"));
        assertTrue(html.contains("bg-purple-50"));
        assertTrue(html.contains("border-purple-200"));
    }

    @Test
    @DisplayName("Sollte HTML-Response mit VALIDATION Kontext korrekt formatieren")
    void shouldFormatHtmlResponseWithValidationContextCorrectly() {
        String response = "Test validation response";
        ResponseEntity<String> formatted = controller.formatToolResponse(response, "VALIDATION");
        
        assertNotNull(formatted);
        String html = formatted.getBody();
        assertTrue(html.contains("✅"));
        assertTrue(html.contains("VALIDATION-Modus"));
        assertTrue(html.contains("Test validation response"));
        assertTrue(html.contains("bg-green-50"));
        assertTrue(html.contains("border-green-200"));
    }

    @Test
    @DisplayName("Sollte HTML-Response mit EXTRACTION Kontext korrekt formatieren")
    void shouldFormatHtmlResponseWithExtractionContextCorrectly() {
        String response = "Test extraction response";
        ResponseEntity<String> formatted = controller.formatToolResponse(response, "EXTRACTION");
        
        assertNotNull(formatted);
        String html = formatted.getBody();
        assertTrue(html.contains("📋"));
        assertTrue(html.contains("EXTRACTION-Modus"));
        assertTrue(html.contains("Test extraction response"));
        assertTrue(html.contains("bg-yellow-50"));
        assertTrue(html.contains("border-yellow-200"));
    }

    @Test
    @DisplayName("Sollte HTML-Response mit TRANSFORMATION Kontext korrekt formatieren")
    void shouldFormatHtmlResponseWithTransformationContextCorrectly() {
        String response = "Test transformation response";
        ResponseEntity<String> formatted = controller.formatToolResponse(response, "TRANSFORMATION");
        
        assertNotNull(formatted);
        String html = formatted.getBody();
        assertTrue(html.contains("🔄"));
        assertTrue(html.contains("TRANSFORMATION-Modus"));
        assertTrue(html.contains("Test transformation response"));
        assertTrue(html.contains("bg-indigo-50"));
        assertTrue(html.contains("border-indigo-200"));
    }

    @Test
    @DisplayName("Sollte HTML-Response mit GENERAL Kontext korrekt formatieren")
    void shouldFormatHtmlResponseWithGeneralContextCorrectly() {
        String response = "Test general response";
        ResponseEntity<String> formatted = controller.formatToolResponse(response, "GENERAL");
        
        assertNotNull(formatted);
        String html = formatted.getBody();
        assertTrue(html.contains("🛠️"));
        assertTrue(html.contains("GENERAL-Modus"));
        assertTrue(html.contains("Test general response"));
        assertTrue(html.contains("bg-indigo-50"));
        assertTrue(html.contains("border-indigo-300"));
    }

    @Test
    @DisplayName("Sollte HTML-Response mit unbekanntem Kontext korrekt formatieren")
    void shouldFormatHtmlResponseWithUnknownContextCorrectly() {
        String response = "Test unknown response";
        ResponseEntity<String> formatted = controller.formatToolResponse(response, "UNKNOWN");
        
        assertNotNull(formatted);
        String html = formatted.getBody();
        assertTrue(html.contains("🛠️"));
        assertTrue(html.contains("UNKNOWN-Modus"));
        assertTrue(html.contains("Test unknown response"));
        assertTrue(html.contains("bg-indigo-50"));
        assertTrue(html.contains("border-indigo-300"));
    }
}