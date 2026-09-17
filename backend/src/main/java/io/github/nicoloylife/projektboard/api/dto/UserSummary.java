package io.github.nicoloylife.projektboard.api.dto;

import io.github.nicoloylife.projektboard.domain.User;

/** Kurzform eines Benutzers für Zuordnungen in Projekten und Aufgaben. */
public record UserSummary(Long id, String displayName) {

    public static UserSummary from(User user) {
        return user == null ? null : new UserSummary(user.getId(), user.getDisplayName());
    }
}
