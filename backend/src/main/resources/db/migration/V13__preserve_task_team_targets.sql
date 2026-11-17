CREATE TABLE task_teams (
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    team_id BIGINT NOT NULL REFERENCES project_teams(id) ON DELETE CASCADE,
    PRIMARY KEY (task_id, team_id)
);
CREATE INDEX idx_task_teams_team ON task_teams(team_id);
-- Previous team selections were expanded to people and cannot be safely inferred.
-- Existing task_assignees remain unchanged. A team may be targeted by many tasks.
