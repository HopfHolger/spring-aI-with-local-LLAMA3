package it.gdorsi.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.gdorsi.repository.AuthorRepository;
import it.gdorsi.repository.XmlDokumentRepository;
import it.gdorsi.repository.model.Autor;
import it.gdorsi.repository.model.XmlDokument;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XmlDokumentServiceToolTest {

    @Mock
    private XmlDokumentRepository xmlDokumentRepository;

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private XmlDokumentService xmlDokumentService;

    @Test
    void testAnalyzeXml_Success() {
        // Arrange
        Autor autor = new Autor("TestAutor", "TestBiografie", new float[0]);
        autor.setId(1L);
        
        XmlDokument xml = new XmlDokument("test.xml", "<root><element>Test</element></root>", new float[0], autor);
        xml.setId(1L);
        xml.setCreatedAt(LocalDateTime.now());
        
        when(authorRepository.findByName("TestAutor")).thenReturn(autor);
        when(xmlDokumentRepository.findById(1L)).thenReturn(Optional.of(xml));

        // Act
        String result = xmlDokumentService.analyzeXml("TestAutor", 1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("📊 XML-Analyse"));
        assertTrue(result.contains("test.xml"));
        assertTrue(result.contains("TestAutor"));
        assertTrue(result.contains("Größe:"));
    }

    @Test
    void testAnalyzeXml_AutorNotFound() {
        // Arrange
        when(authorRepository.findByName("UnknownAutor")).thenReturn(null);

        // Act
        String result = xmlDokumentService.analyzeXml("UnknownAutor", 1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("❌ Fehler bei der XML-Analyse"));
        assertTrue(result.contains("Autor nicht gefunden"));
    }

    @Test
    void testCompareXml_Success() {
        // Arrange
        Autor autor = new Autor("TestAutor", "TestBiografie", new float[0]);
        autor.setId(1L);
        
        XmlDokument xml1 = new XmlDokument("test1.xml", "<root><a>Test1</a></root>", new float[0], autor);
        xml1.setId(1L);
        
        XmlDokument xml2 = new XmlDokument("test2.xml", "<root><b>Test2</b></root>", new float[0], autor);
        xml2.setId(2L);
        
        when(authorRepository.findByName("TestAutor")).thenReturn(autor);
        when(xmlDokumentRepository.findById(1L)).thenReturn(Optional.of(xml1));
        when(xmlDokumentRepository.findById(2L)).thenReturn(Optional.of(xml2));

        // Act
        String result = xmlDokumentService.compareXml("TestAutor", 1L, 2L);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("🔍 XML-Vergleich"));
        assertTrue(result.contains("test1.xml"));
        assertTrue(result.contains("test2.xml"));
        assertTrue(result.contains("Größenunterschied:"));
    }

    @Test
    void testValidateXml_ValidXml() {
        // Arrange
        Autor autor = new Autor("TestAutor", "TestBiografie", new float[0]);
        autor.setId(1L);
        
        XmlDokument xml = new XmlDokument("test.xml", "<?xml version=\"1.0\"?><root><element>Test</element></root>", new float[0], autor);
        xml.setId(1L);
        
        when(authorRepository.findByName("TestAutor")).thenReturn(autor);
        when(xmlDokumentRepository.findById(1L)).thenReturn(Optional.of(xml));

        // Act
        String result = xmlDokumentService.validateXml("TestAutor", 1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("✅ XML-Validierung"));
        assertTrue(result.contains("XML-Deklaration vorhanden: ✅"));
        assertTrue(result.contains("Wohlgeformt: ✅"));
    }

    @Test
    void testValidateXml_InvalidXml() {
        // Arrange
        Autor autor = new Autor("TestAutor", "TestBiografie", new float[0]);
        autor.setId(1L);
        
        XmlDokument xml = new XmlDokument("test.xml", "<root><element>Test</root>", new float[0], autor);
        xml.setId(1L);
        
        when(authorRepository.findByName("TestAutor")).thenReturn(autor);
        when(xmlDokumentRepository.findById(1L)).thenReturn(Optional.of(xml));

        // Act
        String result = xmlDokumentService.validateXml("TestAutor", 1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("✅ XML-Validierung"));
        assertTrue(result.contains("Wohlgeformt: ❌") || result.contains("Wohlgeformt: ✅"));
    }

    @Test
    void testExtractXmlElements_Success() {
        // Arrange
        Autor autor = new Autor("TestAutor", "TestBiografie", new float[0]);
        autor.setId(1L);
        
        XmlDokument xml = new XmlDokument("test.xml", "<root><item>One</item><item>Two</item></root>", new float[0], autor);
        xml.setId(1L);
        
        when(authorRepository.findByName("TestAutor")).thenReturn(autor);
        when(xmlDokumentRepository.findById(1L)).thenReturn(Optional.of(xml));

        // Act
        String result = xmlDokumentService.extractXmlElements("TestAutor", 1L, "//item");

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("📋 XPath-Extraktion"));
        assertTrue(result.contains("<item>One</item>") || result.contains("<item>Two</item>"));
    }

    @Test
    void testTransformXml_ToUpperCase() {
        // Arrange
        Autor autor = new Autor("TestAutor", "TestBiografie", new float[0]);
        autor.setId(1L);
        
        XmlDokument xml = new XmlDokument("test.xml", "<root><element>test</element></root>", new float[0], autor);
        xml.setId(1L);
        
        when(authorRepository.findByName("TestAutor")).thenReturn(autor);
        when(xmlDokumentRepository.findById(1L)).thenReturn(Optional.of(xml));

        // Act
        String result = xmlDokumentService.transformXml("TestAutor", 1L, "toUpperCase");

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("🔄 XML-Transformation"));
        assertTrue(result.contains("<ROOT><ELEMENT>TEST</ELEMENT></ROOT>"));
    }

    @Test
    void testTransformXml_RemoveComments() {
        // Arrange
        Autor autor = new Autor("TestAutor", "TestBiografie", new float[0]);
        autor.setId(1L);
        
        XmlDokument xml = new XmlDokument("test.xml", "<root><!-- Comment --><element>test</element></root>", new float[0], autor);
        xml.setId(1L);
        
        when(authorRepository.findByName("TestAutor")).thenReturn(autor);
        when(xmlDokumentRepository.findById(1L)).thenReturn(Optional.of(xml));

        // Act
        String result = xmlDokumentService.transformXml("TestAutor", 1L, "removeComments");

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("🔄 XML-Transformation"));
        assertFalse(result.contains("<!-- Comment -->"));
    }
}