package com.enis.sectio.project;

import com.enis.sectio.user.Role;
import com.enis.sectio.user.User;

public record ProjectMemberCandidateResponse(Long id, String name, String email, Role role) {
    static ProjectMemberCandidateResponse from(User user) {
        return new ProjectMemberCandidateResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
