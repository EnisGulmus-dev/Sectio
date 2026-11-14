package com.enis.sectio.task;

import java.time.Instant;
import java.util.List;

public record TaskResponse(Long id, Long projectId, String projectName, Long createdById, String createdByName,
                           List<AssigneeResponse> assignees, String title, String description, TaskStatus status,
                           TaskPriority priority, Instant dueAt, Instant completedAt, Instant createdAt, Instant updatedAt) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(task.getId(), task.getProject().getId(), task.getProject().getName(),
                task.getCreatedBy().getId(), task.getCreatedBy().getName(),
                task.getAssignees().stream().map(AssigneeResponse::from).toList(),
                task.getTitle(), task.getDescription(), task.getStatus(), task.getPriority(), task.getDueAt(),
                task.getCompletedAt(), task.getCreatedAt(), task.getUpdatedAt());
    }
    public record AssigneeResponse(Long userId, String name) {
        static AssigneeResponse from(com.enis.sectio.user.User user) { return new AssigneeResponse(user.getId(), user.getName()); }
    }
}
