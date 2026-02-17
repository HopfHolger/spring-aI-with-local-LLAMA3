package it.gdorsi.config;

import java.time.Duration;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.web.client.RestClient;

import jakarta.persistence.Timeout;

@Configuration
public class AiConfig {

    @Bean
    public PgVectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(1024)            // Dein mxbai-embed-large Wert
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .initializeSchema(true)
                .build();
    }


    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        // Hier wird der moderne Client mit der .prompt() API erzeugt
        return builder
                .defaultSystem("Du bist ein Experte für Cloud-Architektur.")
                .build();
    }

    @Bean
    public RestClient.Builder restClientBuilder() {
        // Nutzt den Standard Java 11+ HttpClient
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(60)); // Großzügig für Ollama

        return RestClient.builder()
                .requestFactory(factory);
    }

}
