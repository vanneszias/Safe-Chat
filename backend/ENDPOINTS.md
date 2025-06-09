# Safe Chat Backend API Endpoints

## Health Check

- **GET** `/health`
  - **Response:**
    - `200 OK` with body `OK`

---

## Authentication

### Register

- **POST** `/auth/register`
- **Request Body (JSON):**
  ```json
  {
    "username": "string",
    "password": "string"
  }
  ```
- **Response:**
  - `201 Created` with body:
    ```json
    {
      "id": "uuid-string",
      "public_key": "string",
      "token": "jwt_token"
    }
    ```
  - `409 Conflict` if username already exists
  - `500 Internal Server Error` for other errors

### Login

- **POST** `/auth/login`
- **Request Body (JSON):**
  ```json
  {
    "username": "string",
    "password": "string"
  }
  ```
- **Response:**
  - `200 OK` with body:
    ```json
    { "token": "<jwt_token>" }
    ```
  - `401 Unauthorized` if credentials are invalid
  - `500 Internal Server Error` for other errors

### Profile

- **GET** `/profile`
- **Headers:**
  - `Authorization: Bearer <jwt_token>`
- **Response:**
  - `200 OK` with body:
    ```json
    {
      "id": "uuid-string",
      "username": "string",
      "public_key": "string",
      "created_at": "string",
      "avatar": "base64-string (optional)"
    }
    ```
  - `401 Unauthorized` if token is missing or invalid
  - `404 Not Found` if user not found

### Update Profile

- `PUT /profile` — Update the user's profile (username and/or avatar)
  - Request body: `{ "username": "newname", "avatar": "<base64>" }` (both optional)
  - Requires Authorization header
  - Updates the username and/or avatar (binary, base64-encoded)

---

## User Lookup

### Get User by Public Key

- **GET** `/user/{public_key}`
- **Headers:**
  - `Authorization: Bearer <jwt_token>` (required)
- **Description:**
  - Returns user info for the given public key. Only accessible to authenticated users.
- **Response:**
  - `200 OK` with body:
    ```json
    {
      "id": "uuid-string",
      "username": "string",
      "public_key": "string",
      "created_at": "string",
      "avatar": "base64-string (optional)"
    }
    ```
  - `401 Unauthorized` if token is missing or invalid
  - `404 Not Found` if no user with that public key exists

---

## Notes

- All endpoints expect and return JSON unless otherwise noted.
- JWT tokens are returned on successful login and should be used for authenticated requests (future endpoints).
- Registration also generates a public key for the user, returned in the response message (not as JSON).
- The backend now returns the user UUID in the `/profile` endpoint and uses it as the JWT `sub` claim.
- There is **no endpoint to list all users or fetch by user id** for privacy and security reasons.

## /admin/dbdump
- Method: GET
- Returns: JSON dump of all users, contacts, and messages in the database.
- Auth: None (for demo/admin use only)

## /admin/dbtable.html (static)
- Method: GET
- Returns: Simple HTML page displaying the database contents in a table, fetched from /admin/dbdump.
- Auth: None (for demo/admin use only)

---

## WebSocket

### Real-time Messaging Connection

- **WS** `/ws?token={jwt_token}`
- **Query Parameters:**
  - `token`: JWT authentication token
- **Description:**
  - Establishes a WebSocket connection for real-time messaging
  - Automatically broadcasts new messages to recipients
  - Sends status updates when messages are read/delivered
  - Provides user online/offline notifications
- **Connection Events:**
  - **Connected**: Connection established successfully
  - **Disconnected**: Connection closed
  - **Error**: Connection or authentication error

### WebSocket Message Types

#### Incoming Messages (Server → Client)

- **new_message**: New message received
  ```json
  {
    "message_type": "new_message",
    "data": {
      "id": "uuid-string",
      "timestamp": "timestamp-string",
      "sender_id": "uuid-string",
      "receiver_id": "uuid-string",
      "status": "SENT",
      "type": "Text",
      "encrypted_content": "base64-string",
      "iv": "base64-string"
    }
  }
  ```

- **status_update**: Message status changed
  ```json
  {
    "message_type": "status_update",
    "data": {
      "message_id": "uuid-string",
      "status": "READ|DELIVERED|FAILED",
      "updated_by": "uuid-string"
    }
  }
  ```

- **user_online**: User came online
  ```json
  {
    "message_type": "user_online",
    "data": {
      "user_id": "uuid-string"
    }
  }
  ```

- **user_offline**: User went offline
  ```json
  {
    "message_type": "user_offline",
    "data": {
      "user_id": "uuid-string"
    }
  }
  ```

#### Outgoing Messages (Client → Server)

- **ping**: Keep connection alive
  ```json
  {
    "message_type": "ping",
    "data": {}
  }
  ```

- **mark_typing**: Indicate typing status
  ```json
  {
    "message_type": "mark_typing",
    "data": {
      "recipient_id": "uuid-string"
    }
  }
  ```

### WebSocket Authentication

- JWT token must be provided as a query parameter
- Invalid or missing tokens result in connection rejection with 401 Unauthorized
- Token validation occurs during connection establishment

### Connection Management

- Automatic reconnection handling on client side
- Heartbeat/ping messages to maintain connection
- Graceful disconnection on user logout
- Broadcast to all connected users for status updates
