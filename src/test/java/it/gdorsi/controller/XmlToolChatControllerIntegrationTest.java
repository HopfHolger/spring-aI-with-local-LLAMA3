package it.gdorsi.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

@SpringBootTest
class XmlToolChatControllerIntegrationTest {

    @Autowired
    private XmlToolChatController controller;

    @Test
    @DisplayName("Sollte Controller erfolgreich initialisieren")
    void shouldInitializeControllerSuccessfully() {
        assertNotNull(controller);
    }

    @Test
    @DisplayName("Sollte korrekten System Prompt für alle Kontext-Typen erstellen")
    void shouldCreateCorrectSystemPromptForAllContextTypes() {
        String[] contextTypes = {"ANALYSIS", "COMPARISON", "VALIDATION", "EXTRACTION", "TRANSFORMATION", "GENERAL"};
        String[] expectedKeywords = {
            "Fokus: XML-Analyse",
            "Fokus: XML-Vergleich", 
            "Fokus: XML-Validierung",
            "Fokus: XPath-Extraktion",
            "Fokus: XML-Transformation",
            "Verfügbare Tools für alle XML-Operationen"
        };

        for (int i = 0; i < contextTypes.length; i++) {
            String prompt = controller.getSystemPromptForContext(contextTypes[i]);
            assertNotNull(prompt);
            assertTrue(prompt.contains(expectedKeywords[i]));
            assertTrue(prompt.contains("Du bist ein XML-Experte"));
            assertTrue(prompt.contains("Antworte auf Deutsch"));
        }
    }

    @Test
    @DisplayName("Sollte HTML-Response für alle Kontext-Typen korrekt formatieren")
    void shouldFormatHtmlResponseForAllContextTypesCorrectly() {
        String[] contextTypes = {"ANALYSIS", "COMPARISON", "VALIDATION", "EXTRACTION", "TRANSFORMATION", "GENERAL"};
        String[] expectedIcons = {"📊", "🔍", "✅", "📋", "🔄", "🛠️"};
        String[] expectedColors = {
            "bg-blue-50 border-blue-200",
            "bg-purple-50 border-purple-200", 
            "bg-green-50 border-green-200",
            "bg-yellow-50 border-yellow-200",
            "bg-indigo-50 border-indigo-200",
            "bg-indigo-50 border-indigo-300"
        };

        String testResponse = "Test response content";

        for (int i = 0; i < contextTypes.length; i++) {
            ResponseEntity<String> formatted = controller.formatToolResponse(testResponse, contextTypes[i]);
            
            assertNotNull(formatted);
            String html = formatted.getBody();
            assertNotNull(html);
            
            assertTrue(html.contains(expectedIcons[i]));
            assertTrue(html.contains(contextTypes[i] + "-Modus"));
            assertTrue(html.contains(testResponse));
            assertTrue(html.contains(expectedColors[i]));
            assertTrue(html.contains("<div class=\""));
            assertTrue(html.contains("p-4"));
            assertTrue(html.contains("rounded-lg"));
            assertTrue(html.contains("border-l-4"));
        }
    }

    @Test
    @DisplayName("Sollte mit unbekanntem Kontext-Typ umgehen können")
    void shouldHandleUnknownContextType() {
        String prompt = controller.getSystemPromptForContext("UNKNOWN_CONTEXT");
        assertNotNull(prompt);
        assertTrue(prompt.contains("Verfügbare Tools für alle XML-Operationen"));

        ResponseEntity<String> formatted = controller.formatToolResponse("Test", "UNKNOWN_CONTEXT");
        assertNotNull(formatted);
        String html = formatted.getBody();
        assertTrue(html.contains("UNKNOWN_CONTEXT-Modus"));
        assertTrue(html.contains("🛠️"));
        assertTrue(html.contains("bg-indigo-50 border-indigo-300"));
    }

    @Test
    @DisplayName("Sollte System Prompt mit Tool-Liste für GENERAL Kontext enthalten")
    void shouldIncludeToolListInGeneralContextPrompt() {
        String prompt = controller.getSystemPromptForContext("GENERAL");
        
        assertTrue(prompt.contains("Für Analyse: analyzeXml"));
        assertTrue(prompt.contains("Für Vergleich: compareXml"));
        assertTrue(prompt.contains("Für Validierung: validateXml"));
        assertTrue(prompt.contains("Für Extraktion: extractXmlElements"));
        assertTrue(prompt.contains("Für Transformation: transformXml"));
        assertTrue(prompt.contains("Für Suche: searchSimilarXml, getXmlById, getXmlByAutor"));
    }
}