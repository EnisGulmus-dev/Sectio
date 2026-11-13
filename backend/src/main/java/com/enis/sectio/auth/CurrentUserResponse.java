package com.enis.sectio.auth;

import com.enis.sectio.user.Role;
import com.enis.sectio.user.JobTitle;
import com.enis.sectio.user.User;

public record CurrentUserResponse(
        Long id,
        String name,
        String email,
        Role role,
        JobTitle jobTitle,
        boolean mustChangePassword
) {
    static CurrentUserResponse from(User user) {
        return new CurrentUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getJobTitle(),
                user.isMustChangePassword()
        );
    }
}
