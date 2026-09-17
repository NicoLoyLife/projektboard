package io.github.nicoloylife.projektboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.nicoloylife.projektboard.domain.Project;
import io.github.nicoloylife.projektboard.domain.ProjectStatus;
import io.github.nicoloylife.projektboard.domain.Role;
import io.github.nicoloylife.projektboard.domain.Task;
import io.github.nicoloylife.projektboard.domain.TaskStatus;
import io.github.nicoloylife.projektboard.domain.Tenant;
import io.github.nicoloylife.projektboard.domain.User;
import io.github.nicoloylife.projektboard.repository.TaskRepository;
import io.github.nicoloylife.projektboard.repository.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository tasks;
    @Mock
    private UserRepository users;
    @Mock
    private ProjectService projectService;

    private TaskService service;
    private User admin;
    private User leitung;
    private User anna;
    private User chris;
    private Project relaunch;

    @BeforeEach
    void setUp() {
        service = new TaskService(tasks, users, projectService);
        Tenant tenant = new Tenant("LoyLife Coding GmbH");
        ReflectionTestUtils.setField(tenant, "id", 1L);
        admin = user(1L, "admin", Role.ADMIN, tenant);
        leitung = user(2L, "leitung", Role.PROJECT_MANAGER, tenant);
        anna = user(3L, "anna", Role.EMPLOYEE, tenant);
        chris = user(5L, "chris", Role.EMPLOYEE, tenant);
        relaunch = new Project("Kundenportal Relaunch", null, leitung, tenant);
        ReflectionTestUtils.setField(relaunch, "id", 100L);
        relaunch.getMembers().add(anna);
    }

    @Test
    void memberCreatesTaskWithAssignee() {
        given(projectService.getVisible(anna, 100L)).willReturn(relaunch);
        given(users.findById(2L)).willReturn(Optional.of(leitung));
        given(tasks.save(any(Task.class))).willAnswer(invocation -> invocation.getArgument(0));

        Task created = service.create(anna, 100L, "Neue Aufgabe", "Text", 2L, LocalDate.of(2026, 10, 1));

        assertThat(created.getProject()).isSameAs(relaunch);
        assertThat(created.getAssignee()).isSameAs(leitung);
        assertThat(created.getStatus()).isEqualTo(TaskStatus.OPEN);
        assertThat(created.getDueDate()).isEqualTo(LocalDate.of(2026, 10, 1));
    }

    @Test
    void adminMayNotCreateTasks() {
        given(projectService.getVisible(admin, 100L)).willReturn(relaunch);

        assertThatThrownBy(() -> service.create(admin, 100L, "Aufgabe", null, null, null))
                .isInstanceOf(ForbiddenException.class);
        verify(tasks, never()).save(any());
    }

    @Test
    void archivedProjectRejectsNewTasks() {
        relaunch.setStatus(ProjectStatus.ARCHIVED);
        given(projectService.getVisible(anna, 100L)).willReturn(relaunch);

        assertThatThrownBy(() -> service.create(anna, 100L, "Aufgabe", null, null, null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void assigneeMustBeMemberOrManager() {
        given(projectService.getVisible(anna, 100L)).willReturn(relaunch);
        given(users.findById(5L)).willReturn(Optional.of(chris));

        assertThatThrownBy(() -> service.create(anna, 100L, "Aufgabe", null, 5L, null))
                .isInstanceOf(ValidationException.class)
                .extracting("field").isEqualTo("assigneeId");
    }

    @Test
    void statusChangeUpdatesTaskOfVisibleProject() {
        Task task = new Task("Aufgabe", null, relaunch, null, null);
        ReflectionTestUtils.setField(task, "id", 500L);
        given(tasks.findById(500L)).willReturn(Optional.of(task));
        given(projectService.getVisible(anna, 100L)).willReturn(relaunch);

        Task changed = service.changeStatus(anna, 500L, TaskStatus.DONE);

        assertThat(changed.getStatus()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void updateReplacesFieldsAndClearsAssignee() {
        Task task = new Task("Alt", "Alt", relaunch, leitung, LocalDate.of(2026, 9, 1));
        ReflectionTestUtils.setField(task, "id", 500L);
        given(tasks.findById(500L)).willReturn(Optional.of(task));
        given(projectService.getVisible(leitung, 100L)).willReturn(relaunch);

        Task updated = service.update(leitung, 500L, "Neu", null, null, null);

        assertThat(updated.getTitle()).isEqualTo("Neu");
        assertThat(updated.getDescription()).isNull();
        assertThat(updated.getAssignee()).isNull();
        assertThat(updated.getDueDate()).isNull();
    }

    @Test
    void unknownTaskIsNotFound() {
        given(tasks.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeStatus(anna, 999L, TaskStatus.DONE))
                .isInstanceOf(NotFoundException.class);
    }

    private static User user(long id, String username, Role role, Tenant tenant) {
        User user = new User(username, "hash", username, role, tenant);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
