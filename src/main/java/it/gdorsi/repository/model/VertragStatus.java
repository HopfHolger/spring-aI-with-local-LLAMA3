package it.gdorsi.repository.model;

public enum VertragStatus {
    ENTWURF("Entwurf"),
    AKTIV("Aktiv"),
    GEKUENDIGT("Gekündigt");

    private final String label;

    VertragStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    // Hilfsmethode für die KI (tolerant gegenüber Kleinschreibung)
    public static VertragStatus fromString(String input) {
        if (input == null || input.isBlank() || input.contains("<")) {
            return null; // Ignoriere KI-Platzhalter wie "<keine Änderung>"
        }
        for (VertragStatus s : VertragStatus.values()) {
            if (s.name().equalsIgnoreCase(input.trim()) ||
                    s.label.equalsIgnoreCase(input.trim())) {
                return s;
            }
        }
        return null; // Kein Treffer -> Altvorgabe beibehalten
    }
}

