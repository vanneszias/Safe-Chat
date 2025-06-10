# 📡 SafeChat - End-to-End Encrypted Messaging App

**SafeChat** is a proof-of-concept end-to-end encrypted (E2EE) messaging application, demonstrating secure communication principles using an Android frontend (Kotlin) and a Rust backend (Axum framework with PostgreSQL).

---

## ⚠️ Disclaimer

> **This is not production-ready software.**
>
> SafeChat is intended strictly for **educational and demonstration purposes only**. It may contain implementation flaws, unpatched vulnerabilities, or insecure design choices. **Do not use this application for real-world communication or to transmit sensitive information.**

---

## 📑 Table of Contents

* [Overview](#overview)
* [Features](#features)
* [Architecture](#architecture)
* [Security Details](#security-details)
* [Message Flow (Mermaid Chart)](#message-flow-mermaid-chart)
* [License](#license)

---

## 🧭 Overview

SafeChat illustrates the fundamentals of secure messaging with:

* **Frontend:** Android app written in Kotlin, using Room for local storage and Jetpack Compose UI.
* **Backend:** Rust server using Axum framework with PostgreSQL database.
* **Encryption:** X25519 for key exchange, AES-GCM for message encryption (client-side).
* **Authentication:** JWT tokens with Argon2 password hashing.
* **Key Storage:** Android Keystore for local private key security.
* **Real-time Communication:** WebSocket connections for instant messaging and status updates.

---

## ✨ Features

* Secure user registration and login with Argon2 password hashing.
* Public key-based contact management.
* Client-side end-to-end encryption and decryption.
* Local encrypted message storage using Room.
* Message status updates (SENDING → SENT → READ) with real-time synchronization.
* Bidirectional status notifications - both sender and receiver always know the current message status.
* Privacy protection - server auto-deletes messages 5 seconds after they are marked as read.
* WebSocket-based real-time communication for instant message delivery and status updates.

---

## 🏗 Architecture

**Complete message flow:**

1. **Registration:** Users register with Argon2-hashed passwords and generate X25519 key pairs.
2. **Contact Discovery:** Contacts are added via public key exchange (no user enumeration).
3. **Message Creation:** Messages are encrypted client-side using AES-GCM with shared secrets.
4. **Real-time Delivery:** WebSocket connections enable instant message delivery and status updates.
5. **Status Synchronization:** Message status progresses through SENDING → SENT → READ with bidirectional updates.
6. **Privacy Protection:** Read messages are automatically deleted from server after 5 seconds.
7. **Local Storage:** All messages are stored locally in encrypted Room database for offline access.

---

## 🔐 Security Details

* **Key Exchange:** X25519 (Elliptic-curve Diffie-Hellman) with X.509 encoding
* **Message Encryption:** AES-GCM (client-side, with IV and authentication tag)
* **Password Hashing:** Argon2 with secure parameters
* **Key Storage:** Android Keystore (for private keys and JWTs)
* **Communication:** WebSocket for real-time messaging and status updates
* **Message Lifecycle:** Messages auto-deleted from server 5 seconds after READ status
* **Database:** PostgreSQL for server storage, Room (SQLite) for local Android storage

---

## 🧬 Message Flow (Mermaid Chart)

The diagram below visualizes registration, authentication, contact management, and E2EE message flow.

<details>
<summary>Click to expand</summary>

```mermaid
sequenceDiagram
    participant U as User
    participant App as SafeChatApp (Android)
    participant Keystore as Android Keystore/EncryptedPrefs
    participant RoomDB as Room Database (Local)
    participant WS as WebSocket Connection
    participant Backend as SafeChatBackend (Rust/Axum)
    participant DB as PostgreSQL

    %% --- Registration & Key Generation ---
    Note over U,DB: User Registration with X25519 Key Generation
    U->>App: Enter username/password and tap Register
    App->>Keystore: Generate X25519 keypair locally
    App->>App: Encode public key with X.509 headers
    App->>Backend: POST /auth/register {username, password}
    Backend->>Backend: Hash password with Argon2
    Backend->>Backend: Generate server X25519 keypair
    Backend->>DB: INSERT user (id, username, password_hash, server_public_key)
    Backend-->>App: Return {id: UUID, public_key: X.509, token: JWT}
    App->>Keystore: Store JWT token securely
    App->>Backend: PUT /profile/key {public_key: X.509_encoded}
    Backend->>Backend: Validate X.509 public key format
    Backend->>DB: UPDATE users SET public_key = X.509_key WHERE id = user_id
    Backend-->>App: 200 OK
    App->>RoomDB: Store user profile locally
    App-->>U: Show "Registration successful"

    %% --- Login & WebSocket Connection ---
    Note over U,DB: Authentication & Real-time Connection Setup
    U->>App: Enter credentials and tap Login
    App->>Backend: POST /auth/login {username, password}
    Backend->>DB: SELECT user WHERE username = ?
    Backend->>Backend: Verify password with Argon2
    alt Valid credentials
        Backend-->>App: Return {token: JWT}
        App->>Keystore: Store JWT securely
        App->>Backend: GET /profile (with Bearer token)
        Backend->>Backend: Validate JWT and extract user_id
        Backend->>DB: SELECT user profile data
        Backend-->>App: Return user profile with public_key
        App->>RoomDB: Store profile in local database
        App->>WS: Establish WebSocket connection to /ws?token=JWT
        Backend->>Backend: Validate JWT from WebSocket query param
        Backend-->>WS: WebSocket connection established
        WS-->>App: Connected event
        App->>RoomDB: Load contacts and recent messages
        App-->>U: Show main chat interface
    else Invalid credentials
        Backend-->>App: 401 Unauthorized
        App-->>U: Show error message
    end

    %% --- Adding a Contact via Public Key ---
    Note over U,DB: Contact Discovery & Local Storage
    U->>App: Enter contact's public key
    App->>Backend: GET /user/{public_key} (with Bearer token)
    Backend->>Backend: Validate JWT authorization
    Backend->>DB: SELECT user WHERE public_key = X.509_key
    alt Contact found
        Backend-->>App: Return {id, username, public_key, created_at, avatar}
        App->>App: Compute shared secret (ECDH: local_private × contact_public)
        App->>Keystore: Store shared secret securely
        App->>RoomDB: INSERT contact locally
        App-->>U: Show contact added successfully
    else Contact not found
        Backend-->>App: 404 Not Found
        App-->>U: Show "Contact not found" error
    end

    %% --- Real-time Message Sending ---
    Note over U,DB: End-to-End Encrypted Messaging with Status Updates
    U->>App: Type message and tap Send
    App->>App: Generate UUID for message
    App->>App: Generate cryptographically secure random 12-byte IV
    App->>Keystore: Retrieve X25519 shared secret for recipient
    App->>App: Encrypt message with AES-GCM-256 (plaintext + shared_secret + IV)
    Note over App: Creates encrypted_content + authentication_tag
    App->>RoomDB: Store message locally (id, plaintext, encrypted_content, iv, status=SENDING)
    App-->>U: Show message with "Sending..." status indicator
    App->>WS: Send {message_type: "send_message", data: {id: UUID, receiver_id: UUID, encrypted_content: base64, iv: base64, type: "Text"}}
    WS->>Backend: Forward encrypted message data
    Backend->>Backend: Generate server timestamp
    Backend->>Backend: Validate receiver_id exists
    Backend->>DB: INSERT message (id, sender_id, receiver_id, encrypted_content, iv, status=SENT, timestamp)
    Note over Backend: Server stores only encrypted content, never plaintext
    Backend->>WS: Broadcast to sender {message_type: "status_update", data: {message_id: UUID, status: "SENT", updated_by: sender_id}}
    WS-->>App: Status update received by sender
    App->>RoomDB: UPDATE message SET status=SENT WHERE id=message_id
    App-->>U: Update UI to show "Sent ✓" status
    Backend->>WS: Broadcast to receiver {message_type: "new_message", data: {complete_message_object}}
    Note over Backend: Real-time delivery if receiver online, queued if offline

    %% --- Real-time Message Receiving ---
    Note over U,DB: Instant Message Delivery & Decryption
    WS-->>App: New message notification {id, sender_id, encrypted_content: base64, iv: base64, timestamp, status: "SENT"}
    App->>App: Decode base64 encrypted_content and IV
    App->>Keystore: Retrieve X25519 shared secret for sender_id
    App->>App: Decrypt with AES-GCM-256 (encrypted_content + shared_secret + IV)
    App->>App: Verify authentication tag for message integrity
    Note over App: Ensures message hasn't been tampered with
    App->>RoomDB: Store message (id, sender_id, plaintext_content, encrypted_content, iv, status=SENT, timestamp)
    App-->>U: Display decrypted message in chat interface with timestamp
    Note over App: Encryption/decryption happens entirely client-side
    
    %% --- Message Read Status & Privacy Protection ---
    Note over U,DB: Bidirectional Status Updates & Auto-deletion for Privacy
    U->>App: Open/view message thread (message becomes visible)
    App->>WS: Send {message_type: "update_status", data: {message_id: UUID, status: "READ"}}
    WS->>Backend: Forward read status update
    Backend->>Backend: Validate message ownership (user is sender or receiver)
    Backend->>DB: UPDATE messages SET status='READ' WHERE id=message_id
    Backend->>WS: Broadcast to sender {message_type: "status_update", data: {message_id: UUID, status: "READ", updated_by: reader_id}}
    Backend->>WS: Broadcast to receiver {message_type: "status_update", data: {message_id: UUID, status: "read", updated_by: reader_id}}
    WS-->>App: Status update received (both sender & receiver)
    App->>RoomDB: UPDATE messages SET status='read' WHERE id=message_id
    App-->>U: Update UI to show "Read ✓✓" status (blue checkmarks)
    Backend->>Backend: Schedule async deletion task with 5-second delay
    Note over Backend: Tokio::spawn background task for privacy protection
    Backend->>Backend: tokio::time::sleep(Duration::from_secs(5))
    Backend->>DB: DELETE FROM messages WHERE id=message_id
    Note over Backend: Permanent deletion ensures forward secrecy & privacy
    Note over Backend: Delayed deletion allows status updates to reach all parties

    %% --- Connection Management & Error Handling ---
    Note over U,DB: WebSocket Reliability & Reconnection with Security
    alt WebSocket disconnected (network/server issue)
        WS-->>App: Connection lost event detected
        App-->>U: Show "Connecting..." indicator
        App->>App: Start exponential backoff reconnection (1s, 2s, 4s, 8s, max 30s)
        loop Reconnection attempts
            App->>Keystore: Retrieve stored JWT token
            App->>WS: Reconnect to /ws?token=JWT
            Backend->>Backend: Validate JWT (check expiry, signature)
            alt JWT valid & reconnection successful
                WS-->>App: Connected event
                App-->>U: Show "Connected" indicator
                App->>Backend: GET /messages/{user_id} to sync missed messages
                Backend->>Backend: Authenticate with Bearer token
                Backend->>DB: SELECT messages WHERE (sender_id=user OR receiver_id=user) AND timestamp > last_sync
                Backend-->>App: Return encrypted message history
                App->>App: Process each encrypted message
                App->>RoomDB: Merge messages (avoid duplicates by UUID)
                App-->>U: Update chat interface with synchronized messages
                App->>App: Reset reconnection backoff timer
            else JWT expired or invalid
                App->>App: Clear stored credentials
                App-->>U: Redirect to login screen
            else Network still unavailable
                App->>App: Increase backoff delay (exponential)
                App-->>U: Show "Retrying connection..." with countdown
            end
        end
    end

    %% --- Offline Message Synchronization & Security Validation ---
    Note over U,DB: Secure Message Synchronization & Integrity Verification
    App->>Backend: GET /messages/{contact_id} (with Bearer: JWT header)
    Backend->>Backend: Validate JWT signature and extract user_id claims
    Backend->>Backend: Verify user_id has permission to access messages with contact_id
    Backend->>DB: SELECT id, sender_id, receiver_id, encrypted_content, iv, status, timestamp FROM messages WHERE (sender_id=user_id AND receiver_id=contact_id) OR (sender_id=contact_id AND receiver_id=user_id) ORDER BY timestamp DESC LIMIT 50
    Backend-->>App: Return JSON array of encrypted message objects
    loop For each encrypted message in response
        App->>App: Validate message structure and required fields
        App->>Keystore: Retrieve X25519 shared secret for contact_id
        App->>App: Attempt AES-GCM decryption with (encrypted_content + shared_secret + IV)
        alt Decryption successful
            App->>App: Verify message integrity with authentication tag
            App->>RoomDB: INSERT OR REPLACE message (prevent duplicates by UUID)
            App-->>U: Update chat interface with decrypted message
        else Decryption failed
            App->>App: Log decryption error (possible key mismatch)
            App-->>U: Show "Unable to decrypt message" placeholder
            Note over App: Handles forward secrecy and key rotation scenarios
        end
    end
    Note over App: Local Room database serves as single source of truth for UI
    Note over App: WebSocket events update Room DB, UI reactively updates via Flow
```

</details>

---

## 📄 License

This project is released under the MIT License.
