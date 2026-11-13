package com.enis.sectio.project;

import com.enis.sectio.user.Role;
import com.enis.sectio.user.JobTitle;
import java.time.Instant;

public record ProjectMemberResponse(Long userId, String name, String email, Role role, JobTitle jobTitle, Instant joinedAt) {
    static ProjectMemberResponse from(ProjectMember m) { var u = m.getUser(); return new ProjectMemberResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getJobTitle(), m.getJoinedAt()); }
}
