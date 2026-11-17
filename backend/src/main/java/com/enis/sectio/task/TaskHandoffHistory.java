package com.enis.sectio.task;

import com.enis.sectio.project.Project;
import com.enis.sectio.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "task_handoff_history")
public class TaskHandoffHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id") private Project project;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "source_task_id") private Task source;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "target_task_id") private Task target;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "actor_id") private User actor;
    @Column(name = "source_title", nullable = false, length = 200) private String sourceTitle;
    @Column(name = "target_title", nullable = false, length = 200) private String targetTitle;
    @Column(name = "recipient_people", nullable = false, columnDefinition = "text") private String recipientPeople;
    @Column(name = "recipient_teams", nullable = false, columnDefinition = "text") private String recipientTeams;
    @Column(name = "handed_off_at", nullable = false) private Instant handedOffAt;
    protected TaskHandoffHistory() {}
    TaskHandoffHistory(Task source, Task target, User actor) {
        this.project = source.getProject(); this.source = source; this.target = target; this.actor = actor;
        this.sourceTitle = source.getTitle(); this.targetTitle = target.getTitle();
        this.recipientPeople = target.getAssignees().stream().map(User::getName).sorted().collect(java.util.stream.Collectors.joining(", "));
        this.recipientTeams = target.getTargetTeams().stream().map(com.enis.sectio.project.ProjectTeam::getName).sorted().collect(java.util.stream.Collectors.joining(", "));
        this.handedOffAt = Instant.now();
    }
    public Long getId() { return id; } public Task getSource() { return source; } public Task getTarget() { return target; }
    public User getActor() { return actor; } public String getSourceTitle() { return sourceTitle; } public String getTargetTitle() { return targetTitle; }
    public String getRecipientPeople() { return recipientPeople; } public String getRecipientTeams() { return recipientTeams; } public Instant getHandedOffAt() { return handedOffAt; }
}
