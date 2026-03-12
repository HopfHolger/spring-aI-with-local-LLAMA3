package it.gdorsi.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import config.TestConfig;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import it.gdorsi.repository.AuthorRepository;
import it.gdorsi.repository.XmlDokumentRepository;
import it.gdorsi.repository.model.Autor;
import it.gdorsi.repository.model.XmlDokument;

/**
 * E2E-Test für XmlToolChatController mit RestAssured.
 * Testet die komplette Tool-Chat API mit realem Spring Boot Server.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestConfig.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class XmlToolChatControllerE2EIT {

    @LocalServerPort
    private int port;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private XmlDokumentRepository xmlDokumentRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "";

        // Test-Autor erstellen
        String autorName = "E2E Tool Test Autor";
        Autor testAutor = new Autor(autorName, "E2E Test Biografie für Tool-Chat", new float[1024]);
        testAutor = authorRepository.save(testAutor);
        Long autorId = testAutor.getId();

        // Test-XML-Dokumente erstellen
        String xmlContent1 = """
            <?xml version="1.0" encoding="UTF-8"?>
            <root>
                <element id="1">
                    <name>Test Element 1</name>
                    <value>100</value>
                </element>
                <element id="2">
                    <name>Test Element 2</name>
                    <value>200</value>
                </element>
            </root>
            """;

        String xmlContent2 = """
            <?xml version="1.0" encoding="UTF-8"?>
            <data>
                <item>One</item>
                <item>Two</item>
                <item>Three</item>
            </data>
            """;

        XmlDokument xml1 = new XmlDokument("test1.xml", xmlContent1, new float[1024], testAutor);
        XmlDokument xml2 = new XmlDokument("test2.xml", xmlContent2, new float[1024], testAutor);

        xml1 = xmlDokumentRepository.save(xml1);
        xml2 = xmlDokumentRepository.save(xml2);
    }

    @AfterEach
    void tearDown() {
        // Cleanup mit Exception-Handling für den Fall, dass Tabellen bereits gelöscht wurden
        try {
            xmlDokumentRepository.deleteAll();
        } catch (Exception e) {
            // Ignorieren, wenn Tabelle nicht existiert (z.B. bei create-drop)
        }
        
        try {
            authorRepository.deleteAll();
        } catch (Exception e) {
            // Ignorieren, wenn Tabelle nicht existiert
        }
    }

    @Test
    @DisplayName("POST /admin/xml/tool-chat - Sollte mit GENERAL Kontext erfolgreich antworten")
    void shouldRespondSuccessfullyWithGeneralContext() {
        given()
            .contentType(ContentType.URLENC)
            .formParam("question", "Was kannst du mit XML-Dokumenten machen?")
            .formParam("contextType", "GENERAL")
        .when()
            .post("/admin/xml/tool-chat")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body(containsString("GENERAL-Modus"))
            .body(containsString("🛠️"))
            .body(containsString("<div class="))
            .body(containsString("border-indigo-500"));
    }

    @Test
    @DisplayName("POST /admin/xml/tool-chat - Sollte ANALYSIS Kontext mit XML-Analyse unterstützen")
    void shouldSupportAnalysisContextWithXmlAnalysis() {
        given()
            .contentType(ContentType.URLENC)
            .formParam("question", "Wie analysiere ich XML-Dokumente?")
            .formParam("contextType", "ANALYSIS")
        .when()
            .post("/admin/xml/tool-chat")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body(containsString("ANALYSIS-Modus"))
            .body(containsString("📊"))
            .body(containsString("bg-blue-50"))
            .body(containsString("border-blue-200"));
    }

    @Test
    @DisplayName("POST /admin/xml/tool-chat - Sollte COMPARISON Kontext mit XML-Vergleich unterstützen")
    void shouldSupportComparisonContextWithXmlComparison() {
        given()
            .contentType(ContentType.URLENC)
            .formParam("question", "Wie vergleiche ich XML-Dokumente?")
            .formParam("contextType", "COMPARISON")
        .when()
            .post("/admin/xml/tool-chat")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body(containsString("COMPARISON-Modus"))
            .body(containsString("🔍"))
            .body(containsString("bg-purple-50"))
            .body(containsString("border-purple-200"));
    }

    @Test
    @DisplayName("POST /admin/xml/tool-chat - Sollte VALIDATION Kontext mit XML-Validierung unterstützen")
    void shouldSupportValidationContextWithXmlValidation() {
        given()
            .contentType(ContentType.URLENC)
            .formParam("question", "Wie validiere ich XML?")
            .formParam("contextType", "VALIDATION")
        .when()
            .post("/admin/xml/tool-chat")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body(containsString("VALIDATION-Modus"))
            .body(containsString("✅"))
            .body(containsString("bg-green-50"))
            .body(containsString("border-green-200"));
    }

    @Test
    @DisplayName("POST /admin/xml/tool-chat - Sollte EXTRACTION Kontext mit XPath-Extraktion unterstützen")
    void shouldSupportExtractionContextWithXPathExtraction() {
        given()
            .contentType(ContentType.URLENC)
            .formParam("question", "Was ist XPath-Extraktion?")
            .formParam("contextType", "EXTRACTION")
        .when()
            .post("/admin/xml/tool-chat")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body(containsString("EXTRACTION-Modus"))
            .body(containsString("📋"))
            .body(containsString("bg-yellow-50"))
            .body(containsString("border-yellow-200"));
    }

    @Test
    @DisplayName("POST /admin/xml/tool-chat - Sollte TRANSFORMATION Kontext mit XML-Transformation unterstützen")
    void shouldSupportTransformationContextWithXmlTransformation() {
        given()
            .contentType(ContentType.URLENC)
            .formParam("question", "Wie kann ich XML transformieren?")
            .formParam("contextType", "TRANSFORMATION")
        .when()
            .post("/admin/xml/tool-chat")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body(containsString("TRANSFORMATION-Modus"))
            .body(containsString("🔄"))
            .body(containsString("bg-indigo-50"))
            .body(containsString("border-indigo-200"));
    }

    @Test
    @DisplayName("POST /admin/xml/tool-chat - Sollte default GENERAL Kontext verwenden wenn nicht angegeben")
    void shouldUseDefaultGeneralContextWhenNotProvided() {
        given()
            .contentType(ContentType.URLENC)
            .formParam("question", "Wie funktioniert der XML Tool-Chat?")
        .when()
            .post("/admin/xml/tool-chat")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body(containsString("GENERAL-Modus"))
            .body(containsString("🛠️"));
    }

    @Test
    @DisplayName("POST /admin/xml/tool-chat - Sollte 400 zurückgeben wenn question Parameter fehlt")
    void shouldReturn400WhenQuestionParameterMissing() {
        given()
            .contentType(ContentType.URLENC)
            .formParam("contextType", "GENERAL")
        .when()
            .post("/admin/xml/tool-chat")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("POST /admin/xml/tool-chat - Sollte mit unbekanntem Kontext-Typ umgehen können")
    void shouldHandleUnknownContextType() {
        given()
            .contentType(ContentType.URLENC)
            .formParam("question", "Test mit unbekanntem Kontext")
            .formParam("contextType", "UNKNOWN_CONTEXT")
        .when()
            .post("/admin/xml/tool-chat")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body(containsString("UNKNOWN_CONTEXT-Modus"))
            .body(containsString("🛠️"))
            .body(containsString("bg-indigo-50"))
            .body(containsString("border-indigo-300"));
    }

    @Test
    @DisplayName("POST /admin/xml/tool-chat - Sollte Session-Handling unterstützen")
    void shouldSupportSessionHandling() {
        // Erste Anfrage mit Session
        String sessionCookie = given()
            .contentType(ContentType.URLENC)
            .formParam("question", "Erste Frage in der Session")
            .formParam("contextType", "GENERAL")
        .when()
            .post("/admin/xml/tool-chat")
        .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .cookie("JSESSIONID");

        // Zweite Anfrage mit gleicher Session (Chat Memory sollte funktionieren)
        given()
            .contentType(ContentType.URLENC)
            .cookie("JSESSIONID", sessionCookie)
            .formParam("question", "Zweite Frage in der gleichen Session")
            .formParam("contextType", "GENERAL")
        .when()
            .post("/admin/xml/tool-chat")
        .then()
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("POST /admin/xml/tool-chat - Sollte korrekte HTML-Struktur zurückgeben")
    void shouldReturnCorrectHtmlStructure() {
        given()
            .contentType(ContentType.URLENC)
            .formParam("question", "Test HTML-Struktur")
            .formParam("contextType", "ANALYSIS")
        .when()
            .post("/admin/xml/tool-chat")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body(containsString("<div"))
            .body(containsString("class=\""))
            .body(containsString("p-4"))
            .body(containsString("rounded-lg"))
            .body(containsString("border-l-4"))
            .body(containsString("mb-2"));
    }

    @Test
    @DisplayName("POST /admin/xml/tool-chat - Sollte Tool-spezifische Antworten für verschiedene Kontexte haben")
    void shouldHaveToolSpecificResponsesForDifferentContexts() {
        // Test ANALYSIS Kontext
        given()
            .contentType(ContentType.URLENC)
            .formParam("question", "Was kann ich mit XML-Analyse machen?")
            .formParam("contextType", "ANALYSIS")
        .when()
            .post("/admin/xml/tool-chat")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body(containsString("ANALYSIS-Modus"))
            .body(containsString("📊"));

        // Test VALIDATION Kontext  
        given()
            .contentType(ContentType.URLENC)
            .formParam("question", "Wie validiere ich XML?")
            .formParam("contextType", "VALIDATION")
        .when()
            .post("/admin/xml/tool-chat")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body(containsString("VALIDATION-Modus"))
            .body(containsString("✅"));

        // Test EXTRACTION Kontext
        given()
            .contentType(ContentType.URLENC)
            .formParam("question", "Was ist XPath-Extraktion?")
            .formParam("contextType", "EXTRACTION")
        .when()
            .post("/admin/xml/tool-chat")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body(containsString("EXTRACTION-Modus"))
            .body(containsString("📋"));
    }
}