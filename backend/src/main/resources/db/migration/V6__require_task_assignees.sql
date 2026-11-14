DELETE FROM tasks WHERE assignee_id IS NULL;

ALTER TABLE tasks
    ALTER COLUMN assignee_id SET NOT NULL;
