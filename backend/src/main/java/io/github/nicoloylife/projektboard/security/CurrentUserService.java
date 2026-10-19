package io.github.nicoloylife.projektboard.security;

import io.github.nicoloylife.projektboard.domain.User;
import io.github.nicoloylife.projektboard.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Liefert den angemeldeten Benutzer als Entität aus der Datenbank. Die Sitzung hält die Rolle aus der
 * Anmeldung. Ist das Konto inzwischen deaktiviert oder hat es eine andere Rolle, gilt die Sitzung nicht
 * mehr. Die Anfrage endet dann mit 401, der ApiExceptionHandler beendet die Sitzung und das Frontend meldet ab.
 * Endpunkte mit @PreAuthorize prüfen vorher die Rolle aus der Sitzung. Stammt ein 403 von einer Sitzung, deren
 * Rolle sich geändert hat, macht der ApiExceptionHandler daraus ebenfalls ein 401 und beendet die Sitzung.
 */
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
            throw loginRequired();
        }
        User user = users.findByUsername(authentication.getName()).orElseThrow(CurrentUserService::loginRequired);
        boolean sameRole = AuthorityUtils.authorityListToSet(authentication.getAuthorities())
                .contains("ROLE_" + user.getRole().name());
        if (!user.isActive() || !sameRole) {
            throw loginRequired();
        }
        return user;
    }

    private static AuthenticationCredentialsNotFoundException loginRequired() {
        return new AuthenticationCredentialsNotFoundException("Anmeldung erforderlich");
    }
}
