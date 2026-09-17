package io.github.nicoloylife.projektboard.api;

import io.github.nicoloylife.projektboard.api.dto.MembersRequest;
import io.github.nicoloylife.projektboard.api.dto.ProjectDetail;
import io.github.nicoloylife.projektboard.api.dto.ProjectRequest;
import io.github.nicoloylife.projektboard.api.dto.ProjectStatusRequest;
import io.github.nicoloylife.projektboard.api.dto.ProjectSummary;
import io.github.nicoloylife.projektboard.api.dto.UserSummary;
import io.github.nicoloylife.projektboard.domain.Project;
import io.github.nicoloylife.projektboard.domain.ProjectStatus;
import io.github.nicoloylife.projektboard.domain.User;
import io.github.nicoloylife.projektboard.security.CurrentUserService;
import io.github.nicoloylife.projektboard.service.ProjectService;
import io.github.nicoloylife.projektboard.service.TaskCounts;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Projekte: Übersicht mit Fortschritt, Detail mit Aufgaben, Anlegen und Pflege durch die Leitung. */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final CurrentUserService currentUserService;

    public ProjectController(ProjectService projectService, CurrentUserService currentUserService) {
        this.projectService = projectService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<ProjectSummary> list(@RequestParam(required = false) ProjectStatus status) {
        User current = currentUserService.require();
        List<Project> visible = projectService.listVisible(current).stream()
                .filter(project -> status == null || project.getStatus() == status)
                .toList();
        Map<Long, TaskCounts> counts = projectService.countsFor(visible);
        return visible.stream()
                .map(project -> ProjectSummary.from(project, counts.getOrDefault(project.getId(), TaskCounts.empty())))
                .toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    public ResponseEntity<ProjectDetail> create(@Valid @RequestBody ProjectRequest request) {
        User current = currentUserService.require();
        Project created = projectService.create(current, request.name(), request.description());
        return ResponseEntity.created(URI.create("/api/projects/" + created.getId()))
                .body(detail(current, created));
    }

    @GetMapping("/{id}")
    public ProjectDetail get(@PathVariable Long id) {
        User current = currentUserService.require();
        return detail(current, projectService.getVisible(current, id));
    }

    @PutMapping("/{id}")
    public ProjectDetail update(@PathVariable Long id, @Valid @RequestBody ProjectRequest request) {
        User current = currentUserService.require();
        return detail(current, projectService.update(current, id, request.name(), request.description()));
    }

    @PatchMapping("/{id}/status")
    public ProjectDetail changeStatus(@PathVariable Long id, @Valid @RequestBody ProjectStatusRequest request) {
        User current = currentUserService.require();
        return detail(current, projectService.changeStatus(current, id, request.status()));
    }

    @PutMapping("/{id}/members")
    public ProjectDetail setMembers(@PathVariable Long id, @Valid @RequestBody MembersRequest request) {
        User current = currentUserService.require();
        return detail(current, projectService.setMembers(current, id, request.userIds()));
    }

    @GetMapping("/{id}/member-candidates")
    public List<UserSummary> memberCandidates(@PathVariable Long id) {
        User current = currentUserService.require();
        return projectService.memberCandidates(current, id).stream().map(UserSummary::from).toList();
    }

    private ProjectDetail detail(User current, Project project) {
        TaskCounts counts = projectService.countsFor(List.of(project))
                .getOrDefault(project.getId(), TaskCounts.empty());
        return ProjectDetail.from(project, counts, projectService.tasksOf(project),
                project.isManagedBy(current), projectService.canEditTasks(current, project));
    }
}
