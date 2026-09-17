package io.github.nicoloylife.projektboard.api.dto;

import io.github.nicoloylife.projektboard.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "darf nicht leer sein")
        @Size(min = 3, max = 50, message = "muss 3 bis 50 Zeichen lang sein")
        @Pattern(regexp = "[a-z0-9._]+", message = "nur Kleinbuchstaben, Ziffern, Punkt und Unterstrich")
        String username,
        @NotBlank(message = "darf nicht leer sein")
        @Size(min = 8, max = 100, message = "muss mindestens 8 Zeichen lang sein")
        String password,
        @NotBlank(message = "darf nicht leer sein")
        @Size(max = 100, message = "darf höchstens 100 Zeichen lang sein")
        String displayName,
        @NotNull(message = "muss angegeben werden")
        Role role) {
}
