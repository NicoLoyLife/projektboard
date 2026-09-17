package io.github.nicoloylife.projektboard.api.dto;

import io.github.nicoloylife.projektboard.domain.Role;
import io.github.nicoloylife.projektboard.domain.User;

public record UserResponse(Long id, String username, String displayName, Role role, boolean active) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole(), user.isActive());
    }
}
