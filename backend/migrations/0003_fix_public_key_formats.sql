-- Migration to fix public key formats from raw 32-byte to X.509-encoded format
-- This migration identifies users with raw 32-byte X25519 public keys and converts them to X.509 format

-- First, let's create a backup table
CREATE TABLE IF NOT EXISTS users_backup_keys AS 
SELECT id, username, public_key, created_at 
FROM users;

-- Update users with raw 32-byte keys to X.509 format
-- X.509 ASN.1 header for X25519 public keys: 30 2a 30 05 06 03 2b 65 6e 03 21 00
UPDATE users 
SET public_key = encode(
    decode('302a300506032b656e032100', 'hex') || decode(public_key, 'base64'),
    'base64'
)
WHERE length(decode(public_key, 'base64')) = 32;

-- Verify the migration worked - all keys should now be 44 bytes when decoded
DO $migration$
DECLARE
    invalid_count integer;
    total_users integer;
    updated_users integer;
BEGIN
    -- Count total users
    SELECT COUNT(*) INTO total_users FROM users;
    
    -- Count users with invalid key lengths
    SELECT COUNT(*) INTO invalid_count
    FROM users 
    WHERE length(decode(public_key, 'base64')) != 44;
    
    -- Count users that were updated (should have been 32-byte keys)
    SELECT COUNT(*) INTO updated_users
    FROM users_backup_keys b
    WHERE length(decode(b.public_key, 'base64')) = 32;
    
    IF invalid_count > 0 THEN
        RAISE EXCEPTION 'Migration failed: % out of % users still have invalid key lengths', invalid_count, total_users;
    END IF;
    
    RAISE NOTICE 'Migration successful: % users updated, all % users now have X.509-encoded public keys', updated_users, total_users;
END $migration$;

-- Add a comment to track this migration
COMMENT ON TABLE users IS 'Public keys migrated to X.509 format';