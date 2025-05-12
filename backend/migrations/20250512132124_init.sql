-- users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    public_key TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- contacts table
CREATE TABLE contacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    contact_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    public_key TEXT NOT NULL,
    status TEXT NOT NULL,
    last_seen TIMESTAMPTZ
);

-- messages table (no FK on chat_id yet)
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id UUID NOT NULL,
    sender_id UUID NOT NULL REFERENCES users(id),
    receiver_id UUID NOT NULL REFERENCES users(id),
    encrypted_content BYTEA NOT NULL,
    iv BYTEA NOT NULL,
    type TEXT NOT NULL,
    status TEXT NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- chat_sessions table (no FK on last_message_id yet)
CREATE TABLE chat_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    participant_ids UUID[] NOT NULL,
    last_message_id UUID,
    unread_count INT NOT NULL DEFAULT 0,
    encryption_status TEXT NOT NULL
);

-- Add FKs after both tables exist
ALTER TABLE messages
    ADD CONSTRAINT fk_messages_chat_id FOREIGN KEY (chat_id) REFERENCES chat_sessions(id) ON DELETE CASCADE;
ALTER TABLE chat_sessions
    ADD CONSTRAINT fk_chat_sessions_last_message_id FOREIGN KEY (last_message_id) REFERENCES messages(id); 