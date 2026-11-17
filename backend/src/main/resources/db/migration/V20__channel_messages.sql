CREATE TABLE channel_messages (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    channel_id BIGINT NOT NULL REFERENCES channels(id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES users(id),
    body VARCHAR(4000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    edited_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_channel_messages_history ON channel_messages(channel_id, id DESC);
