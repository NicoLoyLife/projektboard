package io.github.nicoloylife.projektboard.api.dto;

import io.github.nicoloylife.projektboard.domain.Project;
import io.github.nicoloylife.projektboard.domain.ProjectStatus;
import io.github.nicoloylife.projektboard.service.TaskCounts;
import java.time.LocalDateTime;

/** Zeile der Projektübersicht mit berechnetem Fortschritt. */
public record ProjectSummary(Long id, String name, ProjectStatus status, UserSummary manager, TaskCounts counts,
        int progressPercent, LocalDateTime createdAt) {

    public static ProjectSummary from(Project project, TaskCounts counts) {
        return new ProjectSummary(project.getId(), project.getName(), project.getStatus(),
                UserSummary.from(project.getManager()), counts, counts.progressPercent(), project.getCreatedAt());
    }
}
