package io.github.nicoloylife.projektboard.api;

import io.github.nicoloylife.projektboard.api.dto.CreateUserRequest;
import io.github.nicoloylife.projektboard.api.dto.UpdateUserRequest;
import io.github.nicoloylife.projektboard.api.dto.UserResponse;
import io.github.nicoloylife.projektboard.domain.User;
import io.github.nicoloylife.projektboard.security.CurrentUserService;
import io.github.nicoloylife.projektboard.service.UserService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Benutzerkonten und Rollen, nur für Administratoren. */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;
    private final CurrentUserService currentUserService;

    public UserController(UserService userService, CurrentUserService currentUserService) {
        this.userService = userService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<UserResponse> list() {
        return userService.listUsers(currentUserService.require()).stream()
                .map(UserResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        User created = userService.createUser(currentUserService.require(), request.username(), request.password(),
                request.displayName(), request.role());
        return ResponseEntity.created(URI.create("/api/users/" + created.getId())).body(UserResponse.from(created));
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        User updated = userService.updateUser(currentUserService.require(), id, request.displayName(), request.role(),
                request.active(), request.password());
        return UserResponse.from(updated);
    }
}
