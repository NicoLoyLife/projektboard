package io.github.nicoloylife.projektboard.service;

import io.github.nicoloylife.projektboard.domain.Role;
import io.github.nicoloylife.projektboard.domain.User;
import io.github.nicoloylife.projektboard.repository.UserRepository;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Benutzerverwaltung durch Administratoren innerhalb des eigenen Mandanten. */
@Service
@Transactional
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<User> listUsers(User admin) {
        return users.findByTenantIdOrderByUsernameAsc(admin.getTenant().getId());
    }

    public User createUser(User admin, String username, String password, String displayName, Role role) {
        if (users.findByUsername(username).isPresent()) {
            throw new ValidationException("username", "Benutzername ist bereits vergeben");
        }
        User user = new User(username, passwordEncoder.encode(password), displayName, role, admin.getTenant());
        return users.save(user);
    }

    public User updateUser(User admin, Long id, String displayName, Role role, boolean active, String newPassword) {
        User user = users.findById(id)
                .filter(candidate -> candidate.getTenant().getId().equals(admin.getTenant().getId()))
                .orElseThrow(() -> new NotFoundException("Benutzer nicht gefunden"));
        boolean self = user.getId().equals(admin.getId());
        if (self && (!active || role != Role.ADMIN)) {
            throw new ConflictException("Das eigene Konto kann nicht deaktiviert oder herabgestuft werden");
        }
        user.setDisplayName(displayName);
        user.setRole(role);
        user.setActive(active);
        if (newPassword != null && !newPassword.isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(newPassword));
        }
        return user;
    }
}
