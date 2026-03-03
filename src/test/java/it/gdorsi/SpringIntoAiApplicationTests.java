package it.gdorsi;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class SpringIntoAiApplicationTests {

	@MockitoBean
	private PgVectorStore vectorStore;

	@MockitoBean
	private ChatModel chatModel; // Ersetzt das echte OllamaChatModel

	@Disabled( "Testen nicht notwendig echtes Model laden viel zu groß")
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
