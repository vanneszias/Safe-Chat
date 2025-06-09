//! API module for Safe Chat backend
//!
//! This module handles all API endpoints and implements proper timezone handling
//! for the Europe/Brussels timezone. All timestamps are:
//! - Stored as Unix timestamps (BIGINT) in the database
//! - Generated in Brussels timezone when creating new messages
//! - Converted to Brussels timezone when returning data to clients
//! - The created_at fields remain static as stored in the database

use crate::state::AppState;

use axum::extract::{Json, Path, State};
use axum::http::HeaderMap;
use axum::http::StatusCode;
use axum::http::header::AUTHORIZATION;
use axum::response::IntoResponse;
use base64;
use base64::Engine;
use base64::engine::general_purpose;
use chrono::{DateTime, Utc};
use chrono_tz::Europe::Brussels;
use jsonwebtoken::{DecodingKey, Validation, decode};
use serde::Serialize;
use serde_json::json;
use sqlx::Row;
use sqlx::types::Uuid;
use std::sync::Arc;
use tracing::info;

#[derive(Serialize)]
pub struct UserResponse {
    pub id: String,
    pub username: String,
    pub public_key: String,
    pub created_at: String,
    pub avatar: Option<String>,
}

#[derive(serde::Deserialize, serde::Serialize)]
struct Claims {
    sub: Uuid,
    exp: usize,
}

#[derive(serde::Serialize)]
pub struct MessageResponse {
    pub id: String,
    pub timestamp: String,
    pub sender_id: String,
    pub receiver_id: String,
    pub status: String,
    pub r#type: String,
    pub encrypted_content: String,
    pub iv: String,
}



/// Extracts and validates a user ID from a JWT Bearer token in the HTTP Authorization header.
///
/// Returns the user UUID from the token's claims if the token is valid and properly formatted.
/// Returns an error with `UNAUTHORIZED` status if the header is missing, malformed, or the token is invalid.
///
/// # Examples
///
/// ```
/// let req = axum::http::Request::builder()
///     .header("Authorization", "Bearer <valid_jwt>")
///     .body(())
///     .unwrap();
/// let user_id = extract_user_id_from_auth(&req, "mysecret");
/// assert!(user_id.is_ok() || user_id.is_err());
/// Extracts and validates a user UUID from a JWT Bearer token in the `Authorization` header.
///
/// Returns the user ID (`Uuid`) if the token is present, correctly formatted, and valid; otherwise returns an error with an appropriate HTTP status code.
///
/// # Examples
///
/// ```
/// use axum::http::HeaderMap;
/// let mut headers = HeaderMap::new();
/// headers.insert("authorization", "Bearer <valid_jwt_token>".parse().unwrap());
/// let user_id = extract_user_id_from_auth(&headers, "my_jwt_secret");
/// assert!(user_id.is_ok() || user_id.is_err());
/// ```
fn extract_user_id_from_auth(
    req: &HeaderMap,
    jwt_secret: &str,
) -> Result<Uuid, (StatusCode, &'static str)> {
    let auth_header = req.get(AUTHORIZATION).and_then(|h| h.to_str().ok());
    let token = match auth_header.and_then(|h| h.strip_prefix("Bearer ")) {
        Some(t) => t,
        None => {
            return Err((
                StatusCode::UNAUTHORIZED,
                "Missing or invalid Authorization header",
            ));
        }
    };
    let token_data = match decode::<Claims>(
        token,
        &DecodingKey::from_secret(jwt_secret.as_bytes()),
        &Validation::default(),
    ) {
        Ok(data) => data,
        Err(_) => return Err((StatusCode::UNAUTHORIZED, "Invalid token")),
    };
    Ok(token_data.claims.sub)
}

