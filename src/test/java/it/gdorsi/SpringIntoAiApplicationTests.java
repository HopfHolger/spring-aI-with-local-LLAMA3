package it.gdorsi;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test") // WICHTIG: Nutzt das Test-Profil
class SpringIntoAiApplicationTests {

	@MockitoBean
	private EmbeddingModel embeddingModel;

	@MockitoBean
	private VectorStore vectorStore;

	@MockitoBean
	private ChatModel chatModel; // Ersetzt das echte OllamaChatModel

	@Test
	void contextLoads() {
	}

	@Test
	void testChat() {
		// Setup des Mocks
		when(chatModel.call("Hallo")).thenReturn("Simulierte Antwort");

		// Service-Aufruf
		// ...
	}

}
