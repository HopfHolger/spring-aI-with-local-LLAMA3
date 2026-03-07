package it.gdorsi.dao;

public record XmlValidationResult(boolean gueltig, String fehlermeldung) {
    public static XmlValidationResult erfolgreich() {
        return new XmlValidationResult(true, null);
    }

    public static XmlValidationResult fehler(String nachricht) {
        return new XmlValidationResult(false, nachricht);
    }
}
