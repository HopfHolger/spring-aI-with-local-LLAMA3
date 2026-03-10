package it.gdorsi.service.response;

import java.util.List;

public record XmlListResponse(
    List<XmlResponse> dokumente,
    int count,
    String status
) {
    public static XmlListResponse success(List<XmlResponse> dokumente) {
        return new XmlListResponse(dokumente, dokumente.size(), "OK");
    }
    
    public static XmlListResponse error(String errorMessage) {
        return new XmlListResponse(List.of(), 0, "FEHLER: " + errorMessage);
    }
    
    public static XmlListResponse empty(String autorName) {
        return new XmlListResponse(List.of(), 0, "KEINE_DOKUMENTE: Keine XML-Dokumente für Autor " + autorName + " gefunden");
    }
    
    public static XmlListResponse searchResults(List<XmlResponse> dokumente, String query) {
        return new XmlListResponse(dokumente, dokumente.size(), "SUCHERGEBNISSE für: " + query);
    }
}