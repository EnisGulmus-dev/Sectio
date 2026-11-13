package com.enis.sectio.project;

import com.enis.sectio.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "projects")
public class Project {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "owner_id") private User owner;
    @Column(nullable = false, length = 150) private String name;
    @Column(length = 1000) private String description;
    @Column(name = "archived_at") private Instant archivedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected Project() {}
    public Project(User owner, String name, String description) { this.owner = owner; update(name, description); }
    @PrePersist void create() { var now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void updateTimestamp() { updatedAt = Instant.now(); }
    public void update(String name, String description) { this.name = name.trim(); this.description = description == null || description.isBlank() ? null : description.trim(); }
    public void archive() { archivedAt = Instant.now(); }
    public void restore() { archivedAt = null; }
    public Long getId() { return id; } public User getOwner() { return owner; } public String getName() { return name; }
    public String getDescription() { return description; } public Instant getArchivedAt() { return archivedAt; }
    public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
    public boolean isArchived() { return archivedAt != null; }
}
