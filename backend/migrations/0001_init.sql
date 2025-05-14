-- Enable pgcrypto for UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    public_key TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    avatar BYTEA
);

-- Contacts table
CREATE TABLE IF NOT EXISTS contacts (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    public_key TEXT NOT NULL,
    last_seen BIGINT NOT NULL,
    status TEXT NOT NULL,
    avatar_url TEXT
);

-- Messages table
CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY,
    content TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    sender_id UUID NOT NULL REFERENCES contacts(id),
    receiver_id UUID NOT NULL REFERENCES contacts(id),
    status TEXT NOT NULL,
    type TEXT NOT NULL,
    encrypted_content BYTEA NOT NULL,
    iv BYTEA NOT NULL
); 