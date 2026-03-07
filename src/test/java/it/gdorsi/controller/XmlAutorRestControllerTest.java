package it.gdorsi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
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

import it.gdorsi.service.XmlAutorService;

@ExtendWith(MockitoExtension.class)
class XmlAutorRestControllerTest {

    private MockMvc mockMvc;

    @Mock
    private XmlAutorService xmlAutorService;

    @InjectMocks
    private XmlAutorRestController xmlAutorRestController;

    private MockMultipartFile xmlFile;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(xmlAutorRestController).build();
        
        xmlFile = new MockMultipartFile(
                "file",
                "test.xml",
                MediaType.APPLICATION_XML_VALUE,
                "<test>daten</test>".getBytes()
        );
    }

    @Nested
    @DisplayName("POST /api/autoren/{autorId}/xml")
    class CreateXmlTests {

        @Test
        @DisplayName("Erstellt XML erfolgreich")
        void createXml_success() throws Exception {
            File mockFile = new File("/tmp/test.xml");
            when(xmlAutorService.createXmlFuerAutor(anyLong(), any(File.class))).thenReturn(mockFile);

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
            when(xmlAutorService.getAllXmlForAutor(1L)).thenReturn(List.of());

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
            File mockFile = new File("/tmp/test.xml");
            when(xmlAutorService.getXmlByAutorId(1L)).thenReturn(Optional.of(mockFile));

            mockMvc.perform(get("/api/autoren/1/xml/5"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Gibt 404 wenn nicht gefunden")
        void getXmlById_notFound() throws Exception {
            when(xmlAutorService.getXmlByAutorId(1L)).thenReturn(Optional.empty());

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
            File mockFile = new File("/tmp/test.xml");
            when(xmlAutorService.updateXmlForAutor(anyLong(), any(File.class)))
                    .thenReturn(Optional.of(mockFile));

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
            when(xmlAutorService.updateXmlForAutor(anyLong(), any(File.class)))
                    .thenReturn(Optional.empty());

            mockMvc.perform(multipart("/api/autoren/1/xml/999")
                            .file(xmlFile)
                            .with(request -> {
                                request.setMethod("PUT");
                                return request;
                            }))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/autoren/{autorId}/xml/{xmlId}")
    class DeleteXmlTests {

        @Test
        @DisplayName("Löscht XML erfolgreich")
        void deleteXml_success() throws Exception {
            when(xmlAutorService.deleteXmlForAutor(1L)).thenReturn(true);

            mockMvc.perform(delete("/api/autoren/1/xml/5"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Gibt 404 bei nicht gefunden")
        void deleteXml_notFound() throws Exception {
            when(xmlAutorService.deleteXmlForAutor(1L)).thenReturn(false);

            mockMvc.perform(delete("/api/autoren/1/xml/999"))
                    .andExpect(status().isNotFound());
        }
    }
}
