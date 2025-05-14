-- Migration: Change users.id from SERIAL to UUID
-- 1. Add a new UUID column
ALTER TABLE users ADD COLUMN id_new UUID DEFAULT gen_random_uuid();
-- 2. Update new column with generated UUIDs
UPDATE users SET id_new = gen_random_uuid();
-- 3. Drop old PK constraint
ALTER TABLE users DROP CONSTRAINT users_pkey;
-- 4. Drop old id column
ALTER TABLE users DROP COLUMN id;
-- 5. Rename new column to id
ALTER TABLE users RENAME COLUMN id_new TO id;
-- 6. Set new PK
ALTER TABLE users ADD PRIMARY KEY (id); 