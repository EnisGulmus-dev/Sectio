ALTER TABLE users
    DROP CONSTRAINT IF EXISTS chk_users_job_title;

UPDATE users
SET job_title = CASE job_title
    WHEN 'EMPLOYEE' THEN 'SOFTWARE_DEVELOPER'
    WHEN 'MANAGER' THEN 'PROJECT_MANAGER'
    ELSE job_title
END;

ALTER TABLE users
    ALTER COLUMN job_title SET DEFAULT 'SOFTWARE_DEVELOPER';

ALTER TABLE users
    ADD CONSTRAINT chk_users_job_title
        CHECK (job_title IN (
            'SOFTWARE_DEVELOPER',
            'SOFTWARE_ENGINEER',
            'SENIOR_SOFTWARE_ENGINEER',
            'TEAM_LEADER',
            'PROJECT_MANAGER',
            'PROJECT_OWNER'
        ));
