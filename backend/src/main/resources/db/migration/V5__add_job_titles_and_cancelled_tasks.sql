ALTER TABLE users
    ADD COLUMN job_title VARCHAR(30) NOT NULL DEFAULT 'EMPLOYEE';

ALTER TABLE users
    ADD CONSTRAINT chk_users_job_title
        CHECK (job_title IN ('EMPLOYEE', 'TEAM_LEADER', 'MANAGER'));

ALTER TABLE tasks
    DROP CONSTRAINT IF EXISTS tasks_status_check;

ALTER TABLE tasks
    ADD CONSTRAINT chk_tasks_status
        CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE', 'CANCELLED'));
