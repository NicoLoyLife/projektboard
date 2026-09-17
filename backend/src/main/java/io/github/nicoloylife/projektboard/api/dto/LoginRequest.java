package io.github.nicoloylife.projektboard.api.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "darf nicht leer sein") String username,
        @NotBlank(message = "darf nicht leer sein") String password) {
}
