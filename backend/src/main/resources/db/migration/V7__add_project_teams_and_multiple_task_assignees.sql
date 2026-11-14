CREATE TABLE project_teams
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id BIGINT       NOT NULL,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_project_teams_project
        FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT uk_project_teams_project_name UNIQUE (project_id, name)
);

CREATE TABLE project_team_members
(
    team_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,

    PRIMARY KEY (team_id, user_id),
    CONSTRAINT fk_project_team_members_team
        FOREIGN KEY (team_id) REFERENCES project_teams (id) ON DELETE CASCADE,
    CONSTRAINT fk_project_team_members_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE task_assignees
(
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,

    PRIMARY KEY (task_id, user_id),
    CONSTRAINT fk_task_assignees_task
        FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE CASCADE,
    CONSTRAINT fk_task_assignees_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

INSERT INTO task_assignees (task_id, user_id)
SELECT id, assignee_id
FROM tasks;

ALTER TABLE tasks DROP CONSTRAINT fk_tasks_assignee;
ALTER TABLE tasks DROP COLUMN assignee_id;

CREATE INDEX idx_task_assignees_user ON task_assignees (user_id);
CREATE INDEX idx_project_team_members_user ON project_team_members (user_id);
