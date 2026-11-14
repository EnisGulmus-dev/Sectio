package com.enis.sectio.task;

import com.enis.sectio.project.Project;
import com.enis.sectio.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "tasks")
public class Task {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id") private Project project;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by_id") private User createdBy;
    @ManyToMany
    @JoinTable(name = "task_assignees", joinColumns = @JoinColumn(name = "task_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    @OrderBy("name ASC")
    private Set<User> assignees = new LinkedHashSet<>();
    @Column(nullable = false, length = 200) private String title;
    @Column(length = 5000) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TaskStatus status;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TaskPriority priority;
    @Column(name = "due_at") private Instant dueAt;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Task() {}
    public Task(Project project, User creator, Set<User> assignees, String title, String description, TaskPriority priority, Instant dueAt) {
        this.project = project; this.createdBy = creator; this.status = TaskStatus.TODO;
        update(assignees, title, description, priority, dueAt);
    }
    @PrePersist void create() { var now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void touch() { updatedAt = Instant.now(); }
    public void update(Set<User> assignees, String title, String description, TaskPriority priority, Instant dueAt) {
        this.assignees.clear(); this.assignees.addAll(assignees); this.title = title.trim();
        this.description = description == null || description.isBlank() ? null : description.trim();
        this.priority = priority; this.dueAt = dueAt;
    }
    public void changeStatus(TaskStatus status) { this.status = status; completedAt = status == TaskStatus.DONE ? Instant.now() : null; }
    public Long getId() { return id; } public Project getProject() { return project; } public User getCreatedBy() { return createdBy; }
    public Set<User> getAssignees() { return assignees; } public String getTitle() { return title; } public String getDescription() { return description; }
    public TaskStatus getStatus() { return status; } public TaskPriority getPriority() { return priority; } public Instant getDueAt() { return dueAt; }
    public Instant getCompletedAt() { return completedAt; } public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
}
