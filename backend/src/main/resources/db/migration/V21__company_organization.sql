CREATE TABLE company_teams (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX idx_company_teams_name ON company_teams (lower(name));
CREATE TABLE company_team_members (
    team_id BIGINT NOT NULL REFERENCES company_teams(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    PRIMARY KEY (team_id, user_id)
);
CREATE INDEX idx_company_team_members_user ON company_team_members(user_id);
CREATE TABLE company_reporting_lines (
    user_id BIGINT PRIMARY KEY REFERENCES users(id),
    manager_id BIGINT NOT NULL REFERENCES users(id),
    CHECK (user_id <> manager_id)
);
CREATE INDEX idx_company_reporting_manager ON company_reporting_lines(manager_id);
