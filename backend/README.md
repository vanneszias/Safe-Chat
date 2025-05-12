# Safe Chat Backend

## Project Overview

This is the backend service for the Safe Chat secure messaging application. It is written in Rust and is responsible for user authentication, contact management, secure message delivery, and end-to-end encryption support. The backend exposes a RESTful API and WebSocket endpoints to support real-time, secure communication between clients.

## Architecture Overview

- **Language:** Rust
- **Framework:** [TBD: Actix-Web, Axum, or Rocket]
- **Database:** [TBD: PostgreSQL or SQLite]
- **Authentication:** JWT-based
- **Encryption:** Diffie-Hellman key exchange, AES-256 for message encryption
- **WebSocket:** Real-time message and status updates
- **Testing:** Unit, integration, and security tests

## Features

- User registration and authentication
- Contact management (add, remove, search)
- Secure message sending and retrieval
- End-to-end encryption key exchange
- Real-time updates via WebSocket
- Secure storage of user keys and messages
- Message delivery and read status tracking

## API Endpoints

### Authentication

- `POST /auth/register` — Register a new user
- `POST /auth/login` — Authenticate and receive JWT
- `POST /auth/refresh-token` — Refresh JWT

### Contacts

- `GET /contacts` — List all contacts for the authenticated user
- `POST /contacts` — Add a new contact
- `DELETE /contacts/{id}` — Remove a contact
- `PUT /contacts/{id}/key-exchange` — Update contact's public key for key exchange

### Messages

- `GET /messages/{chatId}` — Retrieve messages for a chat session
- `POST /messages` — Send a new message
- `PUT /messages/{id}/status` — Update message delivery/read status

### WebSocket

- Real-time events for:
  - New message
  - Message status update
  - Contact status update

## Data Models

- **User**: id, username, password_hash, public_key, created_at
- **Contact**: id, user_id, contact_user_id, public_key, status, last_seen
- **Message**: id, chat_id, sender_id, receiver_id, encrypted_content, iv, type, status, timestamp
- **ChatSession**: id, participant_ids, last_message_id, unread_count, encryption_status

## Security Requirements

- Passwords hashed with Argon2 or bcrypt
- JWT for authentication and session management
- No plaintext storage of sensitive data
- Secure key storage (server-side for public keys only)
- Diffie-Hellman for key exchange
- AES-256 for message encryption
- HMAC for message authentication
- Input validation and sanitization
- Regular security audits

## Implementation Roadmap

1. **Project Setup**

   - Choose web framework (Actix-Web, Axum, or Rocket)
   - Set up database schema and migrations
   - Configure JWT authentication

2. **Core Features**

   - User registration and login
   - Contact management endpoints
   - Message sending and retrieval endpoints
   - WebSocket server for real-time events

3. **Encryption**

   - Implement Diffie-Hellman key exchange logic
   - Integrate AES-256 encryption for messages
   - HMAC for message authentication

4. **Testing**

   - Unit tests for all modules
   - Integration tests for API endpoints
   - Security and penetration tests

5. **Deployment**
   - Dockerize the application
   - Set up CI/CD pipeline
   - Prepare production configuration

## Development Notes

- Follow Rust best practices and idiomatic code style
- Use environment variables for configuration
- Write comprehensive documentation and tests
- Ensure all cryptographic operations use secure, vetted libraries

## Database Schema (Initial)

```sql
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

-- chat_sessions table
CREATE TABLE chat_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    participant_ids UUID[] NOT NULL,
    last_message_id UUID REFERENCES messages(id),
    unread_count INT NOT NULL DEFAULT 0,
    encryption_status TEXT NOT NULL
);

-- messages table
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id UUID NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users(id),
    receiver_id UUID NOT NULL REFERENCES users(id),
    encrypted_content BYTEA NOT NULL,
    iv BYTEA NOT NULL,
    type TEXT NOT NULL,
    status TEXT NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

---

**This backend is designed to work seamlessly with the Safe Chat Android app, providing secure, real-time messaging.**
