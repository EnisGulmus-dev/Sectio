ALTER TABLE channel_messages ADD COLUMN event_type VARCHAR(24);
ALTER TABLE channel_messages ADD COLUMN subject_user_id BIGINT REFERENCES users(id);
ALTER TABLE channel_messages ADD CONSTRAINT ck_channel_message_event CHECK (
    (event_type IS NULL AND subject_user_id IS NULL)
    OR (event_type IN ('MEMBER_LEFT', 'MEMBER_REMOVED') AND subject_user_id IS NOT NULL)
);
