ALTER TABLE users DROP CONSTRAINT chk_users_job_title;

ALTER TABLE users
    ADD CONSTRAINT chk_users_job_title
        CHECK (job_title IN (
            'SOFTWARE_DEVELOPER',
            'SOFTWARE_ENGINEER',
            'SENIOR_SOFTWARE_ENGINEER',
            'TEAM_LEADER',
            'PROJECT_MANAGER',
            'PROJECT_OWNER',
            'CIO'
        ));

-- Preserve explicitly selected non-default titles and all employee titles.
UPDATE users
SET job_title = 'CIO', updated_at = CURRENT_TIMESTAMP
WHERE role = 'ADMIN' AND job_title = 'SOFTWARE_DEVELOPER';
