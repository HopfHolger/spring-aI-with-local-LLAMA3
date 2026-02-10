package it.gdorsi.dao;

/**
 * ein PDF wird in Chunks unterteilt, sollte ein Chung schon in der DB sein, dann wird daraus ein
 * updatet Chunk erzeugt.
 * Sonst wird ein neuer Chunk erzeugt. - das PDF hat sich dann wohl vergrößert...
 * <p>
 * Hier ist das Szenario bei einem PDF, das von 10 auf 7 Chunks gekürzt wurde:
 * Chunks 0 bis 6: Werden per Upsert aktualisiert (updateCount steigt).
 * Chunks 7 bis 9: Diese IDs werden im neuen Durchlauf gar nicht mehr generiert. Sie bleiben mit dem alten Stand in der Datenbank liegen.
 * Das Problem: Die KI findet bei einer Suche dann sowohl die neuen Infos (0–6) als auch die veralteten „Leichen“ (7–9), was zu falschen Antworten führt.
 *  um die update funlktion zu erhalten
 * <p>
 *     Zwei-Phasen-Logik anwenden:
 *  Generiere alle neuen Chunks und deren IDs (ohne zu speichern).
 * Frage die DB: „Welche IDs von fileName existieren aktuell in der DB, die nicht in meiner neuen Liste sind?“ -> Lösche diese.
 * Führe den accept() für die neuen Chunks aus.
 * <p>
 * @param fileName
 * @param totalChunks
 * @param newChunks
 * @param updatedChunks
 * @param durationMillis
 */
public record PdfIngestResult(
        String fileName,
        int totalChunks,
        int newChunks,
        int updatedChunks,
        long durationMillis
) {}
