package io.github.nicoloylife.projektboard.service;

import io.github.nicoloylife.projektboard.domain.Project;
import io.github.nicoloylife.projektboard.domain.ProjectStatus;
import io.github.nicoloylife.projektboard.domain.Role;
import io.github.nicoloylife.projektboard.domain.Task;
import io.github.nicoloylife.projektboard.domain.User;
import io.github.nicoloylife.projektboard.repository.ProjectRepository;
import io.github.nicoloylife.projektboard.repository.TaskRepository;
import io.github.nicoloylife.projektboard.repository.UserRepository;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fachliche Regeln für Projekte. Sichtbar ist ein Projekt für Leitung und Mitglieder sowie lesend für
 * Administratoren. Ändern darf nur die Leitung, archivierte Projekte sind schreibgeschützt.
 */
@Service
@Transactional
public class ProjectService {

    static final List<Role> MEMBER_ROLES = List.of(Role.PROJECT_MANAGER, Role.EMPLOYEE);

    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final UserRepository users;

    public ProjectService(ProjectRepository projects, TaskRepository tasks, UserRepository users) {
        this.projects = projects;
        this.tasks = tasks;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<Project> listVisible(User current) {
        Long tenantId = current.getTenant().getId();
        if (current.getRole() == Role.ADMIN) {
            return projects.findByTenantIdOrderByNameAsc(tenantId);
        }
        return projects.findVisibleFor(tenantId, current.getId());
    }

    @Transactional(readOnly = true)
    public Map<Long, TaskCounts> countsFor(Collection<Project> projectList) {
        List<Long> ids = projectList.stream().map(Project::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return TaskCounts.byProject(tasks.countByProjectIds(ids));
    }

    /** Liefert das Projekt, wenn der Benutzer es sehen darf, sonst 404, damit die Existenz nicht verraten wird. */
    @Transactional(readOnly = true)
    public Project getVisible(User current, Long projectId) {
        Project project = projects.findById(projectId)
                .filter(candidate -> candidate.getTenant().getId().equals(current.getTenant().getId()))
                .orElseThrow(() -> new NotFoundException("Projekt nicht gefunden"));
        if (current.getRole() != Role.ADMIN && !project.isAccessibleBy(current)) {
            throw new NotFoundException("Projekt nicht gefunden");
        }
        project.getMembers().size();
        return project;
    }

    @Transactional(readOnly = true)
    public List<Task> tasksOf(Project project) {
        return tasks.findByProjectIdOrderByCreatedAtAscIdAsc(project.getId());
    }

    public Project create(User current, String name, String description) {
        Project project = new Project(name, description, current, current.getTenant());
        return projects.save(project);
    }

    public Project update(User current, Long projectId, String name, String description) {
        Project project = requireManagedAndActive(current, projectId);
        project.setName(name);
        project.setDescription(description);
        return project;
    }

    public Project changeStatus(User current, Long projectId, ProjectStatus status) {
        Project project = requireManaged(current, projectId);
        project.setStatus(status);
        return project;
    }

    public Project setMembers(User current, Long projectId, List<Long> userIds) {
        Project project = requireManagedAndActive(current, projectId);
        Set<Long> wanted = new HashSet<>(userIds);
        wanted.remove(project.getManager().getId());
        List<User> resolved = users.findAllById(wanted).stream()
                .filter(user -> isAssignable(project, user))
                .toList();
        if (resolved.size() != wanted.size()) {
            throw new ValidationException("userIds", "Mindestens ein Benutzer ist nicht zuordenbar");
        }
        project.getMembers().clear();
        project.getMembers().addAll(resolved);
        return project;
    }

    /** Aktive Benutzer des Mandanten mit passender Rolle, ohne die Leitung selbst. */
    @Transactional(readOnly = true)
    public List<User> memberCandidates(User current, Long projectId) {
        Project project = requireManaged(current, projectId);
        return users.findByTenantIdAndActiveTrueAndRoleInOrderByDisplayNameAsc(project.getTenant().getId(), MEMBER_ROLES)
                .stream()
                .filter(user -> !user.getId().equals(project.getManager().getId()))
                .toList();
    }

    public boolean canEditTasks(User current, Project project) {
        return !project.isArchived() && current.getRole() != Role.ADMIN && project.isAccessibleBy(current);
    }

    static boolean isAssignable(Project project, User user) {
        return user.isActive()
                && MEMBER_ROLES.contains(user.getRole())
                && user.getTenant().getId().equals(project.getTenant().getId());
    }

    private Project requireManaged(User current, Long projectId) {
        Project project = getVisible(current, projectId);
        if (!project.isManagedBy(current)) {
            throw new ForbiddenException("Nur die Projektleitung darf das Projekt ändern");
        }
        return project;
    }

    private Project requireManagedAndActive(User current, Long projectId) {
        Project project = requireManaged(current, projectId);
        if (project.isArchived()) {
            throw new ConflictException("Das Projekt ist archiviert");
        }
        return project;
    }
}
