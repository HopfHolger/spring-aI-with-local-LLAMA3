package it.gdorsi.service;

import org.springframework.ai.tool.annotation.Tool;

public interface XmlOperations {

    @Tool(description = "Speichert ein XML-Dokument für einen existierenden Autor")
    String saveXml(String autorName, String dateiname, String xmlInhalt);

    @Tool(description = "Löscht alle XML-Dokumente eines Autors")
    String deleteXmlByAutor(String autorName);

    @Tool(description = "Gibt alle XML-Dokumente eines Autors zurück")
    String getXmlByAutor(String autorName);
}
