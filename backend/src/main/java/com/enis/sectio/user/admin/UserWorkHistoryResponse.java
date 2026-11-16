package com.enis.sectio.user.admin;

import com.enis.sectio.task.EndedTaskAssignment;
import java.time.Instant;

public record UserWorkHistoryResponse(Long id, Long taskId, Long projectId, String taskTitle,
                                      String projectName, String statusAtEnd, Instant endedAt, String reason) {
    static UserWorkHistoryResponse from(EndedTaskAssignment entry) {
        return new UserWorkHistoryResponse(entry.getId(), entry.getTaskId(), entry.getProjectId(),
                entry.getTaskTitle(), entry.getProjectName(), entry.getStatusAtEnd(), entry.getEndedAt(), entry.getReason());
    }
}
