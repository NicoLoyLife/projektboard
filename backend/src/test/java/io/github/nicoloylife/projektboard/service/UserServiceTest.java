package io.github.nicoloylife.projektboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.nicoloylife.projektboard.domain.Role;
import io.github.nicoloylife.projektboard.domain.Tenant;
import io.github.nicoloylife.projektboard.domain.User;
import io.github.nicoloylife.projektboard.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository users;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService service;
    private Tenant tenant;
    private User admin;

    @BeforeEach
    void setUp() {
        service = new UserService(users, passwordEncoder);
        tenant = new Tenant("LoyLife Coding GmbH");
        ReflectionTestUtils.setField(tenant, "id", 1L);
        admin = new User("admin", "hash", "Administration", Role.ADMIN, tenant);
        ReflectionTestUtils.setField(admin, "id", 10L);
    }

    @Test
    void createUserHashesPasswordAndUsesTenantOfAdmin() {
        given(users.findByUsername("neu")).willReturn(Optional.empty());
        given(passwordEncoder.encode("geheim123")).willReturn("bcrypt");
        given(users.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        User created = service.createUser(admin, "neu", "geheim123", "Neue Person", Role.EMPLOYEE);

        assertThat(created.getPasswordHash()).isEqualTo("bcrypt");
        assertThat(created.getTenant()).isSameAs(tenant);
        assertThat(created.getRole()).isEqualTo(Role.EMPLOYEE);
        assertThat(created.isActive()).isTrue();
    }

    @Test
    void duplicateUsernameIsRejectedAsFieldError() {
        given(users.findByUsername("anna")).willReturn(Optional.of(new User("anna", "h", "Anna", Role.EMPLOYEE, tenant)));

        assertThatThrownBy(() -> service.createUser(admin, "anna", "geheim123", "Anna", Role.EMPLOYEE))
                .isInstanceOf(ValidationException.class)
                .extracting("field").isEqualTo("username");
        verify(users, never()).save(any());
    }

    @Test
    void updateChangesFieldsAndKeepsPasswordWhenNoneGiven() {
        User anna = userWithId(20L, "anna", Role.EMPLOYEE);
        given(users.findById(20L)).willReturn(Optional.of(anna));

        User updated = service.updateUser(admin, 20L, "Anna Berger-Klein", Role.PROJECT_MANAGER, false, null);

        assertThat(updated.getDisplayName()).isEqualTo("Anna Berger-Klein");
        assertThat(updated.getRole()).isEqualTo(Role.PROJECT_MANAGER);
        assertThat(updated.isActive()).isFalse();
        assertThat(updated.getPasswordHash()).isEqualTo("hash");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void updateWithPasswordStoresNewHash() {
        User anna = userWithId(20L, "anna", Role.EMPLOYEE);
        given(users.findById(20L)).willReturn(Optional.of(anna));
        given(passwordEncoder.encode("neues1234")).willReturn("neuer-hash");

        service.updateUser(admin, 20L, "Anna Berger", Role.EMPLOYEE, true, "neues1234");

        assertThat(anna.getPasswordHash()).isEqualTo("neuer-hash");
    }

    @Test
    void adminCannotDeactivateOwnAccount() {
        given(users.findById(10L)).willReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.updateUser(admin, 10L, "Administration", Role.ADMIN, false, null))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.updateUser(admin, 10L, "Administration", Role.EMPLOYEE, true, null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void unknownUserOrOtherTenantIsNotFound() {
        given(users.findById(99L)).willReturn(Optional.empty());
        Tenant other = new Tenant("Anderer Mandant");
        ReflectionTestUtils.setField(other, "id", 2L);
        User foreign = new User("fremd", "h", "Fremd", Role.EMPLOYEE, other);
        ReflectionTestUtils.setField(foreign, "id", 30L);
        given(users.findById(30L)).willReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.updateUser(admin, 99L, "x", Role.EMPLOYEE, true, null))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.updateUser(admin, 30L, "x", Role.EMPLOYEE, true, null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void listUsesTenantOfAdmin() {
        service.listUsers(admin);

        ArgumentCaptor<Long> tenantId = ArgumentCaptor.forClass(Long.class);
        verify(users).findByTenantIdOrderByUsernameAsc(tenantId.capture());
        assertThat(tenantId.getValue()).isEqualTo(1L);
    }

    private User userWithId(long id, String username, Role role) {
        User user = new User(username, "hash", "Anna Berger", role, tenant);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
