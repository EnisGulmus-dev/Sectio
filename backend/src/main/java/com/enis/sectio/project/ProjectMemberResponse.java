package com.enis.sectio.project;

import com.enis.sectio.user.Role;
import java.time.Instant;

public record ProjectMemberResponse(Long userId, String name, String email, Role role, Instant joinedAt) {
    static ProjectMemberResponse from(ProjectMember m) { var u = m.getUser(); return new ProjectMemberResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), m.getJoinedAt()); }
}
