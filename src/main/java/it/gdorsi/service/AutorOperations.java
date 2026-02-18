package it.gdorsi.service;

import org.springframework.ai.tool.annotation.Tool;

import it.gdorsi.dao.AutorRequest;

public interface AutorOperations {

    @Tool(description = "Speichert einen neuen Autor in der Datenbank")
    String saveAutor(String name, String biografie);
}
