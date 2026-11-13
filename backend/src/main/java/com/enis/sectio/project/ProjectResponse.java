package com.enis.sectio.project;

import java.time.Instant;

public record ProjectResponse(Long id, Long ownerId, String ownerName, String name, String description,
                              boolean archived, Instant createdAt, Instant updatedAt) {
    public static ProjectResponse from(Project p) { return new ProjectResponse(p.getId(), p.getOwner().getId(), p.getOwner().getName(), p.getName(), p.getDescription(), p.isArchived(), p.getCreatedAt(), p.getUpdatedAt()); }
}
