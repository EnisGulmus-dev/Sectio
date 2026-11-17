package com.enis.sectio.task;

import com.enis.sectio.common.web.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
public class TaskController {
    private final TaskService service;

    public TaskController(TaskService service) { this.service = service; }

    @GetMapping("/api/v1/projects/{projectId}/tasks")
    PageResponse<TaskResponse> list(@PathVariable Long projectId, Authentication authentication,
                                    @RequestParam(defaultValue = "") String query,
                                    @RequestParam(required = false) TaskStatus status,
                                    @RequestParam(required = false) TaskPriority priority,
                                    @RequestParam(required = false) Long assigneeId,
                                    @RequestParam(defaultValue = "false") boolean unassigned,
                                    @RequestParam(required = false) TaskBucket bucket,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return service.listProject(projectId, authentication.getName(), query, status, priority, assigneeId, unassigned,
                effectiveBucket(bucket, status), page, size);
    }

    @PostMapping("/api/v1/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    TaskResponse create(@PathVariable Long projectId, @Valid @RequestBody TaskInput request, Authentication authentication) {
        return service.create(projectId, authentication.getName(), request.assigneeIds(), request.teamIds(),
                request.allMembers(), request.title(), request.description(), request.priority(), request.dueAt());
    }

    @GetMapping("/api/v1/tasks/my")
    PageResponse<TaskResponse> mine(Authentication authentication,
                                    @RequestParam(defaultValue = "") String query,
                                    @RequestParam(required = false) TaskStatus status,
                                    @RequestParam(required = false) TaskPriority priority,
                                    @RequestParam(required = false) TaskBucket bucket,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return service.mine(authentication.getName(), query, status, priority, effectiveBucket(bucket, status), page, size);
    }

    private static TaskBucket effectiveBucket(TaskBucket bucket, TaskStatus status) {
        return bucket != null ? bucket : status != null ? TaskBucket.ALL : TaskBucket.ACTIVE;
    }

    @GetMapping("/api/v1/tasks/{id}")
    TaskResponse get(@PathVariable Long id, Authentication authentication) { return service.get(id, authentication.getName()); }

    @PatchMapping("/api/v1/tasks/{id}")
    TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskInput request, Authentication authentication) {
        return service.update(id, authentication.getName(), request.assigneeIds(), request.teamIds(),
                request.allMembers(), request.title(), request.description(), request.priority(), request.dueAt());
    }

    @PatchMapping("/api/v1/tasks/{id}/status")
    TaskResponse status(@PathVariable Long id, @Valid @RequestBody StatusInput request, Authentication authentication) {
        return service.status(id, authentication.getName(), request.status(), request.note(), request.expectedStatus());
    }

    @GetMapping("/api/v1/tasks/{id}/progress")
    List<TaskService.ProgressResponse> progress(@PathVariable Long id, Authentication authentication) { return service.progress(id, authentication.getName()); }

    @GetMapping("/api/v1/tasks/{id}/handoff-history")
    List<TaskService.HandoffResponse> handoffHistory(@PathVariable Long id, Authentication authentication) { return service.handoffHistory(id, authentication.getName()); }

    @GetMapping("/api/v1/tasks/{id}/successors")
    List<TaskResponse> successors(@PathVariable Long id, Authentication authentication) { return service.successors(id, authentication.getName()); }

    @PostMapping("/api/v1/tasks/{id}/handoff") @ResponseStatus(HttpStatus.CREATED)
    TaskResponse handoff(@PathVariable Long id, @Valid @RequestBody TaskInput request, Authentication authentication) {
        return service.handoff(id, authentication.getName(), request.assigneeIds(), request.teamIds(), request.allMembers(), request.title(), request.description(), request.priority(), request.dueAt());
    }

    @DeleteMapping("/api/v1/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id, Authentication authentication) { service.delete(id, authentication.getName()); }

    public record TaskInput(@Size(max = 50) List<@Positive Long> assigneeIds,
                            @Size(max = 20) List<@Positive Long> teamIds,
                            boolean allMembers,
                            @NotBlank @Size(max = 200) String title,
                            @Size(max = 5000) String description,
                            @NotNull TaskPriority priority, Instant dueAt) {}
    public record StatusInput(@NotNull TaskStatus status, @Size(max = 2000) String note, TaskStatus expectedStatus) {}
}