/// Retrieves user information by public key, returning user details as JSON if found.
///
/// This endpoint requires a valid JWT Bearer token in the `Authorization` header. If the token is missing or invalid, an unauthorized response is returned. On success, the user matching the provided public key is returned as a JSON object. If no user is found, a 404 response is returned. In case of a database error, a 500 response is returned.
///
/// # Examples
///
/// ```
/// // Example Axum route registration:
/// // app.route("/user/:public_key", get(get_user_by_public_key));
/// Retrieves user information by public key, requiring JWT authentication.
///
/// Returns the user's details as JSON if found; otherwise, returns an appropriate HTTP error status.
///
/// # Examples
///
/// ```
/// // Example Axum route usage:
/// // GET /user/{public_key} with Authorization: Bearer <token>
/// let response = get_user_by_public_key(
///     Path("user_public_key_string".to_string()),
///     State(app_state_arc),
///     headers_with_valid_jwt()
/// ).await;
/// assert_eq!(response.status(), StatusCode::OK);
/// ```
pub async fn get_user_by_public_key(
    Path(public_key): Path<String>,
    State(state): State<Arc<AppState>>,
    headers: HeaderMap,
) -> impl IntoResponse {
    let auth_result = extract_user_id_from_auth(&headers, &state.jwt_secret);
    let requesting_user = match auth_result {
        Ok(uid) => uid,
        Err(e) => {
            info!("Unauthorized access attempt to /user/{{}} endpoint");
            return e.into_response();
        }
    };
    info!(
        "User {} requested user lookup by public key: {}",
        requesting_user, public_key
    );
    let row = match sqlx::query(
        "SELECT id, username, public_key, created_at, avatar FROM users WHERE public_key = $1",
    )
    .bind(&public_key)
    .fetch_optional(&state.db)
    .await
    {
        Ok(Some(record)) => record,
        Ok(None) => {
            info!("User not found for public key: {}", public_key);
            return (axum::http::StatusCode::NOT_FOUND, "User not found").into_response();
        }
        Err(err) => {
            info!("Database error in /user/{{public_key}}: {}", err);
            return (
                axum::http::StatusCode::INTERNAL_SERVER_ERROR,
                "Database error",
            )
                .into_response();
        }
    };

    // Get created_at from database and convert to Brussels timezone
    let created_at_utc: DateTime<Utc> = row.try_get::<DateTime<Utc>, _>("created_at").unwrap();
    let created_at_brussels = created_at_utc.with_timezone(&Brussels);

    let user = UserResponse {
        id: row.try_get::<Uuid, _>("id").unwrap().to_string(),
        username: row.try_get::<String, _>("username").unwrap(),
        public_key: row.try_get::<String, _>("public_key").unwrap(),
        created_at: created_at_brussels.to_rfc3339(),
        avatar: row
            .try_get::<Option<Vec<u8>>, _>("avatar")
            .ok()
            .flatten()
            .map(base64::encode),
    };
    info!(
        "User found for public key: {} (id: {})",
        public_key, user.id
    );
    (axum::http::StatusCode::OK, Json(user)).into_response()
}

/// Retrieves user information by user ID, requiring JWT authentication.
///
/// Returns the user's details as JSON if found; otherwise, returns an appropriate HTTP error status.
///
/// # Examples
///
/// ```
/// // Example Axum route usage:
/// // GET /user/by-id/{user_id} with Authorization: Bearer <token>
/// let response = get_user_by_id(
///     Path("user-uuid-string".to_string()),
///     State(app_state_arc),
///     headers_with_valid_jwt()
/// ).await;
/// assert_eq!(response.status(), StatusCode::OK);
/// ```
pub async fn get_user_by_id(
    Path(user_id): Path<String>,
    State(state): State<Arc<AppState>>,
    headers: HeaderMap,
) -> impl IntoResponse {
    let auth_result = extract_user_id_from_auth(&headers, &state.jwt_secret);
    let requesting_user = match auth_result {
        Ok(uid) => uid,
        Err(e) => {
            info!("Unauthorized access attempt to /user/by-id/{{}} endpoint");
            return e.into_response();
        }
    };

    // Parse the user ID
    let target_user_id = match Uuid::parse_str(&user_id) {
        Ok(uid) => uid,
        Err(_) => {
            return (
                axum::http::StatusCode::BAD_REQUEST,
                "Invalid user_id format",
            )
                .into_response();
        }
    };

    info!(
        "User {} requested user lookup by ID: {}",
        requesting_user, target_user_id
    );

    let row = match sqlx::query(
        "SELECT id, username, public_key, created_at, avatar FROM users WHERE id = $1",
    )
    .bind(&target_user_id)
    .fetch_optional(&state.db)
    .await
    {
        Ok(Some(record)) => record,
        Ok(None) => {
            info!("User not found for ID: {}", target_user_id);
            return (axum::http::StatusCode::NOT_FOUND, "User not found").into_response();
        }
        Err(err) => {
            info!("Database error in /user/by-id/{{user_id}}: {}", err);
            return (
                axum::http::StatusCode::INTERNAL_SERVER_ERROR,
                "Database error",
            )
                .into_response();
        }
    };

    // Get created_at from database and convert to Brussels timezone
    let created_at_utc: DateTime<Utc> = row.try_get::<DateTime<Utc>, _>("created_at").unwrap();
    let created_at_brussels = created_at_utc.with_timezone(&Brussels);

    let user = UserResponse {
        id: row.try_get::<Uuid, _>("id").unwrap().to_string(),
        username: row.try_get::<String, _>("username").unwrap(),
        public_key: row.try_get::<String, _>("public_key").unwrap(),
        created_at: created_at_brussels.to_rfc3339(),
        avatar: row
            .try_get::<Option<Vec<u8>>, _>("avatar")
            .ok()
            .flatten()
            .map(base64::encode),
    };

    info!(
        "User found for ID: {} (username: {})",
        target_user_id, user.username
    );
    (axum::http::StatusCode::OK, Json(user)).into_response()
}

