# 📡 SafeChat - End-to-End Encrypted Messaging App

**SafeChat** is a proof-of-concept end-to-end encrypted (E2EE) messaging application, demonstrating secure communication principles using an Android frontend (Kotlin) and a Rust backend.

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

* **Frontend:** Android app written in Kotlin, using Room for local storage.
* **Backend:** Rust server using PostgreSQL.
* **Encryption:** X25519 for key exchange, AES-GCM for message encryption.
* **Authentication:** JWT tokens.
* **Key Storage:** Android Keystore for local private key security.

---

## ✨ Features

* Secure user registration and login with Argon2 password hashing.
* Public key-based contact management.
* Client-side end-to-end encryption and decryption.
* Local encrypted message storage using Room.
* Message status updates (sent/read).
* Server auto-deletes messages after they are marked as read.

---

## 🏗 Architecture

**Typical flow:**

1. Users register and generate X25519 key pairs.
2. Contacts are added via public key exchange.
3. Messages are encrypted on the sender’s device using the shared secret.
4. Recipients decrypt messages locally after retrieving them from the server.
5. Read messages are marked and removed from the backend.

---

## 🔐 Security Details

* **Key Exchange:** X25519 (Elliptic-curve Diffie-Hellman)
* **Encryption:** AES-GCM (with IV and authentication tag)
* **Password Hashing:** Argon2
* **Key Storage:** Android Keystore (for private keys and JWTs)
* **Message Lifecycle:** Messages are deleted from the server after being read.

---

## 🧬 Message Flow (Mermaid Chart)

The diagram below visualizes registration, authentication, contact management, and E2EE message flow.

<details>
<summary>Click to expand</summary>

```mermaid
sequenceDiagram
    participant U as User
    participant App as SafeChatApp (Android)
    participant Keystore as Android Keystore/Prefs
    participant Backend as SafeChatBackend (Rust)
    participant DB as PostgreSQL

    %% --- Registration & Initial Key Setup ---
    U->>App: Enter username/password and tap Register
    App->>Backend: POST /auth/register
    Backend->>Backend: Hash password (Argon2)
    Backend->>Backend: Generate server-side X25519 key
    Backend->>DB: INSERT user (hashed credentials, server public key)
    Backend-->>App: Return user ID, server key, JWT
    App->>Keystore: Store JWT and keys
    App->>Keystore: Generate client-side keypair
    App->>Backend: PUT /profile/key (client pubkey)
    Backend->>DB: UPDATE user's public key
    App-->>U: Show "Registration successful"

    %% --- Login ---
    U->>App: Enter credentials and tap Login
    App->>Backend: POST /auth/login
    Backend->>DB: SELECT user record
    Backend->>Backend: Verify password (Argon2)
    alt Valid credentials
        Backend-->>App: Return JWT
        App->>Backend: GET /profile
        Backend->>DB: SELECT user profile
        App->>Keystore: Store profile data
        App-->>U: Show contacts
    else Invalid credentials
        Backend-->>App: 401 Unauthorized
        App-->>U: Show error
    end

    %% --- Adding a Contact ---
    U->>App: Add contact's public key
    App->>Backend: GET /user/{public_key}
    Backend->>DB: SELECT user by pubkey
    alt Contact found
        Backend-->>App: Return contact profile
        App->>Keystore: Store contact locally
        App-->>U: Show new contact
    else Not found
        Backend-->>App: 404 Not Found
        App-->>U: Show error
    end

    %% --- Sending a Message ---
    U->>App: Send message
    App->>Keystore: Retrieve keys
    App->>App: Compute shared secret (X25519)
    App->>App: Encrypt using AES-GCM
    App->>Backend: POST /messages
    Backend->>DB: Store encrypted message
    Backend-->>App: Confirm with ID, status
    App->>Keystore: Store plaintext locally
    App-->>U: Update chat UI

    %% --- Receiving Messages ---
    U->>App: Open chat
    App->>App: Load local messages
    App->>Backend: GET /messages/{contact_id}
    Backend->>DB: SELECT relevant messages
    Backend-->>App: Return encrypted messages
    loop For each message
        App->>Keystore: Get keys
        App->>App: Decrypt with AES-GCM
        App->>Keystore: Store decrypted message
        App->>Backend: PUT /messages/{id}/status=READ
        Backend->>DB: Delete message
    end
```

</details>

---

## 📄 License

This project is released under the MIT License.
