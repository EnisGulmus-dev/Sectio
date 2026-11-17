CREATE TABLE task_handoff_history (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    source_task_id BIGINT REFERENCES tasks(id) ON DELETE SET NULL,
    target_task_id BIGINT REFERENCES tasks(id) ON DELETE SET NULL,
    actor_id BIGINT NOT NULL REFERENCES users(id),
    source_title VARCHAR(200) NOT NULL,
    target_title VARCHAR(200) NOT NULL,
    recipient_people TEXT NOT NULL,
    recipient_teams TEXT NOT NULL,
    handed_off_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_handoff_source ON task_handoff_history(source_task_id);
CREATE INDEX idx_handoff_target ON task_handoff_history(target_task_id);
