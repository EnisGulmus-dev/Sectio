ALTER TABLE users ADD COLUMN session_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE users ADD CONSTRAINT chk_users_session_version CHECK (session_version >= 0);
