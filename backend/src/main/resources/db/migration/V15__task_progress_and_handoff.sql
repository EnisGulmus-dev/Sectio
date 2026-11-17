CREATE TABLE task_progress_events (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    actor_id BIGINT NOT NULL REFERENCES users(id),
    old_status VARCHAR(20) NOT NULL,
    new_status VARCHAR(20) NOT NULL,
    note VARCHAR(2000),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_task_progress_task ON task_progress_events(task_id, occurred_at, id);
ALTER TABLE tasks ADD COLUMN predecessor_id BIGINT REFERENCES tasks(id) ON DELETE SET NULL;
ALTER TABLE tasks ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
CREATE INDEX idx_tasks_predecessor ON tasks(predecessor_id);
