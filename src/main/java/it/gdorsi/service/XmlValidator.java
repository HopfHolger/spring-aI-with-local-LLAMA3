package it.gdorsi.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import it.gdorsi.dao.XmlValidationResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class XmlValidator {

    public XmlValidationResult validate(File xmlFile) {
        return validate(xmlFile, null);
    }

    public XmlValidationResult validate(File xmlFile, File xsdFile) {
        if (!xmlFile.exists()) {
            return XmlValidationResult.fehler("XML-Datei nicht gefunden: " + xmlFile.getAbsolutePath());
        }

        try {
            String xmlContent = Files.readString(Path.of(xmlFile.toURI()));
            return validateContent(xmlContent, xsdFile);
        } catch (IOException e) {
            log.error("Fehler beim Lesen der XML-Datei: {}", e.getMessage(), e);
            return XmlValidationResult.fehler("Fehler beim Lesen der XML-Datei: " + e.getMessage());
        }
    }

    public XmlValidationResult validateContent(String xmlContent) {
        return validateContent(xmlContent, null);
    }

    public XmlValidationResult validateContent(String xmlContent, File xsdFile) {
        if (xmlContent == null || xmlContent.isBlank()) {
            return XmlValidationResult.fehler("XML-Inhalt darf nicht leer oder null sein.");
        }
        
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            db.parse(new org.xml.sax.InputSource(new java.io.StringReader(xmlContent)));

            if (xsdFile != null && xsdFile.exists()) {
                return validateAgainstXsd(xmlContent, xsdFile);
            }

            return XmlValidationResult.erfolgreich();
        } catch (SAXParseException e) {
            String fehler = String.format("XML Parse Fehler in Zeile %d, Spalte %d: %s",
                    e.getLineNumber(), e.getColumnNumber(), e.getMessage());
            log.warn("XML Validierungsfehler: {}", fehler);
            return XmlValidationResult.fehler(fehler);
        } catch (SAXException e) {
            String fehler = "XML Validierungsfehler: " + e.getMessage();
            log.warn("XML Validierungsfehler: {}", fehler);
            return XmlValidationResult.fehler(fehler);
        } catch (ParserConfigurationException e) {
            String fehler = "XML Parser Konfigurationsfehler: " + e.getMessage();
            log.error("Parser Fehler: {}", fehler, e);
            return XmlValidationResult.fehler(fehler);
        } catch (IOException e) {
            String fehler = "IO-Fehler beim Validieren: " + e.getMessage();
            log.error("IO Fehler: {}", fehler, e);
            return XmlValidationResult.fehler(fehler);
        }
    }

    private XmlValidationResult validateAgainstXsd(String xmlContent, File xsdFile) {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(xsdFile);
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new java.io.StringReader(xmlContent)));
            return XmlValidationResult.erfolgreich();
        } catch (SAXParseException e) {
            String fehler = String.format("XSD Validierungsfehler in Zeile %d, Spalte %d: %s",
                    e.getLineNumber(), e.getColumnNumber(), e.getMessage());
            log.warn("XSD Validierungsfehler: {}", fehler);
            return XmlValidationResult.fehler(fehler);
        } catch (SAXException e) {
            String fehler = "XSD Validierungsfehler: " + e.getMessage();
            log.warn("XSD Validierungsfehler: {}", fehler);
            return XmlValidationResult.fehler(fehler);
        } catch (IOException e) {
            String fehler = "IO-Fehler bei XSD Validierung: " + e.getMessage();
            log.error("IO Fehler: {}", fehler, e);
            return XmlValidationResult.fehler(fehler);
        }
    }
}
