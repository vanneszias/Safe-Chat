#!/bin/sh
set -e

# Ensure migrations directory exists
if [ ! -d ./migrations ]; then
  mkdir ./migrations
fi

# If no migration files exist, create initial migrations for users, messages, and contacts
if [ -z "$(ls -A ./migrations/*.sql 2>/dev/null)" ]; then
  cat > ./migrations/0001_init.sql <<EOF
-- Enable pgcrypto for UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
-- Users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    public_key TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
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
EOF
fi

# Wait for the database to be ready
until pg_isready -h db -p 5432; do
  sleep 1
done

# Run migrations
db_url=${DATABASE_URL:-postgres://postgres:password@db:5432/postgres}
DATABASE_URL=$db_url sqlx migrate run --source ./migrations

# Start the backend
exec /app/backend 