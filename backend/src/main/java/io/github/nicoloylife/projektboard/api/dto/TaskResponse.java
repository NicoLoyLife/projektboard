package io.github.nicoloylife.projektboard.api.dto;

import io.github.nicoloylife.projektboard.domain.Task;
import io.github.nicoloylife.projektboard.domain.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResponse(Long id, String title, String description, TaskStatus status, UserSummary assignee,
        LocalDate dueDate, LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(task.getId(), task.getTitle(), task.getDescription(), task.getStatus(),
                UserSummary.from(task.getAssignee()), task.getDueDate(), task.getCreatedAt(), task.getUpdatedAt());
    }
}
