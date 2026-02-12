package it.gdorsi.config;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Testet beim Starten, ob alles in Ordnung ist :-)
 */
@Configuration
public class AiStartTestConfig {

    @Bean
    CommandLineRunner testAi(VectorStore vectorStore) {
        return args -> {
            try {
                System.out.println(">>> START: KI-Infrastruktur Test");

                // Wir erstellen ein Test-Dokument manuell (ohne PDF-Library Stress)
                Document testDoc = new Document(
                        "Das ist ein Test-Inhalt, um die Verbindung zu Ollama und Postgres zu prüfen.",
                        Map.of("typ", "test-run", "version", "1.0")
                );

                // 1. Ingest: Vektorisieren und Speichern
                System.out.println(">>> Sende Daten an Ollama zur Vektorisierung...");
                vectorStore.accept(List.of(testDoc));
                System.out.println(">>> Erfolg: Daten in Postgres gespeichert.");

                // 2. Suche: Test-Abfrage starten
                System.out.println(">>> Teste semantische Suche...");
                var results = vectorStore.similaritySearch("Was ist das für ein Test?");

                if (!results.isEmpty()) {
                    System.out.println(">>> Suche erfolgreich! Gefunden: " + results.getFirst().getText());
                }

                System.out.println(">>> ENDE: KI-Infrastruktur ist BEREIT!");

            } catch (Exception e) {
                System.err.println(">>> FEHLER beim KI-Test: " + e.getMessage());
            }
        };
    }

    @Bean
    CommandLineRunner debugIngest(VectorStore vectorStore) {
        return args -> {
            System.out.println("Starte manuellen Ingest...");
            Document doc = new Document("Java Test Content", Map.of("debug", "true"));
            vectorStore.accept(List.of(doc));
            System.out.println("Sollte jetzt in der DB sein.");

            // Sofortige Gegenprüfung im selben Code:
            var results = vectorStore.similaritySearch("Java");
            results.forEach(d -> System.out.println("Gefunden ID in DB: " + d.getId()));
            System.out.println("Gefundene Treffer in der DB: " + results.size());

        };
    }

    @Bean
    CommandLineRunner checkDb(JdbcTemplate jdbcTemplate) {
        return args -> {
            String url = null;
            if (jdbcTemplate.getDataSource() != null) {
                url = jdbcTemplate.getDataSource().getConnection().getMetaData().getURL();
            }
            System.out.println("Java schreibt aktuell in: " + url);
        };
    }
}
