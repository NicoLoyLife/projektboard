package io.github.nicoloylife.projektboard.api.dto;

import io.github.nicoloylife.projektboard.domain.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusRequest(@NotNull(message = "muss angegeben werden") TaskStatus status) {
}
