package it.gdorsi.service.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import it.gdorsi.dao.XmlValidationResult;
import it.gdorsi.service.validation.XmlValidator;

class XmlValidatorTest {

    private XmlValidator xmlValidator;

    @TempDir
    Path tempDir;

    private File gueltigeXml;
    private File ungewoehnendXml;
    private File xsdDatei;
    private File xmlXsdGueltig;
    private File xmlXsdUngueltig;
    private File nichtExistenteDatei;

    @BeforeEach
    void setUp() {
        xmlValidator = new XmlValidator();

        gueltigeXml = new File("src/test/resources/it/gdorsi/service/vertrag-gueltig.xml");
        ungewoehnendXml = new File("src/test/resources/it/gdorsi/service/vertrag-ungewoehnend.xml");
        xsdDatei = new File("src/test/resources/it/gdorsi/service/vertrag.xsd");
        xmlXsdGueltig = new File("src/test/resources/it/gdorsi/service/vertrag-xsd-gueltig.xml");
        xmlXsdUngueltig = new File("src/test/resources/it/gdorsi/service/vertrag-xsd-ungeueltig.xml");

        nichtExistenteDatei = tempDir.resolve("nicht-existiert.xml").toFile();
    }

    @Nested
    @DisplayName("validate(File) Tests")
    class ValidateFileTests {

        @Test
        @DisplayName("Gültige XML-Datei wird akzeptiert")
        void validate_file_gueltig() {
            XmlValidationResult ergebnis = xmlValidator.validate(gueltigeXml);

            assertTrue(ergebnis.gueltig());
            assertNull(ergebnis.fehlermeldung());
        }

        @Test
        @DisplayName("Nicht existierende Datei gibt Fehler zurück")
        void validate_file_nicht_existiert() {
            XmlValidationResult ergebnis = xmlValidator.validate(nichtExistenteDatei);

            assertFalse(ergebnis.gueltig());
            assertNotNull(ergebnis.fehlermeldung());
            assertTrue(ergebnis.fehlermeldung().contains("nicht gefunden"));
        }
    }

    @Nested
    @DisplayName("validate(File, File) Tests")
    class ValidateFileMitXsdTests {

        @Test
        @DisplayName("Gültige XML mit passender XSD wird akzeptiert")
        void validate_file_mit_xsd_gueltig() {
            XmlValidationResult ergebnis = xmlValidator.validate(xmlXsdGueltig, xsdDatei);

            assertTrue(ergebnis.gueltig());
            assertNull(ergebnis.fehlermeldung());
        }

        @Test
        @DisplayName("Ungültige XML gegen XSD wird abgelehnt")
        void validate_file_mit_xsd_ungeueltig() {
            XmlValidationResult ergebnis = xmlValidator.validate(xmlXsdUngueltig, xsdDatei);

            assertFalse(ergebnis.gueltig());
            assertNotNull(ergebnis.fehlermeldung());
        }

        @Test
        @DisplayName("Nicht existierende XSD-Datei wird ignoriert")
        void validate_file_mit_nicht_existenter_xsd() {
            File nichtExistenteXsd = tempDir.resolve("schema.xsd").toFile();

            XmlValidationResult ergebnis = xmlValidator.validate(gueltigeXml, nichtExistenteXsd);

            assertTrue(ergebnis.gueltig());
        }

        @Test
        @DisplayName("null XSD-Datei wird ignoriert")
        void validate_file_mit_null_xsd() {
            XmlValidationResult ergebnis = xmlValidator.validate(gueltigeXml, null);

            assertTrue(ergebnis.gueltig());
        }
    }

    @Nested
    @DisplayName("validateContent(String) Tests")
    class ValidateContentTests {

        @Test
        @DisplayName("Gültiger XML-String wird akzeptiert")
        void validate_content_gueltig() throws Exception {
            String xmlContent = Files.readString(Path.of(gueltigeXml.toURI()));

            XmlValidationResult ergebnis = xmlValidator.validateContent(xmlContent);

            assertTrue(ergebnis.gueltig());
            assertNull(ergebnis.fehlermeldung());
        }

        @Test
        @DisplayName("Ungültiger XML-String (nicht wohlgeformt) wird abgelehnt")
        void validate_content_ungewoehnend() throws Exception {
            String xmlContent = Files.readString(Path.of(ungewoehnendXml.toURI()));

            XmlValidationResult ergebnis = xmlValidator.validateContent(xmlContent);

            assertFalse(ergebnis.gueltig());
            assertNotNull(ergebnis.fehlermeldung());
            assertTrue(ergebnis.fehlermeldung().contains("XML Parse Fehler"));
        }

