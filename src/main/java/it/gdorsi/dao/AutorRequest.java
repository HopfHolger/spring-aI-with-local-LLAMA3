package it.gdorsi.dao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AutorRequest(
        @NotBlank(message = "Name darf nicht leer sein")
        @Size(min = 2, max = 100, message = "Name muss zwischen 2 und 100 Zeichen lang sein")
        String name,
        @NotBlank(message = "Biografie darf nicht leer sein")
        @Size(max = 2000, message = "Biografie ist zu lang (max. 2000 Zeichen)")
        String biografie) {
    //Es wird der Name aus dem Prompt extrahiert, biografie ist der Rest... (?)
}
