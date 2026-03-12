package config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.mockito.Mockito;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
@EnableAutoConfiguration(exclude = {
    org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration.class
})
public class TestConfig {

    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        EmbeddingModel mockModel = Mockito.mock(EmbeddingModel.class);

        // Standard-Verhalten: Gib immer einen simplen Test-Vektor zurück (1024 Dimensionen für pgvector)
        float[] dummyVector = new float[1024];
        for (int i = 0; i < dummyVector.length; i++) {
            dummyVector[i] = (float) Math.random() * 0.1f; // Kleine Zufallswerte
        }

        when(mockModel.embed(anyString())).thenReturn(dummyVector);
        // Falls dein VectorStore auch Dokumente batch-verarbeitet:
        // embed(Document) gibt float[] zurück
        when(mockModel.embed(any(Document.class))).thenReturn(dummyVector);

        return mockModel;
    }

    @Bean
    @Primary // Damit dieser Store den echten Ollama-Vektor-Store ersetzt
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel)
                // Hier könnten noch Parameter wie der Speicherort (File)
                // konfiguriert werden, für Tests lassen wir es "in-memory"
                .build();
    }
}
