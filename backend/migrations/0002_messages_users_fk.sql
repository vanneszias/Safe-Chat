-- Migration: Update messages table to reference users instead of contacts

ALTER TABLE messages
    DROP CONSTRAINT IF EXISTS messages_sender_id_fkey,
    DROP CONSTRAINT IF EXISTS messages_receiver_id_fkey;

ALTER TABLE messages
    ALTER COLUMN sender_id TYPE UUID USING sender_id::UUID,
    ALTER COLUMN receiver_id TYPE UUID USING receiver_id::UUID,
    ADD CONSTRAINT messages_sender_id_fkey FOREIGN KEY (sender_id) REFERENCES users(id),
    ADD CONSTRAINT messages_receiver_id_fkey FOREIGN KEY (receiver_id) REFERENCES users(id); 