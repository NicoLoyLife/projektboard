package io.github.nicoloylife.projektboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import io.github.nicoloylife.projektboard.domain.Project;
import io.github.nicoloylife.projektboard.domain.ProjectStatus;
import io.github.nicoloylife.projektboard.domain.Role;
import io.github.nicoloylife.projektboard.domain.Tenant;
import io.github.nicoloylife.projektboard.domain.User;
import io.github.nicoloylife.projektboard.repository.ProjectRepository;
import io.github.nicoloylife.projektboard.repository.TaskRepository;
import io.github.nicoloylife.projektboard.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projects;
    @Mock
    private TaskRepository tasks;
    @Mock
    private UserRepository users;

    private ProjectService service;
    private Tenant tenant;
    private User admin;
    private User leitung;
    private User anna;
    private User chris;
    private Project relaunch;

    @BeforeEach
    void setUp() {
        service = new ProjectService(projects, tasks, users);
        tenant = new Tenant("LoyLife Coding GmbH");
        ReflectionTestUtils.setField(tenant, "id", 1L);
        admin = user(1L, "admin", Role.ADMIN);
        leitung = user(2L, "leitung", Role.PROJECT_MANAGER);
        anna = user(3L, "anna", Role.EMPLOYEE);
        chris = user(5L, "chris", Role.EMPLOYEE);
        relaunch = new Project("Kundenportal Relaunch", null, leitung, tenant);
        ReflectionTestUtils.setField(relaunch, "id", 100L);
        relaunch.getMembers().add(anna);
    }

    @Test
    void adminSeesAllProjectsOfTenant() {
        given(projects.findByTenantIdOrderByNameAsc(1L)).willReturn(List.of(relaunch));

        assertThat(service.listVisible(admin)).containsExactly(relaunch);
    }

    @Test
    void employeeSeesOnlyVisibleProjects() {
        given(projects.findVisibleFor(1L, 3L)).willReturn(List.of(relaunch));

        assertThat(service.listVisible(anna)).containsExactly(relaunch);
        verify(projects).findVisibleFor(1L, 3L);
    }

    @Test
    void memberAndManagerAndAdminMayReadProject() {
        given(projects.findById(100L)).willReturn(Optional.of(relaunch));

        assertThat(service.getVisible(anna, 100L)).isSameAs(relaunch);
        assertThat(service.getVisible(leitung, 100L)).isSameAs(relaunch);
        assertThat(service.getVisible(admin, 100L)).isSameAs(relaunch);
    }

    @Test
    void outsiderGetsNotFoundInsteadOfForbidden() {
        given(projects.findById(100L)).willReturn(Optional.of(relaunch));

        assertThatThrownBy(() -> service.getVisible(chris, 100L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void onlyManagerMayChangeProject() {
        given(projects.findById(100L)).willReturn(Optional.of(relaunch));

        assertThatThrownBy(() -> service.update(anna, 100L, "Neu", null)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.changeStatus(admin, 100L, ProjectStatus.ARCHIVED))
                .isInstanceOf(ForbiddenException.class);

        Project updated = service.update(leitung, 100L, "Neu", "Beschreibung");
        assertThat(updated.getName()).isEqualTo("Neu");
        assertThat(updated.getDescription()).isEqualTo("Beschreibung");
    }

    @Test
    void archivedProjectRejectsChangesButAllowsReactivation() {
        relaunch.setStatus(ProjectStatus.ARCHIVED);
        given(projects.findById(100L)).willReturn(Optional.of(relaunch));

        assertThatThrownBy(() -> service.update(leitung, 100L, "Neu", null)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.setMembers(leitung, 100L, List.of(3L))).isInstanceOf(ConflictException.class);

        assertThat(service.changeStatus(leitung, 100L, ProjectStatus.ACTIVE).getStatus()).isEqualTo(ProjectStatus.ACTIVE);
    }

    @Test
    void createSetsManagerAndTenantOfCurrentUser() {
        given(projects.save(org.mockito.ArgumentMatchers.any(Project.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        Project created = service.create(leitung, "Neues Projekt", null);

        assertThat(created.getManager()).isSameAs(leitung);
        assertThat(created.getTenant()).isSameAs(tenant);
        assertThat(created.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
    }

    @Test
    void setMembersReplacesListAndIgnoresManager() {
        given(projects.findById(100L)).willReturn(Optional.of(relaunch));
        given(users.findAllById(Set.of(5L))).willReturn(List.of(chris));

        Project updated = service.setMembers(leitung, 100L, List.of(2L, 5L));

        assertThat(updated.getMembers()).containsExactly(chris);
    }

    @Test
    void setMembersRejectsInactiveOrAdminUsers() {
        User inactive = user(6L, "alt", Role.EMPLOYEE);
        inactive.setActive(false);
        given(projects.findById(100L)).willReturn(Optional.of(relaunch));
        given(users.findAllById(Set.of(6L))).willReturn(List.of(inactive));

        assertThatThrownBy(() -> service.setMembers(leitung, 100L, List.of(6L)))
                .isInstanceOf(ValidationException.class)
                .extracting("field").isEqualTo("userIds");
        assertThat(relaunch.getMembers()).containsExactly(anna);
    }

    @Test
    void memberCandidatesExcludeManagerAndAdmin() {
        given(projects.findById(100L)).willReturn(Optional.of(relaunch));
        given(users.findByTenantIdAndActiveTrueAndRoleInOrderByDisplayNameAsc(1L, ProjectService.MEMBER_ROLES))
                .willReturn(List.of(leitung, anna, chris));

        assertThat(service.memberCandidates(leitung, 100L)).containsExactly(anna, chris);
    }

    @Test
    void tasksAreEditableForMembersOfActiveProjectsOnly() {
        assertThat(service.canEditTasks(anna, relaunch)).isTrue();
        assertThat(service.canEditTasks(leitung, relaunch)).isTrue();
        assertThat(service.canEditTasks(admin, relaunch)).isFalse();
        assertThat(service.canEditTasks(chris, relaunch)).isFalse();
        relaunch.setStatus(ProjectStatus.ARCHIVED);
        assertThat(service.canEditTasks(anna, relaunch)).isFalse();
    }

    private User user(long id, String username, Role role) {
        User user = new User(username, "hash", username, role, tenant);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
