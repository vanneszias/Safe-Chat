# Safe Chat Backend

## Project Overview

This is the backend service for the Safe Chat secure messaging application. It is written in Rust and is responsible for user authentication, secure message delivery, and end-to-end encryption support. The backend exposes a RESTful API and WebSocket endpoints to support real-time, secure communication between clients.

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

### Messages

- `GET /messages/{chatId}` — Retrieve messages for a chat session
- `POST /messages` — Send a new message
- `PUT /messages/{id}/status` — Update message delivery/read status

### WebSocket

- Real-time events for:
  - New message
  - Message status update

## Data Models

- **User**: id, username, password_hash, public_key, created_at
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

---

**This backend is designed to work seamlessly with the Safe Chat Android app, providing secure, real-time messaging.**
