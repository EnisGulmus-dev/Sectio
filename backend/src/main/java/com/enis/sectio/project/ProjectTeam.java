package com.enis.sectio.project;

import com.enis.sectio.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "project_teams", uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "name"}))
public class ProjectTeam {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id") private Project project;
    @Column(nullable = false, length = 100) private String name;
    @ManyToMany
    @JoinTable(name = "project_team_members",
            joinColumns = @JoinColumn(name = "team_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    @OrderBy("name ASC")
    private Set<User> members = new LinkedHashSet<>();
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected ProjectTeam() {}

    public ProjectTeam(Project project, String name, Set<User> members) {
        this.project = project;
        update(name, members);
    }

    @PrePersist void create() { if (createdAt == null) createdAt = Instant.now(); }

    public void update(String name, Set<User> members) {
        this.name = name.trim();
        this.members.clear();
        this.members.addAll(members);
    }

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public String getName() { return name; }
    public Set<User> getMembers() { return members; }
    public Instant getCreatedAt() { return createdAt; }
}
