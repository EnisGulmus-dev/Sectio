package com.enis.sectio.task;

import java.util.List;

public enum TaskBucket {
    ACTIVE, COMPLETED, CANCELLED, ALL;

    public List<TaskStatus> statuses() {
        return switch (this) {
            case ACTIVE -> List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS);
            case COMPLETED -> List.of(TaskStatus.DONE);
            case CANCELLED -> List.of(TaskStatus.CANCELLED);
            case ALL -> List.of(TaskStatus.values());
        };
    }
}
