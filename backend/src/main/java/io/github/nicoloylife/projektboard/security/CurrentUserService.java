package io.github.nicoloylife.projektboard.security;

import io.github.nicoloylife.projektboard.domain.User;
import io.github.nicoloylife.projektboard.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Liefert den angemeldeten Benutzer als Entität aus der Datenbank. */
@Service
public class CurrentUserService {

    private final UserRepository users;

    public CurrentUserService(UserRepository users) {
        this.users = users;
    }

    public User require() {
        Authentication authentication = SecurityContextHolder.getContextHolderStrategy().getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException("Anmeldung erforderlich");
        }
        return users.findByUsername(authentication.getName())
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Anmeldung erforderlich"));
    }
}
