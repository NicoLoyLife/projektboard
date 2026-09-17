package io.github.nicoloylife.projektboard.repository;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.nicoloylife.projektboard.domain.Project;
import io.github.nicoloylife.projektboard.domain.ProjectStatus;
import io.github.nicoloylife.projektboard.domain.Role;
import io.github.nicoloylife.projektboard.domain.Task;
import io.github.nicoloylife.projektboard.domain.TaskStatus;
import io.github.nicoloylife.projektboard.domain.Tenant;
import io.github.nicoloylife.projektboard.domain.User;
import io.github.nicoloylife.projektboard.service.TaskCounts;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class ProjectRepositoryTest {

    @Autowired
    private TenantRepository tenants;
    @Autowired
    private UserRepository users;
    @Autowired
    private ProjectRepository projects;
    @Autowired
    private TaskRepository tasks;

    private Tenant tenant;
    private User leitung;
    private User anna;
    private User ben;
    private User chris;
    private Project relaunch;
    private Project migration;

    @BeforeEach
    void setUpData() {
        tenant = tenants.save(new Tenant("Testmandant"));
        leitung = users.save(new User("leitung", "hash", "Petra Lang", Role.PROJECT_MANAGER, tenant));
        anna = users.save(new User("anna", "hash", "Anna Berger", Role.EMPLOYEE, tenant));
        ben = users.save(new User("ben", "hash", "Ben Kaiser", Role.EMPLOYEE, tenant));
        chris = users.save(new User("chris", "hash", "Chris Vogt", Role.EMPLOYEE, tenant));

        relaunch = new Project("Kundenportal Relaunch", null, leitung, tenant);
        relaunch.getMembers().addAll(List.of(anna, ben));
        projects.save(relaunch);

        migration = new Project("Interne Migration", null, leitung, tenant);
        migration.getMembers().add(anna);
        projects.save(migration);

        Project website = new Project("Website 2025", null, leitung, tenant);
        website.getMembers().add(ben);
        website.setStatus(ProjectStatus.ARCHIVED);
        projects.save(website);

        tasks.saveAll(List.of(
                taskWithStatus("A", relaunch, TaskStatus.DONE),
                taskWithStatus("B", relaunch, TaskStatus.DONE),
                taskWithStatus("C", relaunch, TaskStatus.IN_PROGRESS),
                taskWithStatus("D", relaunch, TaskStatus.OPEN),
                taskWithStatus("E", relaunch, TaskStatus.OPEN),
                taskWithStatus("F", migration, TaskStatus.OPEN)));
    }

    @Test
    void managerSeesAllProjectsSheLeads() {
        List<Project> visible = projects.findVisibleFor(tenant.getId(), leitung.getId());
        assertThat(visible).extracting(Project::getName)
                .containsExactly("Interne Migration", "Kundenportal Relaunch", "Website 2025");
    }

    @Test
    void memberSeesOnlyProjectsSheBelongsTo() {
        assertThat(projects.findVisibleFor(tenant.getId(), anna.getId()))
                .extracting(Project::getName)
                .containsExactly("Interne Migration", "Kundenportal Relaunch");
        assertThat(projects.findVisibleFor(tenant.getId(), ben.getId()))
                .extracting(Project::getName)
                .containsExactly("Kundenportal Relaunch", "Website 2025");
    }

    @Test
    void userWithoutMembershipSeesNothing() {
        assertThat(projects.findVisibleFor(tenant.getId(), chris.getId())).isEmpty();
    }

    @Test
    void projectsOfAnotherTenantStayHidden() {
        Tenant other = tenants.save(new Tenant("Anderer Mandant"));
        User otherManager = users.save(new User("fremd", "hash", "Fremde Leitung", Role.PROJECT_MANAGER, other));
        Project foreign = new Project("Fremdes Projekt", null, otherManager, other);
        foreign.getMembers().add(anna);
        projects.save(foreign);

        assertThat(projects.findVisibleFor(tenant.getId(), anna.getId()))
                .extracting(Project::getName)
                .doesNotContain("Fremdes Projekt");
    }

    @Test
    void taskCountsAreGroupedPerProjectAndStatus() {
        Map<Long, TaskCounts> counts = TaskCounts.byProject(
                tasks.countByProjectIds(List.of(relaunch.getId(), migration.getId())));

        assertThat(counts.get(relaunch.getId())).isEqualTo(new TaskCounts(5, 2, 1, 2));
        assertThat(counts.get(relaunch.getId()).progressPercent()).isEqualTo(40);
        assertThat(counts.get(migration.getId())).isEqualTo(new TaskCounts(1, 1, 0, 0));
    }

    private static Task taskWithStatus(String title, Project project, TaskStatus status) {
        Task task = new Task(title, null, project, null, null);
        task.setStatus(status);
        return task;
    }
}
