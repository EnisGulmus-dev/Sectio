package com.enis.sectio.user.admin;

import com.enis.sectio.user.Role;
import com.enis.sectio.user.User;

import java.time.Instant;

public record AdminUserResponse(
        Long id,
        String name,
        String email,
        Role role,
        boolean active,
        boolean mustChangePassword,
        Instant createdAt,
        Instant updatedAt
) {
    static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.isMustChangePassword(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
