package com.enis.sectio.project;

import com.enis.sectio.user.Role;
import com.enis.sectio.user.User;
import com.enis.sectio.user.JobTitle;

public record ProjectMemberCandidateResponse(Long id, String name, String email, Role role, JobTitle jobTitle) {
    static ProjectMemberCandidateResponse from(User user) {
        return new ProjectMemberCandidateResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getJobTitle());
    }
}
