package it.gdorsi.service.response;

public record XmlResponse(
    Long id,
    String dateiname,
    String inhalt,
    String autorName,
    String status
) {
    public static XmlResponse success(Long id, String dateiname, String inhalt, String autorName) {
        return new XmlResponse(id, dateiname, inhalt, autorName, "OK");
    }
    
    public static XmlResponse error(String autorName, String errorMessage) {
        return new XmlResponse(null, null, null, autorName, "FEHLER: " + errorMessage);
    }
    
    public static XmlResponse notFound(String autorName, Long xmlId) {
        return new XmlResponse(null, null, null, autorName, "FEHLER: XML mit ID " + xmlId + " nicht gefunden");
    }
}