package it.gdorsi.service.response;

public record XmlResponse(
    Long id,
    String dateiname,
    String inhalt,
    String autorName,
    String status,
    Double similarityScore
) {
    public static XmlResponse success(Long id, String dateiname, String inhalt, String autorName) {
        return new XmlResponse(id, dateiname, inhalt, autorName, "OK", null);
    }
    
    public static XmlResponse error(String autorName, String errorMessage) {
        return new XmlResponse(null, null, null, autorName, "FEHLER: " + errorMessage, null);
    }
    
    public static XmlResponse notFound(String autorName, Long xmlId) {
        return new XmlResponse(null, null, null, autorName, "FEHLER: XML mit ID " + xmlId + " nicht gefunden", null);
    }
    
    public static XmlResponse searchResult(Long id, String dateiname, String inhalt, String autorName, Double similarityScore) {
        return new XmlResponse(id, dateiname, inhalt, autorName, "SEARCH_RESULT", similarityScore);
    }
}