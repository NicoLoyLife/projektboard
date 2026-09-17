package io.github.nicoloylife.projektboard.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TaskRequest(
        @NotBlank(message = "darf nicht leer sein")
        @Size(max = 200, message = "darf höchstens 200 Zeichen lang sein")
        String title,
        @Size(max = 2000, message = "darf höchstens 2000 Zeichen lang sein")
        String description,
        Long assigneeId,
        LocalDate dueDate) {
}
