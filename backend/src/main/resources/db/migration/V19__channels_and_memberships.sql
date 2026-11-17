CREATE TABLE channels (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    scope VARCHAR(30) NOT NULL DEFAULT 'CORPORATE' CHECK (scope = 'CORPORATE'),
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    visibility VARCHAR(10) NOT NULL CHECK (visibility IN ('PUBLIC', 'PRIVATE')),
    owner_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX idx_channels_scope_name ON channels(scope, lower(name));
CREATE TABLE channel_members (
    channel_id BIGINT NOT NULL REFERENCES channels(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(channel_id, user_id)
);
CREATE INDEX idx_channel_members_user ON channel_members(user_id, channel_id);