/// Handles sending a new message from the authenticated user to a specified recipient.
///
/// Validates the JWT Bearer token from the `Authorization` header, parses and verifies the recipient's UUID,
/// decodes base64-encoded encrypted content and IV, inserts the message into the database, and returns the stored message as JSON.
/// Returns appropriate HTTP status codes for authorization failures, invalid input, decoding errors, or database errors.
///
/// # Examples
///
/// ```
/// // Example Axum route registration:
/// // router.route("/messages/send", post(send_message));
/// ```


/// Placeholder endpoint for retrieving messages exchanged with a specific user.
///
/// Currently not implemented; always returns HTTP 200 with a not implemented message.
///
/// # Examples
///
/// ```
/// // This endpoint is not implemented and always returns a 200 OK status.
/// Retrieves all messages exchanged between the authenticated user and the specified user.
///
/// Requires a valid JWT Bearer token in the `Authorization` header. Returns a JSON array of messages ordered by timestamp, with binary fields base64-encoded. Responds with appropriate HTTP status codes for authorization failures, invalid user IDs, or database errors.
///
/// # Examples
///
/// ```
/// // Example Axum route usage:
/// // GET /messages/{user_id} with Authorization header
/// let response = get_messages_with_user(
///     Path("target-user-uuid".to_string()),
///     State(app_state_arc),
///     headers_with_valid_jwt()
/// ).await;
/// assert_eq!(response.status(), axum::http::StatusCode::OK);
/// Retrieves all messages exchanged between the authenticated user and the specified user.
///
/// Authenticates the request using the JWT Bearer token in the `Authorization` header. Returns a JSON array of messages ordered by timestamp, with encrypted content and IV fields base64-encoded. Responds with 401 if authentication fails, 400 if the user ID is invalid, or 500 on database errors.
///
/// # Examples
///
/// ```
/// // Example Axum route usage:
/// // GET /messages/{user_id}
/// let response = get_messages_with_user(
///     Path("target-user-uuid".to_string()),
///     State(app_state_arc),
///     headers
/// ).await;
/// ```

pub async fn get_messages_with_user(
    Path(user_id): Path<String>,
    State(state): State<Arc<AppState>>,
    headers: HeaderMap,
) -> impl IntoResponse {
    // Authenticate user
    let auth_result = extract_user_id_from_auth(&headers, &state.jwt_secret);
    let requesting_user = match auth_result {
        Ok(uid) => uid,
        Err(e) => {
            info!("Unauthorized access attempt to /messages/{{}} endpoint");
            return e.into_response();
        }
    };
    let other_user = match Uuid::parse_str(&user_id) {
        Ok(uid) => uid,
        Err(_) => {
            return (
                axum::http::StatusCode::BAD_REQUEST,
                "Invalid user_id format",
            )
                .into_response();
        }
    };
    // Query messages between requesting_user and other_user
    let rows = match sqlx::query(
        "SELECT id, timestamp, sender_id, receiver_id, status, type, encrypted_content, iv FROM messages WHERE (sender_id = $1 AND receiver_id = $2) OR (sender_id = $2 AND receiver_id = $1) ORDER BY timestamp ASC"
    )
    .bind(requesting_user)
    .bind(other_user)
    .fetch_all(&state.db)
    .await {
        Ok(records) => records,
        Err(err) => {
            info!("Database error in /messages/{{user_id}}: {}", err);
            return (
                axum::http::StatusCode::INTERNAL_SERVER_ERROR,
                "Database error",
            )
                .into_response();
        }
    };
    let messages: Vec<MessageResponse> = rows
        .into_iter()
        .map(|row| MessageResponse {
            id: row.try_get::<Uuid, _>("id").unwrap().to_string(),
            timestamp: row.try_get::<i64, _>("timestamp").unwrap().to_string(),
            sender_id: row.try_get::<Uuid, _>("sender_id").unwrap().to_string(),
            receiver_id: row.try_get::<Uuid, _>("receiver_id").unwrap().to_string(),
            status: row.try_get::<String, _>("status").unwrap_or_default(),
            r#type: row.try_get::<String, _>("type").unwrap_or_default(),
            encrypted_content: base64::encode(
                row.try_get::<Vec<u8>, _>("encrypted_content")
                    .unwrap_or_default(),
            ),
            iv: base64::encode(row.try_get::<Vec<u8>, _>("iv").unwrap_or_default()),
        })
        .collect();
    (axum::http::StatusCode::OK, axum::Json(messages)).into_response()
}

