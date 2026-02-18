package it.gdorsi.service;

import org.springframework.ai.tool.annotation.Tool;

/**
 * Damit @Tool und @Transactional getrennt sind, wegen spring proxy kann es
 * passieren, dass @Tool sonst nicht gefunden wird.
 */
public interface AutorOperations {

    @Tool(description = "Speichert einen neuen Autor in der Datenbank")
    String saveAutor(String name, String biografie);

    @Tool(description = "Löscht einen Autor unwiderruflich aus der Datenbank")
    String deleteAutor(String name);
}
