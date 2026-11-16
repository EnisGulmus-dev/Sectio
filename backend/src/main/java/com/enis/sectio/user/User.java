package com.enis.sectio.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Entity
@org.hibernate.annotations.DynamicUpdate
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = false, length = 100)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    @JsonIgnore
    private String passwordHash;

    @Column(name = "role", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "job_title", length = 30, nullable = false)
    @Enumerated(EnumType.STRING)
    private JobTitle jobTitle;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "session_version", nullable = false)
    private long sessionVersion;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
    }

    public User(String name, String email, String passwordHash, Role role) {
        this.name = requireNonBlank(name, "name");
        this.email = normalizeEmail(email);
        this.passwordHash = requireNonBlank(passwordHash, "passwordHash");
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.jobTitle = role == Role.ADMIN ? JobTitle.CIO : JobTitle.SOFTWARE_DEVELOPER;
        this.active = true;
        this.mustChangePassword = true;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    //Getters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public JobTitle getJobTitle() {
        return jobTitle;
    }

    public boolean isActive() {
        return active;
    }

    public long getSessionVersion() {
        return sessionVersion;
    }

    public Instant getDeletedAt() { return deletedAt; }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    //Setters
    public void setName(String name) {
        this.name = requireNonBlank(name, "name");
    }

    public void setEmail(String email) {
        this.email = normalizeEmail(email);
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = requireNonBlank(passwordHash, "passwordHash");
        this.mustChangePassword = false;
    }

    public void resetPasswordHash(String passwordHash) {
        this.passwordHash = requireNonBlank(passwordHash, "passwordHash");
        this.mustChangePassword = true;
        this.sessionVersion++;
    }

    public void changeRole(Role role) {
        Role nextRole = Objects.requireNonNull(role, "role must not be null");
        if (this.role != nextRole) sessionVersion++;
        this.role = nextRole;
    }

    public void changeJobTitle(JobTitle jobTitle) {
        this.jobTitle = Objects.requireNonNull(jobTitle, "jobTitle must not be null");
    }

    public void activate() {
        if (deletedAt != null) throw new IllegalStateException("Deleted accounts cannot be activated");
        this.active = true;
    }

    public void deactivate() {
        // Persist revocation so reactivation cannot revive previously issued sessions.
        if (active) sessionVersion++;
        this.active = false;
    }

    public void deleteAccount() {
        if (deletedAt != null) return;
        deactivate();
        deletedAt = Instant.now();
    }

    private static String normalizeEmail(String email) {
        return requireNonBlank(email, "email").toLowerCase(Locale.ROOT);
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalizedValue = value.trim();
        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalizedValue;
    }
}
