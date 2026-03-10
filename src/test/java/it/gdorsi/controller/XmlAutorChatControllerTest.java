package it.gdorsi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.gdorsi.repository.AuthorRepository;
import it.gdorsi.repository.model.Autor;
import it.gdorsi.repository.model.XmlDokument;
import it.gdorsi.service.XmlDokumentService;
import it.gdorsi.service.XmlOperations;
import it.gdorsi.service.response.XmlListResponse;
import it.gdorsi.service.response.XmlResponse;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = Strictness.LENIENT)
class XmlAutorChatControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ChatClient chatClient;



    @Mock
    private XmlOperations xmlOperations;

    @Mock
    private XmlDokumentService xmlDokumentService;

    private XmlAutorChatController xmlAutorChatController;

    private MockMultipartFile xmlFile;
    private Autor autor;
    private XmlDokument xmlDokument;

    @BeforeEach
    void setUp() {
        // Erstelle Controller direkt mit Mocks
        xmlAutorChatController = new XmlAutorChatController(
            chatClient, 
            xmlOperations, 
            xmlDokumentService
        );

        mockMvc = MockMvcBuilders.standaloneSetup(xmlAutorChatController).build();
        
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
        @DisplayName("Erstellt XML erfolgreich (old-school)")
        void createXml_success() throws Exception {
            when(xmlDokumentService.getAutorNameById(1L)).thenReturn("Test Autor");
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
        @DisplayName("Gibt 404 wenn Autor nicht gefunden")
        void getAllXml_autorNotFound() throws Exception {
            when(xmlDokumentService.getAutorNameById(999L)).thenReturn(null);

            mockMvc.perform(get("/api/autoren/999/xml"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/autoren/{autorId}/xml/{xmlId}")
    class GetXmlByIdTests {

        @Test
        @DisplayName("Gibt 404 wenn Autor nicht gefunden")
        void getXmlById_autorNotFound() throws Exception {
            when(xmlDokumentService.getAutorNameById(999L)).thenReturn(null);

            mockMvc.perform(get("/api/autoren/999/xml/5"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/autoren/{autorId}/xml/{xmlId}")
    class UpdateXmlTests {

        @Test
        @DisplayName("Gibt 404 wenn Autor nicht gefunden")
        void updateXml_autorNotFound() throws Exception {
            when(xmlDokumentService.getAutorNameById(999L)).thenReturn(null);

            mockMvc.perform(multipart("/api/autoren/999/xml/5")
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
        @DisplayName("Gibt 404 wenn Autor nicht gefunden")
        void deleteXml_autorNotFound() throws Exception {
            when(xmlDokumentService.getAutorNameById(999L)).thenReturn(null);

            mockMvc.perform(delete("/api/autoren/999/xml/5"))
                    .andExpect(status().isNotFound());
        }
    }
}
