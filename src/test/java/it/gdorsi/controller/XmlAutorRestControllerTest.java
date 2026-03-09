package it.gdorsi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import it.gdorsi.repository.model.Autor;
import it.gdorsi.repository.model.XmlDokument;
import it.gdorsi.service.XmlDokumentService;

@ExtendWith(MockitoExtension.class)
class XmlAutorRestControllerTest {

    private MockMvc mockMvc;

    @Mock
    private XmlDokumentService xmlDokumentService;

    @InjectMocks
    private XmlAutorRestController xmlAutorRestController;

    private MockMultipartFile xmlFile;
    private Autor autor;
    private XmlDokument xmlDokument;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(xmlAutorRestController).build();
        
        xmlFile = new MockMultipartFile(
                "file",
                "test.xml",
                MediaType.APPLICATION_XML_VALUE,
                "<test>daten</test>".getBytes()
        );

        autor = new Autor();
        autor.setId(1L);
        autor.setName("Test Autor");

        xmlDokument = new XmlDokument();
        xmlDokument.setId(5L);
        xmlDokument.setDateiname("test.xml");
        xmlDokument.setInhalt("<test>daten</test>");
        xmlDokument.setAutor(autor);
    }

    @Nested
    @DisplayName("POST /api/autoren/{autorId}/xml")
    class CreateXmlTests {

        @Test
        @DisplayName("Erstellt XML erfolgreich")
        void createXml_success() throws Exception {
            when(xmlDokumentService.saveXml(eq(1L), any(), any())).thenReturn(xmlDokument);

            mockMvc.perform(multipart("/api/autoren/1/xml")
                            .file(xmlFile))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("GET /api/autoren/{autorId}/xml")
    class GetAllXmlTests {

        @Test
        @DisplayName("Gibt alle XMLs für Autor zurück")
        void getAllXml_success() throws Exception {
            when(xmlDokumentService.findByAutorId(1L)).thenReturn(List.of(xmlDokument));

            mockMvc.perform(get("/api/autoren/1/xml"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/autoren/{autorId}/xml/{xmlId}")
    class GetXmlByIdTests {

        @Test
        @DisplayName("Gibt XML nach ID zurück")
        void getXmlById_found() throws Exception {
            when(xmlDokumentService.findByIdAndAutorId(5L, 1L)).thenReturn(Optional.of(xmlDokument));

            mockMvc.perform(get("/api/autoren/1/xml/5"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Gibt 404 wenn nicht gefunden")
        void getXmlById_notFound() throws Exception {
            when(xmlDokumentService.findByIdAndAutorId(999L, 1L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/autoren/1/xml/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/autoren/{autorId}/xml/{xmlId}")
    class UpdateXmlTests {

        @Test
        @DisplayName("Aktualisiert XML erfolgreich")
        void updateXml_success() throws Exception {
            when(xmlDokumentService.updateXml(eq(1L), eq(5L), any(), any())).thenReturn(Optional.of(xmlDokument));

            mockMvc.perform(multipart("/api/autoren/1/xml/5")
                            .file(xmlFile)
                            .with(request -> {
                                request.setMethod("PUT");
                                return request;
                            }))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Gibt 404 bei nicht gefunden")
        void updateXml_notFound() throws Exception {
            when(xmlDokumentService.updateXml(eq(1L), eq(999L), any(), any()))
                    .thenReturn(Optional.empty());

            mockMvc.perform(multipart("/api/autoren/1/xml/999")
                            .file(xmlFile)
                            .with(request -> {
                                request.setMethod("PUT");
                                return request;
                            }))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("PUT ist idempotent - gleiche Anfrage gibt gleiche Antwort")
        void updateXml_idempotent() throws Exception {
            when(xmlDokumentService.updateXml(eq(1L), eq(5L), eq("test.xml"), eq("<test>daten</test>")))
                    .thenReturn(Optional.of(xmlDokument));

            mockMvc.perform(multipart("/api/autoren/1/xml/5")
                            .file(xmlFile)
                            .with(request -> {
                                request.setMethod("PUT");
                                return request;
                            }))
                    .andExpect(status().isOk());

            mockMvc.perform(multipart("/api/autoren/1/xml/5")
                            .file(xmlFile)
                            .with(request -> {
                                request.setMethod("PUT");
                                return request;
                            }))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/autoren/{autorId}/xml/{xmlId}")
    class DeleteXmlTests {

        @Test
        @DisplayName("Löscht XML erfolgreich")
        void deleteXml_success() throws Exception {
            when(xmlDokumentService.deleteXml(1L, 5L)).thenReturn(true);

            mockMvc.perform(delete("/api/autoren/1/xml/5"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Gibt 404 bei nicht gefunden")
        void deleteXml_notFound() throws Exception {
            when(xmlDokumentService.deleteXml(1L, 999L)).thenReturn(false);

            mockMvc.perform(delete("/api/autoren/1/xml/999"))
                    .andExpect(status().isNotFound());
        }
    }
}
