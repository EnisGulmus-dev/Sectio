package com.enis.sectio.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobTitleTest {
    @Test
    void onlyTeamLeadershipAndProjectManagementCanCreateProjects() {
        assertThat(JobTitle.SOFTWARE_DEVELOPER.canCreateProjects()).isFalse();
        assertThat(JobTitle.SOFTWARE_ENGINEER.canCreateProjects()).isFalse();
        assertThat(JobTitle.SENIOR_SOFTWARE_ENGINEER.canCreateProjects()).isFalse();
        assertThat(JobTitle.TEAM_LEADER.canCreateProjects()).isTrue();
        assertThat(JobTitle.PROJECT_MANAGER.canCreateProjects()).isTrue();
        assertThat(JobTitle.PROJECT_OWNER.canCreateProjects()).isTrue();
    }
}
