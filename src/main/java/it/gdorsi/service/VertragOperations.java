package it.gdorsi.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import it.gdorsi.dao.VertragRequest;
import it.gdorsi.repository.model.VertragStatus;

public interface VertragOperations {

    @Tool(description = "Extrahiert Vertragsdaten aus Dokumenten und speichert sie mit 8 Feldern (Nummer, Kunde, Start, Ende, Betrag, Typ, Status, Bemerkung) in der Datenbank")
    String saveContractFromPdf(VertragRequest request);

    @Tool(description = "Ändert NUR den Betrag oder den Status eines bestehenden Vertrags. Andere Felder werden ignoriert.")
    String updateContractStatusOrAmount(
            @ToolParam(description = "Die Vertragsnummer") String vertragsNummer,
            @ToolParam(description = "Der neue Betrag") Double betrag,
            @ToolParam(description = "Der neue Status (AKTIV, ENTWURF, GEKUENDIGT)") VertragStatus status
    );
}
