package it.gdorsi.service;

import org.springframework.ai.tool.annotation.Tool;

import it.gdorsi.service.response.XmlListResponse;
import it.gdorsi.service.response.XmlResponse;

public interface XmlOperations {

    @Tool(description = "Speichert ein XML-Dokument für einen existierenden Autor")
    String saveXml(String autorName, String dateiname, String xmlInhalt);

    @Tool(description = "Löscht alle XML-Dokumente eines Autors")
    String deleteXmlByAutor(String autorName);

    @Tool(description = "Gibt alle XML-Dokumente eines Autors zurück")
    XmlListResponse getXmlByAutor(String autorName);

    @Tool(description = "Gibt ein spezifisches XML-Dokument nach ID zurück")
    XmlResponse getXmlById(String autorName, Long xmlId);

    @Tool(description = "Aktualisiert ein existierendes XML-Dokument")
    XmlResponse updateXml(String autorName, Long xmlId, String dateiname, String xmlInhalt);

    @Tool(description = "Löscht ein spezifisches XML-Dokument nach ID")
    String deleteXmlById(String autorName, Long xmlId);

    @Tool(description = "Sucht ähnliche XML-Dokumente basierend auf einer semantischen Anfrage")
    XmlListResponse searchSimilarXml(String query, int limit);
}
