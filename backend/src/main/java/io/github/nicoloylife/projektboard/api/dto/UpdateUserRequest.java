package io.github.nicoloylife.projektboard.api.dto;

import io.github.nicoloylife.projektboard.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Änderung eines Benutzers. Ein neues Passwort ist optional und wird nur gesetzt, wenn es angegeben ist. */
public record UpdateUserRequest(
        @NotBlank(message = "darf nicht leer sein")
        @Size(max = 100, message = "darf höchstens 100 Zeichen lang sein")
        String displayName,
        @NotNull(message = "muss angegeben werden")
        Role role,
        @NotNull(message = "muss angegeben werden")
        Boolean active,
        @Size(min = 8, max = 100, message = "muss mindestens 8 Zeichen lang sein")
        String password) {
}
