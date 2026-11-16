package com.enis.sectio.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobTitleTest {
    @Test
    void accessChangesRevokeSessionsAndActivationDoesNotRestoreThem() {
        User user = new User("Employee", "employee@example.com", "hash", Role.EMPLOYEE);
        user.changeRole(Role.ADMIN);
        assertThat(user.getSessionVersion()).isEqualTo(1);
        user.resetPasswordHash("new-hash");
        assertThat(user.getSessionVersion()).isEqualTo(2);
        user.deactivate();
        user.deactivate();
        user.activate();
        assertThat(user.getSessionVersion()).isEqualTo(3);
    }

    @Test
    void onlyTeamLeadershipAndProjectManagementCanCreateProjects() {
        assertThat(JobTitle.SOFTWARE_DEVELOPER.canCreateProjects()).isFalse();
        assertThat(JobTitle.SOFTWARE_ENGINEER.canCreateProjects()).isFalse();
        assertThat(JobTitle.SENIOR_SOFTWARE_ENGINEER.canCreateProjects()).isFalse();
        assertThat(JobTitle.TEAM_LEADER.canCreateProjects()).isTrue();
        assertThat(JobTitle.PROJECT_MANAGER.canCreateProjects()).isTrue();
        assertThat(JobTitle.PROJECT_OWNER.canCreateProjects()).isTrue();
        assertThat(JobTitle.CIO.canCreateProjects()).isFalse();
    }

    @Test
    void initialTitleDependsOnAccountRoleWithoutGrantingAuthorityFromTitle() {
        User admin = new User("Admin", "admin@example.com", "hash", Role.ADMIN);
        User employee = new User("Employee", "employee@example.com", "hash", Role.EMPLOYEE);

        assertThat(admin.getJobTitle()).isEqualTo(JobTitle.CIO);
        assertThat(employee.getJobTitle()).isEqualTo(JobTitle.SOFTWARE_DEVELOPER);
        employee.changeJobTitle(JobTitle.CIO);
        assertThat(employee.getRole()).isEqualTo(Role.EMPLOYEE);
    }
}
