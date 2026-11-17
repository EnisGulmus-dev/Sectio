package com.enis.sectio.task;

import com.enis.sectio.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "task_progress_events")
public class TaskProgressEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "task_id") private Task task;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "actor_id") private User actor;
    @Enumerated(EnumType.STRING) @Column(name = "old_status", nullable = false) private TaskStatus oldStatus;
    @Enumerated(EnumType.STRING) @Column(name = "new_status", nullable = false) private TaskStatus newStatus;
    @Column(length = 2000) private String note;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    protected TaskProgressEvent() {}
    TaskProgressEvent(Task task, User actor, TaskStatus oldStatus, TaskStatus newStatus, String note) {
        this.task = task; this.actor = actor; this.oldStatus = oldStatus; this.newStatus = newStatus;
        this.note = note == null || note.isBlank() ? null : note.trim(); this.occurredAt = Instant.now();
    }
    public Long getId() { return id; } public User getActor() { return actor; }
    public TaskStatus getOldStatus() { return oldStatus; } public TaskStatus getNewStatus() { return newStatus; }
    public String getNote() { return note; } public Instant getOccurredAt() { return occurredAt; }
}
