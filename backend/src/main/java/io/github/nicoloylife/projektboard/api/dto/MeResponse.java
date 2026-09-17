package io.github.nicoloylife.projektboard.api.dto;

import io.github.nicoloylife.projektboard.domain.Role;
import io.github.nicoloylife.projektboard.domain.User;

public record MeResponse(Long id, String username, String displayName, Role role) {

    public static MeResponse from(User user) {
        return new MeResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole());
    }
}
