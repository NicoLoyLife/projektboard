package io.github.nicoloylife.projektboard.api;

import io.github.nicoloylife.projektboard.api.dto.TaskRequest;
import io.github.nicoloylife.projektboard.api.dto.TaskResponse;
import io.github.nicoloylife.projektboard.api.dto.TaskStatusRequest;
import io.github.nicoloylife.projektboard.domain.Task;
import io.github.nicoloylife.projektboard.security.CurrentUserService;
import io.github.nicoloylife.projektboard.service.TaskService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Aufgaben eines Projekts anlegen, ändern und im Status wechseln. */
@RestController
@RequestMapping("/api")
public class TaskController {

    private final TaskService taskService;
    private final CurrentUserService currentUserService;

    public TaskController(TaskService taskService, CurrentUserService currentUserService) {
        this.taskService = taskService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<TaskResponse> create(@PathVariable Long projectId, @Valid @RequestBody TaskRequest request) {
        Task created = taskService.create(currentUserService.require(), projectId, request.title(),
                request.description(), request.assigneeId(), request.dueDate());
        return ResponseEntity.created(URI.create("/api/tasks/" + created.getId())).body(TaskResponse.from(created));
    }

    @PutMapping("/tasks/{id}")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        Task updated = taskService.update(currentUserService.require(), id, request.title(), request.description(),
                request.assigneeId(), request.dueDate());
        return TaskResponse.from(updated);
    }

    @PatchMapping("/tasks/{id}/status")
    public TaskResponse changeStatus(@PathVariable Long id, @Valid @RequestBody TaskStatusRequest request) {
        return TaskResponse.from(taskService.changeStatus(currentUserService.require(), id, request.status()));
    }
}
