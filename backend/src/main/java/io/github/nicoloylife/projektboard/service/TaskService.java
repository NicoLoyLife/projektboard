package io.github.nicoloylife.projektboard.service;

import io.github.nicoloylife.projektboard.domain.Project;
import io.github.nicoloylife.projektboard.domain.Role;
import io.github.nicoloylife.projektboard.domain.Task;
import io.github.nicoloylife.projektboard.domain.TaskStatus;
import io.github.nicoloylife.projektboard.domain.User;
import io.github.nicoloylife.projektboard.repository.TaskRepository;
import io.github.nicoloylife.projektboard.repository.UserRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aufgaben dürfen Leitung und Mitglieder eines aktiven Projekts anlegen und ändern.
 * Administratoren lesen nur. Zugewiesen werden kann nur an Leitung oder aktive Mitglieder.
 */
@Service
@Transactional
public class TaskService {

    private static final String TASK_NOT_FOUND = "Aufgabe nicht gefunden";
    private static final String NOT_ASSIGNABLE = "Nur Leitung und aktive Mitglieder des Projekts sind zuordenbar";

    private final TaskRepository tasks;
    private final UserRepository users;
    private final ProjectService projectService;

    public TaskService(TaskRepository tasks, UserRepository users, ProjectService projectService) {
        this.tasks = tasks;
        this.users = users;
        this.projectService = projectService;
    }

    public Task create(User current, Long projectId, String title, String description, Long assigneeId,
            LocalDate dueDate) {
        Project project = requireEditableProject(current, projectId);
        User assignee = resolveAssignee(project, assigneeId);
        return tasks.save(new Task(title, description, project, assignee, dueDate));
    }

    public Task update(User current, Long taskId, String title, String description, Long assigneeId,
            LocalDate dueDate) {
        Task task = requireEditableTask(current, taskId);
        task.setTitle(title);
        task.setDescription(description);
        if (!keepsAssignee(task, assigneeId)) {
            task.setAssignee(resolveAssignee(task.getProject(), assigneeId));
        }
        task.setDueDate(dueDate);
        return task;
    }

    public Task changeStatus(User current, Long taskId, TaskStatus status) {
        Task task = requireEditableTask(current, taskId);
        task.setStatus(status);
        return task;
    }

    /** Eine unveränderte Zuweisung bleibt bestehen, auch wenn die Person inzwischen deaktiviert ist. */
    private static boolean keepsAssignee(Task task, Long assigneeId) {
        return task.getAssignee() != null && task.getAssignee().getId().equals(assigneeId);
    }

    /** Ob das Projekt nur unsichtbar oder die Aufgabe unbekannt ist, verrät die Antwort nicht. */
    private Task requireEditableTask(User current, Long taskId) {
        Task task = tasks.findById(taskId).orElseThrow(() -> new NotFoundException(TASK_NOT_FOUND));
        try {
            requireEditableProject(current, task.getProject().getId());
        } catch (NotFoundException hiddenProject) {
            throw new NotFoundException(TASK_NOT_FOUND);
        }
        return task;
    }

    private Project requireEditableProject(User current, Long projectId) {
        Project project = projectService.getVisible(current, projectId);
        if (current.getRole() == Role.ADMIN) {
            throw new ForbiddenException("Administratoren bearbeiten keine Aufgaben");
        }
        if (project.isArchived()) {
            throw new ConflictException("Das Projekt ist archiviert");
        }
        return project;
    }

    private User resolveAssignee(Project project, Long assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        User assignee = users.findById(assigneeId)
                .orElseThrow(() -> new ValidationException("assigneeId", NOT_ASSIGNABLE));
        if (!assignee.isActive() || !project.isAccessibleBy(assignee)) {
            throw new ValidationException("assigneeId", NOT_ASSIGNABLE);
        }
        return assignee;
    }
}
