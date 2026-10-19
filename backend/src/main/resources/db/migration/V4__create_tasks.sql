CREATE TABLE tasks
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id    BIGINT       NOT NULL,
    created_by_id BIGINT       NOT NULL,
    assignee_id   BIGINT,
    title         VARCHAR(200) NOT NULL,
    description   VARCHAR(5000),
    status        VARCHAR(20)  NOT NULL DEFAULT 'TODO'
        CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    priority      VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM'
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    due_at        TIMESTAMPTZ,
    completed_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_tasks_project
        FOREIGN KEY (project_id)
            REFERENCES projects (id),

    CONSTRAINT fk_tasks_created_by
        FOREIGN KEY (created_by_id)
            REFERENCES users (id),

    CONSTRAINT fk_tasks_assignee
        FOREIGN KEY (assignee_id)
            REFERENCES users (id),

    CONSTRAINT chk_tasks_completed_at
        CHECK (
            (status = 'DONE' AND completed_at IS NOT NULL)
                OR
            (status <> 'DONE' AND completed_at IS NULL)
            )
);
