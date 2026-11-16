CREATE TABLE task_assignment_history (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    task_id BIGINT REFERENCES tasks(id) ON DELETE SET NULL,
    project_id BIGINT REFERENCES projects(id) ON DELETE SET NULL,
    task_title VARCHAR(200) NOT NULL,
    project_name VARCHAR(150) NOT NULL,
    status_at_end VARCHAR(20) NOT NULL,
    ended_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason VARCHAR(32) NOT NULL CHECK (reason IN ('USER_DELETED', 'PROJECT_MEMBER_REMOVED'))
);
CREATE INDEX idx_task_assignment_history_user
    ON task_assignment_history(user_id, ended_at DESC, id DESC);
