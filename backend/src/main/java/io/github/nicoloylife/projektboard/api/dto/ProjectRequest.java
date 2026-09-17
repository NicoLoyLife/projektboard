package io.github.nicoloylife.projektboard.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
        @NotBlank(message = "darf nicht leer sein")
        @Size(max = 100, message = "darf höchstens 100 Zeichen lang sein")
        String name,
        @Size(max = 2000, message = "darf höchstens 2000 Zeichen lang sein")
        String description) {
}
