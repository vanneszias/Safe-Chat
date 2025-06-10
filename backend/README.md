# Safe Chat Backend

## Project Overview

This is the backend service for the Safe Chat secure messaging application. It is written in Rust and provides user authentication, secure message delivery, and end-to-end encryption support. The backend exposes a RESTful API and WebSocket endpoints to support real-time, secure communication between clients.

## Architecture Overview

- **Language:** Rust
- **Framework:** Axum
- **Database:** PostgreSQL
- **Authentication:** JWT-based
- **Encryption:** X25519 for key exchange (client-side AES-GCM for message encryption)
- **WebSocket:** Real-time message and status updates
- **Password Hashing:** Argon2
- **Testing:** Unit, integration, and security tests

## Current Implementation Status

✅ **Completed Features:**
- User registration and authentication with Argon2 password hashing
- JWT-based session management
- User profile management with avatar support
- X25519 key pair generation and management
- Secure message sending and retrieval
- Real-time WebSocket communication
- Message status tracking (SENDING → SENT → READ)
- Bidirectional status updates (both sender and receiver notified)
- Automatic message deletion 5 seconds after being marked as read
- User lookup by public key
- Admin endpoints for demo/debugging purposes

## Security Features

- **Password Security:** Argon2 hashing for user passwords
- **Authentication:** JWT tokens with secure claims
- **Key Management:** X25519 public key storage and validation
- **Message Privacy:** Automatic deletion of read messages after 5 seconds
- **Input Validation:** Comprehensive validation and sanitization
- **Secure Headers:** X.509 encoding for X25519 public keys

## API Endpoints

### Authentication
- `POST /auth/register` — Register a new user with username/password
- `POST /auth/login` — Authenticate and receive JWT token
- `GET /profile` — Get current user profile
- `PUT /profile` — Update user profile (username/avatar)
- `PUT /profile/key` — Update user's public key

### User Management
- `GET /user/{public_key}` — Look up user by public key (authenticated)
- `GET /user/by-id/{user_id}` — Look up user by ID (authenticated)

### Messages
- `GET /messages/{user_id}` — Retrieve message history with specific user

### WebSocket
- `WS /ws?token={jwt_token}` — Real-time messaging and status updates

### Admin (Demo/Debug)
- `GET /admin/dbdump` — JSON dump of database contents
- `GET /admin/dbtable.html` — HTML table view of database

### Health Check
- `GET /health` — Health check endpoint

## WebSocket Events

### Incoming Events (Client → Server)
- **send_message**: Send encrypted message to recipient
- **update_status**: Update message status (READ/DELIVERED)
- **ping**: Keep connection alive

### Outgoing Events (Server → Client)
- **new_message**: Broadcast new message to recipient
- **status_update**: Notify status changes to both sender and receiver
- **user_online/offline**: User presence notifications

## Message Status Flow

1. **SENDING** → Message created locally on sender device
2. **SENT** → Message confirmed received by server (both parties notified)
3. **READ** → Message read by recipient (both parties notified)
4. **Auto-deletion** → Message deleted from server 5 seconds after READ status

This ensures both sender and receiver always know the current message status while maintaining privacy through automatic cleanup.

## Data Models

### User
```rust
{
    id: UUID,
    username: String,
    password_hash: String,
    public_key: String,
    avatar: Option<Vec<u8>>,
    created_at: DateTime
}
```

### Message
```rust
{
    id: UUID,
    sender_id: UUID,
    receiver_id: UUID,
    encrypted_content: String,
    iv: String,
    status: MessageStatus,
    timestamp: DateTime
}
```

## Security Implementation

### Cryptographic Operations
- **Key Generation:** X25519 elliptic curve keys with X.509 encoding
- **Password Hashing:** Argon2 with secure parameters
- **JWT Security:** HS256 signing with configurable secrets
- **Message Encryption:** Client-side AES-GCM (server stores encrypted content only)

### Privacy Protection
- Server never stores plaintext messages
- Automatic message deletion after read confirmation
- Public key-based user discovery (no user enumeration)
- Secure key validation and storage

## Environment Configuration

Required environment variables:
```bash
DATABASE_URL=postgresql://username:password@localhost/safechat
JWT_SECRET=your-secure-jwt-secret-key
SERVER_PORT=8080  # Optional, defaults to 8080
```

## Database Schema

The application uses PostgreSQL with the following main tables:
- `users` — User accounts and authentication data
- `messages` — Encrypted message storage with status tracking
- Automatic migrations handle schema setup

## Development Setup

1. **Install Dependencies:**
   ```bash
   cargo build
   ```

2. **Setup Database:**
   ```bash
   # Create PostgreSQL database
   createdb safechat
   
   # Set environment variables
   export DATABASE_URL="postgresql://username:password@localhost/safechat"
   export JWT_SECRET="your-secure-secret-key"
   ```

3. **Run Server:**
   ```bash
   cargo run
   ```

4. **Run Tests:**
   ```bash
   cargo test
   ```

## Docker Support

```bash
# Build and run with Docker Compose
docker-compose up --build

# Or build manually
docker build -t safechat-backend .
docker run -p 8080:8080 safechat-backend
```

## Testing

The backend includes comprehensive tests:
- **Unit Tests:** Individual function and module testing
- **Integration Tests:** API endpoint testing
- **WebSocket Tests:** Real-time communication testing
- **Security Tests:** Authentication and authorization validation

## Performance Considerations

- **Connection Pooling:** PostgreSQL connection pool (max 5 connections)
- **Async Operations:** Full async/await implementation with Tokio
- **Memory Management:** Efficient message handling and cleanup
- **WebSocket Optimization:** Connection management with DashMap for concurrent access

## Production Deployment

For production use:
1. Use strong JWT secrets and database credentials
2. Enable HTTPS/TLS termination at load balancer
3. Configure proper PostgreSQL connection limits
4. Set up monitoring and logging
5. Implement rate limiting and DDoS protection
6. Regular security updates and dependency audits

**Note:** This is a proof-of-concept implementation for educational purposes. Additional security hardening is required for production use.

---

**This backend works seamlessly with the Safe Chat Android app, providing secure, real-time messaging with end-to-end encryption support.**