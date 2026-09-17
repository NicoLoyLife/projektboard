package io.github.nicoloylife.projektboard.api.dto;

import io.github.nicoloylife.projektboard.domain.ProjectStatus;
import jakarta.validation.constraints.NotNull;

public record ProjectStatusRequest(@NotNull(message = "muss angegeben werden") ProjectStatus status) {
}
