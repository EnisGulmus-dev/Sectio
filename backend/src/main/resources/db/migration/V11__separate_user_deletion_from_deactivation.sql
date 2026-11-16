-- Existing inactive accounts stay inactive: their earlier intent is not knowable.
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE users ADD CONSTRAINT chk_deleted_users_inactive
    CHECK (deleted_at IS NULL OR active = FALSE);