        @Test
        @DisplayName("Leerer String wird abgelehnt")
        void validate_content_leer() {
            XmlValidationResult ergebnis = xmlValidator.validateContent("");

            assertFalse(ergebnis.gueltig());
            assertNotNull(ergebnis.fehlermeldung());
        }

        @Test
        @DisplayName("Null-String wird abgelehnt")
        void validate_content_null() {
            XmlValidationResult ergebnis = xmlValidator.validateContent((String) null);

            assertFalse(ergebnis.gueltig());
            assertNotNull(ergebnis.fehlermeldung());
        }

        @Test
        @DisplayName("Ungültiges XML (falsches Tag) wird abgelehnt")
        void validate_content_falsches_xml() {
            XmlValidationResult ergebnis = xmlValidator.validateContent("<ungueltig><tag></unbekannt>");

            assertFalse(ergebnis.gueltig());
            assertNotNull(ergebnis.fehlermeldung());
        }
    }

    @Nested
    @DisplayName("validateContent(String, File) Tests")
    class ValidateContentMitXsdTests {

        @Test
        @DisplayName("Gültiger XML-String mit passender XSD wird akzeptiert")
        void validate_content_mit_xsd_gueltig() throws Exception {
            String xmlContent = Files.readString(Path.of(xmlXsdGueltig.toURI()));

            XmlValidationResult ergebnis = xmlValidator.validateContent(xmlContent, xsdDatei);

            assertTrue(ergebnis.gueltig());
            assertNull(ergebnis.fehlermeldung());
        }

        @Test
        @DisplayName("Ungültiger XML-String gegen XSD wird abgelehnt")
        void validate_content_mit_xsd_ungeueltig() throws Exception {
            String xmlContent = Files.readString(Path.of(xmlXsdUngueltig.toURI()));

            XmlValidationResult ergebnis = xmlValidator.validateContent(xmlContent, xsdDatei);

            assertFalse(ergebnis.gueltig());
            assertNotNull(ergebnis.fehlermeldung());
        }

        @Test
        @DisplayName("Gültiges XML mit fehlendem Pflichtfeld wird abgelehnt")
        void validate_content_mit_xsd_fehlendes_pflichtfeld() {
            String xmlContent = "<vertrag><kundeName>Nur Kunde</kundeName></vertrag>";

            XmlValidationResult ergebnis = xmlValidator.validateContent(xmlContent, xsdDatei);

            assertFalse(ergebnis.gueltig());
            assertNotNull(ergebnis.fehlermeldung());
        }

        @Test
        @DisplayName("Gültiges XML mit zusätzlichen Feldern wird akzeptiert")
        void validate_content_mit_xsd_zusaetzliche_felder() {
            String xmlContent = """
                <vertrag xmlns:extra="http://example.com/extra">
                    <vertragsNummer>V-001</vertragsNummer>
                    <kundeName>Test</kundeName>
                    <startDatum>2024-01-01</startDatum>
                    <endDatum>2024-12-31</endDatum>
                    <betrag>100.50</betrag>
                    <vertragsTyp>Wartung</vertragsTyp>
                    <status>AKTIV</status>
                    <bemerkung>Test</bemerkung>
                </vertrag>
                """;

            XmlValidationResult ergebnis = xmlValidator.validateContent(xmlContent, xsdDatei);

            assertTrue(ergebnis.gueltig());
        }

        @Test
        @DisplayName("XSD mit leerem Content wird abgelehnt")
        void validate_content_mit_xsd_leer() {
            XmlValidationResult ergebnis = xmlValidator.validateContent("", xsdDatei);

            assertFalse(ergebnis.gueltig());
            assertNotNull(ergebnis.fehlermeldung());
        }
    }

    @Nested
    @DisplayName("XmlValidationResult Tests")
    class XmlValidationResultTests {

        @Test
        @DisplayName("erfolgreich() erstellt gültiges Ergebnis")
        void testErfolgreich() {
            XmlValidationResult ergebnis = XmlValidationResult.erfolgreich();

            assertTrue(ergebnis.gueltig());
            assertNull(ergebnis.fehlermeldung());
        }

        @Test
        @DisplayName("fehler() erstellt ungültiges Ergebnis")
        void testFehler() {
            XmlValidationResult ergebnis = XmlValidationResult.fehler("Testfehler");

            assertFalse(ergebnis.gueltig());
            assertEquals("Testfehler", ergebnis.fehlermeldung());
        }
    }
}
