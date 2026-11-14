package com.enis.sectio.user;

public enum JobTitle {
    SOFTWARE_DEVELOPER(false),
    SOFTWARE_ENGINEER(false),
    SENIOR_SOFTWARE_ENGINEER(false),
    TEAM_LEADER(true),
    PROJECT_MANAGER(true),
    PROJECT_OWNER(true);

    private final boolean projectCreationAllowed;

    JobTitle(boolean projectCreationAllowed) {
        this.projectCreationAllowed = projectCreationAllowed;
    }

    public boolean canCreateProjects() {
        return projectCreationAllowed;
    }
}
