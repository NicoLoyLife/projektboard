package io.github.nicoloylife.projektboard.repository;

import io.github.nicoloylife.projektboard.domain.Role;
import io.github.nicoloylife.projektboard.domain.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    List<User> findByTenantIdOrderByUsernameAsc(Long tenantId);

    /** Benutzer, die als Mitglied oder bearbeitende Person zuordenbar sind. */
    List<User> findByTenantIdAndActiveTrueAndRoleInOrderByDisplayNameAsc(Long tenantId, Collection<Role> roles);
}