/// Returns a JSON dump of all users, contacts, and messages for admin viewing.
/// No authentication required (for demo purposes).
#[axum::debug_handler]
/// Returns a JSON dump of all users, contacts, and messages in the database.
///
/// This endpoint retrieves all records from the `users`, `contacts`, and `messages` tables,
/// encoding binary fields such as avatars and encrypted content as base64 strings. No authentication is required.
/// If any query fails, the corresponding section in the response will be an empty array.
///
/// # Examples
///
/// ```
/// // Example Axum route registration:
/// router.route("/admin/db_dump", get(db_dump));
/// // GET /admin/db_dump returns:
/// // {
/// //   "users": [ ... ],
/// //   "contacts": [ ... ],
/// //   "messages": [ ... ]
/// // }
/// ```
pub async fn db_dump(State(state): State<Arc<AppState>>) -> impl IntoResponse {
    // Fetch users
    let users =
        match sqlx::query(r#"SELECT id, username, public_key, created_at, avatar FROM users"#)
            .fetch_all(&state.db)
            .await
        {
            Ok(rows) => rows
                .into_iter()
                .map(|row| {
                    let id: sqlx::types::Uuid = row.try_get("id").unwrap();
                    let username: String = row.try_get("username").unwrap();
                    let public_key: String = row.try_get("public_key").unwrap();
                    let created_at_utc: DateTime<Utc> = row.try_get("created_at").unwrap();
                    let created_at_brussels = created_at_utc.with_timezone(&Brussels);
                    let avatar: Option<Vec<u8>> = row.try_get("avatar").ok().flatten();
                    json!({
                        "id": id,
                        "username": username,
                        "public_key": public_key,
                        "created_at": created_at_brussels.to_rfc3339(),
                        "avatar": avatar.map(|a| general_purpose::STANDARD.encode(a)),
                    })
                })
                .collect::<Vec<_>>(),
            Err(_) => vec![],
        };
    // Fetch contacts
    let contacts = match sqlx::query(
        r#"SELECT id, name, public_key, last_seen, status, avatar_url FROM contacts"#,
    )
    .fetch_all(&state.db)
    .await
    {
        Ok(rows) => rows
            .into_iter()
            .map(|row| {
                let id: sqlx::types::Uuid = row.try_get("id").unwrap();
                let name: String = row.try_get("name").unwrap();
                let public_key: String = row.try_get("public_key").unwrap();
                let last_seen: String = row.try_get("last_seen").unwrap();
                let status: Option<String> = row.try_get("status").ok().flatten();
                let avatar_url: Option<String> = row.try_get("avatar_url").ok().flatten();
                json!({
                    "id": id,
                    "name": name,
                    "public_key": public_key,
                    "last_seen": last_seen,
                    "status": status,
                    "avatar_url": avatar_url,
                })
            })
            .collect::<Vec<_>>(),
        Err(_) => vec![],
    };
    // Fetch messages
    let messages = match sqlx::query(r#"SELECT id, timestamp, sender_id, receiver_id, status, type, encrypted_content, iv FROM messages"#)
        .fetch_all(&state.db)
        .await {
            Ok(rows) => rows.into_iter().map(|row| {
                let id: sqlx::types::Uuid = row.try_get("id").unwrap();
                let timestamp_millis: i64 = row.try_get("timestamp").unwrap_or(0);
                // Convert Unix timestamp to Brussels timezone for display
                let timestamp_utc = DateTime::from_timestamp_millis(timestamp_millis).unwrap_or_else(|| Utc::now());
                let timestamp_brussels = timestamp_utc.with_timezone(&Brussels);
                let sender_id: sqlx::types::Uuid = row.try_get("sender_id").unwrap();
                let receiver_id: sqlx::types::Uuid = row.try_get("receiver_id").unwrap();
                let status: Option<String> = row.try_get("status").ok().flatten();
                let r#type: Option<String> = row.try_get("type").ok().flatten();
                let encrypted_content: Option<Vec<u8>> = row.try_get("encrypted_content").ok().flatten();
                let iv: Option<Vec<u8>> = row.try_get("iv").ok().flatten();
                json!({
                    "id": id,
                    "timestamp": timestamp_brussels.to_rfc3339(),
                    "sender_id": sender_id,
                    "receiver_id": receiver_id,
                    "status": status,
                    "type": r#type,
                    "encrypted_content": encrypted_content.map(|ec| general_purpose::STANDARD.encode(ec)),
                    "iv": iv.map(|iv| general_purpose::STANDARD.encode(iv)),
                })
            }).collect::<Vec<_>>(),
            Err(_) => vec![],
        };
    (
        StatusCode::OK,
        Json(json!({
            "users": users,
            "contacts": contacts,
            "messages": messages,
        })),
    )
}



