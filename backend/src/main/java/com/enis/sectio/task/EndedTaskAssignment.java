package com.enis.sectio.task;

import jakarta.persistence.*;
import java.time.Instant;

/** Snapshot of an assignment removed during account or project membership removal. */
@Entity
@Table(name = "task_assignment_history")
public class EndedTaskAssignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "task_id") private Long taskId;
    @Column(name = "project_id") private Long projectId;
    @Column(name = "task_title", nullable = false, length = 200) private String taskTitle;
    @Column(name = "project_name", nullable = false, length = 150) private String projectName;
    @Column(name = "status_at_end", nullable = false, length = 20) private String statusAtEnd;
    @Column(name = "ended_at", nullable = false) private Instant endedAt;
    @Column(nullable = false, length = 32) private String reason;

    protected EndedTaskAssignment() {}
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getTaskId() { return taskId; }
    public Long getProjectId() { return projectId; }
    public String getTaskTitle() { return taskTitle; }
    public String getProjectName() { return projectName; }
    public String getStatusAtEnd() { return statusAtEnd; }
    public Instant getEndedAt() { return endedAt; }
    public String getReason() { return reason; }
}
