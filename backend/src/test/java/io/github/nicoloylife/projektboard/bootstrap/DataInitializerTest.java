package io.github.nicoloylife.projektboard.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.nicoloylife.projektboard.domain.User;
import io.github.nicoloylife.projektboard.repository.ProjectRepository;
import io.github.nicoloylife.projektboard.repository.TaskRepository;
import io.github.nicoloylife.projektboard.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DataInitializerTest {

    @Autowired
    private UserRepository users;
    @Autowired
    private ProjectRepository projects;
    @Autowired
    private TaskRepository tasks;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void demoDataIsCreatedOnce() {
        assertThat(users.count()).isEqualTo(5);
        assertThat(projects.count()).isEqualTo(3);
        assertThat(tasks.count()).isEqualTo(12);
    }

    @Test
    void demoPasswordsAreStoredAsBcryptHash() {
        User admin = users.findByUsername("admin").orElseThrow();
        assertThat(admin.getPasswordHash()).startsWith("$2");
        assertThat(passwordEncoder.matches(DataInitializer.DEMO_PASSWORD, admin.getPasswordHash())).isTrue();
    }
}
