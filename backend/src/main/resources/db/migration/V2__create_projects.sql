CREATE TABLE projects
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_id    BIGINT       NOT NULL,
    name        VARCHAR(150) NOT NULL,
    description VARCHAR(1000),
    archived_at TIMESTAMPTZ           DEFAULT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_projects_owner
        FOREIGN KEY (owner_id)
            REFERENCES users (id)
);
