package io.github.nicoloylife.projektboard.api.dto;

import io.github.nicoloylife.projektboard.domain.Project;
import io.github.nicoloylife.projektboard.domain.ProjectStatus;
import io.github.nicoloylife.projektboard.domain.Task;
import io.github.nicoloylife.projektboard.service.TaskCounts;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Projektdetail mit Mitgliedern und Aufgaben. Die Flags sagen dem Frontend, welche Schaltflächen
 * sinnvoll sind, verbindlich prüft weiterhin das Backend.
 */
public record ProjectDetail(Long id, String name, String description, ProjectStatus status, UserSummary manager,
        List<UserSummary> members, TaskCounts counts, int progressPercent, List<TaskResponse> tasks,
        LocalDateTime createdAt, boolean canManage, boolean canEditTasks) {

    public static ProjectDetail from(Project project, TaskCounts counts, List<Task> tasks, boolean canManage,
            boolean canEditTasks) {
        List<UserSummary> members = project.getMembers().stream()
                .map(UserSummary::from)
                .sorted(Comparator.comparing(UserSummary::displayName))
                .toList();
        return new ProjectDetail(project.getId(), project.getName(), project.getDescription(), project.getStatus(),
                UserSummary.from(project.getManager()), members, counts, counts.progressPercent(),
                tasks.stream().map(TaskResponse::from).toList(), project.getCreatedAt(), canManage, canEditTasks);
    }
}
