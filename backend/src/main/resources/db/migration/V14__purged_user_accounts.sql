ALTER TABLE users ADD COLUMN purged_at TIMESTAMPTZ;
ALTER TABLE users ADD CONSTRAINT ck_users_purged_deleted
    CHECK (purged_at IS NULL OR (deleted_at IS NOT NULL AND active = FALSE));
CREATE INDEX idx_users_deleted_visible ON users(name)
    WHERE deleted_at IS NOT NULL AND purged_at IS NULL;
