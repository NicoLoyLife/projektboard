package io.github.nicoloylife.projektboard.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Setzt die Mitgliederliste eines Projekts vollständig neu. */
public record MembersRequest(@NotNull(message = "muss angegeben werden") List<Long> userIds) {
}
