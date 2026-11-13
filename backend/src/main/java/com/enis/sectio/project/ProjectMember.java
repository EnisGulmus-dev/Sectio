package com.enis.sectio.project;

import com.enis.sectio.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "project_members", uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "user_id"}))
public class ProjectMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id") private Project project;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @Column(name = "joined_at", nullable = false, updatable = false) private Instant joinedAt;
    protected ProjectMember() {}
    public ProjectMember(Project project, User user) { this.project = project; this.user = user; }
    @PrePersist void create() { if (joinedAt == null) joinedAt = Instant.now(); }
    public Long getId() { return id; } public Project getProject() { return project; } public User getUser() { return user; }
    public Instant getJoinedAt() { return joinedAt; }
}
