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
      "created_at": "string"
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

## Notes

- All endpoints expect and return JSON unless otherwise noted.
- JWT tokens are returned on successful login and should be used for authenticated requests (future endpoints).
- Registration also generates a public key for the user, returned in the response message (not as JSON).
- The backend now returns the user UUID in the `/profile` endpoint and uses it as the JWT `sub` claim.
