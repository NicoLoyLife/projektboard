package io.github.nicoloylife.projektboard.bootstrap;

import io.github.nicoloylife.projektboard.domain.Project;
import io.github.nicoloylife.projektboard.domain.ProjectStatus;
import io.github.nicoloylife.projektboard.domain.Role;
import io.github.nicoloylife.projektboard.domain.Task;
import io.github.nicoloylife.projektboard.domain.TaskStatus;
import io.github.nicoloylife.projektboard.domain.Tenant;
import io.github.nicoloylife.projektboard.domain.User;
import io.github.nicoloylife.projektboard.repository.ProjectRepository;
import io.github.nicoloylife.projektboard.repository.TaskRepository;
import io.github.nicoloylife.projektboard.repository.TenantRepository;
import io.github.nicoloylife.projektboard.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Legt Demo-Daten an, wenn die Datenbank noch keine Benutzer enthält.
 * Das Passwort aller Demo-Konten lautet demo1234 und dient nur der Demonstration.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    static final String DEMO_PASSWORD = "demo1234";

    private final TenantRepository tenants;
    private final UserRepository users;
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(TenantRepository tenants, UserRepository users, ProjectRepository projects,
            TaskRepository tasks, PasswordEncoder passwordEncoder) {
        this.tenants = tenants;
        this.users = users;
        this.projects = projects;
        this.tasks = tasks;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        String hash = passwordEncoder.encode(DEMO_PASSWORD);
        Tenant tenant = tenants.save(new Tenant("LoyLife Coding GmbH"));

        users.save(new User("admin", hash, "Administration", Role.ADMIN, tenant));
        User leitung = users.save(new User("leitung", hash, "Petra Lang", Role.PROJECT_MANAGER, tenant));
        User anna = users.save(new User("anna", hash, "Anna Berger", Role.EMPLOYEE, tenant));
        User ben = users.save(new User("ben", hash, "Ben Kaiser", Role.EMPLOYEE, tenant));
        users.save(new User("chris", hash, "Chris Vogt", Role.EMPLOYEE, tenant));

        LocalDate today = LocalDate.now();

        Project relaunch = new Project("Kundenportal Relaunch",
                "Neuer Kundenauftritt mit Self-Service-Bereich.", leitung, tenant);
        relaunch.getMembers().addAll(List.of(anna, ben));
        projects.save(relaunch);
        tasks.saveAll(List.of(
                task("Anmeldeseite umsetzen", relaunch, anna, today.plusDays(1), TaskStatus.DONE),
                task("Datenmodell abstimmen", relaunch, leitung, today.plusDays(3), TaskStatus.DONE),
                task("Projektliste bauen", relaunch, ben, today.plusDays(8), TaskStatus.IN_PROGRESS),
                task("Aufgabenliste bauen", relaunch, anna, today.plusDays(13), TaskStatus.OPEN),
                task("Tests ergänzen", relaunch, null, null, TaskStatus.OPEN)));

        Project migration = new Project("Interne Migration",
                "Umzug der internen Werkzeuge auf die neue Serverumgebung.", leitung, tenant);
        migration.getMembers().add(anna);
        projects.save(migration);
        tasks.saveAll(List.of(
                task("Server inventarisieren", migration, anna, today.plusDays(5), TaskStatus.OPEN),
                task("Migrationsplan schreiben", migration, leitung, today.plusDays(12), TaskStatus.OPEN),
                task("Testumgebung aufbauen", migration, null, today.plusDays(20), TaskStatus.OPEN)));

        Project website = new Project("Website 2025",
                "Abgeschlossener Relaunch der Unternehmenswebsite.", leitung, tenant);
        website.getMembers().add(ben);
        website.setStatus(ProjectStatus.ARCHIVED);
        projects.save(website);
        tasks.saveAll(List.of(
                task("Inhalte übernehmen", website, ben, today.minusDays(60), TaskStatus.DONE),
                task("Design abstimmen", website, leitung, today.minusDays(50), TaskStatus.DONE),
                task("Formulare testen", website, ben, today.minusDays(40), TaskStatus.DONE),
                task("Freigabe einholen", website, leitung, today.minusDays(30), TaskStatus.DONE)));
    }

    private static Task task(String title, Project project, User assignee, LocalDate dueDate, TaskStatus status) {
        Task task = new Task(title, null, project, assignee, dueDate);
        task.setStatus(status);
        return task;
    }
}
