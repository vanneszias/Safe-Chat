-- Migration: Remove the 'content' column from the 'messages' table

-- First, let's backup the existing messages table just in case
CREATE TABLE IF NOT EXISTS messages_backup_content AS
SELECT * FROM messages;

-- Remove the 'content' column from the 'messages' table
ALTER TABLE messages
DROP COLUMN IF EXISTS content;

-- Add a comment to track this migration
COMMENT ON TABLE messages IS 'Removed plaintext content column to enforce E2EE. Encrypted content is in encrypted_content.';

-- Verify the column is removed
DO $$
DECLARE
    col_exists boolean;
BEGIN
    SELECT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'messages' AND column_name = 'content'
    ) INTO col_exists;

    IF col_exists THEN
        RAISE EXCEPTION 'Migration failed: content column still exists in messages table.';
    ELSE
        RAISE NOTICE 'Migration successful: content column removed from messages table.';
    END IF;
END $$;