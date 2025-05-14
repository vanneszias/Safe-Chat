use crate::crypto::generate_keypair_base64;
use crate::state::AppState;
use argon2::password_hash::{SaltString, rand_core::OsRng};
use argon2::{Argon2, PasswordHasher, PasswordVerifier};
use axum::{
    Json, body,
    extract::{Request, State},
    http::StatusCode,
    http::header::AUTHORIZATION,
    response::IntoResponse,
};
use base64;
use chrono::{DateTime, Utc};
use jsonwebtoken::{DecodingKey, EncodingKey, Header, Validation, decode, encode};
use serde::Deserialize;
use serde::Serialize;
use serde_json::json;
use sqlx::Postgres;
use sqlx::Row;
use sqlx::types::Uuid;
use std::sync::Arc;
use tracing::info;

#[derive(Deserialize)]
pub struct RegisterRequest {
    pub username: String,
    pub password: String,
}

#[derive(Deserialize)]
pub struct LoginRequest {
    pub username: String,
    pub password: String,
}

#[derive(Serialize, Deserialize)]
struct Claims {
    sub: Uuid,
    exp: usize,
}

#[derive(Serialize)]
pub struct UserProfile {
    pub id: String,
    pub username: String,
    pub public_key: String,
    pub created_at: String,
    pub avatar: Option<String>,
}

#[derive(Deserialize)]
pub struct UpdateKeyRequest {
    pub public_key: String,
}

#[derive(Deserialize)]
pub struct UpdateProfileRequest {
    pub username: Option<String>,
    pub avatar: Option<String>, // base64-encoded
}

/// Handles user registration by creating a new user account with a hashed password, generating a public key, and returning a JWT token.
///
/// On success, returns HTTP 201 with the user's UUID, generated public key, and a JWT token. If the username already exists, returns HTTP 409 with an error message. Returns HTTP 500 for internal errors.
///
/// # Examples
///
/// ```
/// use axum::{body::Body, http::{Request, StatusCode}};
/// use serde_json::json;
///
/// // Assume `app` is your Axum router with the register route.
/// let payload = json!({ "username": "alice", "password": "securepass" });
/// let req = Request::builder()
///     .method("POST")
///     .uri("/register")
///     .header("content-type", "application/json")
///     .body(Body::from(payload.to_string()))
///     .unwrap();
///
/// let response = app.oneshot(req).await.unwrap();
/// assert_eq!(response.status(), StatusCode::CREATED);
/// ```
pub async fn register(
    State(state): State<Arc<AppState>>,
    Json(payload): Json<RegisterRequest>,
) -> impl IntoResponse {
    info!("Register attempt for username: {}", payload.username);
    // Hash the password
    let salt = SaltString::generate(&mut OsRng);
    let argon2 = Argon2::default();
    let password_hash = match argon2.hash_password(payload.password.as_bytes(), &salt) {
        Ok(hash) => hash.to_string(),
        Err(_) => {
            return (
                axum::http::StatusCode::INTERNAL_SERVER_ERROR,
                "Password hash error",
            )
                .into_response();
        }
    };

    // Generate key pair
    let public_key_b64 = generate_keypair_base64();
    // Insert user into DB and return id
    let res = sqlx::query(
        "INSERT INTO users (username, password_hash, public_key) VALUES ($1, $2, $3) RETURNING id",
    )
    .bind(&payload.username)
    .bind(&password_hash)
    .bind(&public_key_b64)
    .fetch_one(&state.db)
    .await;

    match res {
        Ok(record) => {
            let id: Uuid = record.try_get("id").unwrap();
            // Create JWT
            let expiration = chrono::Utc::now()
                .checked_add_signed(chrono::Duration::hours(24))
                .expect("valid timestamp")
                .timestamp() as usize;
            let claims = Claims {
                sub: id,
                exp: expiration,
            };
            let token = match encode(
                &Header::default(),
                &claims,
                &EncodingKey::from_secret(state.jwt_secret.as_bytes()),
            ) {
                Ok(t) => t,
                Err(_) => {
                    return (
                        axum::http::StatusCode::INTERNAL_SERVER_ERROR,
                        "Token creation error",
                    )
                        .into_response();
                }
            };
            (
                axum::http::StatusCode::CREATED,
                Json(serde_json::json!({
                    "id": id.to_string(),
                    "public_key": public_key_b64,
                    "token": token
                })),
            )
                .into_response()
        }
        Err(e) if e.to_string().contains("duplicate key") => (
            axum::http::StatusCode::CONFLICT,
            axum::Json(serde_json::json!({ "error": "Username already exists" })),
        )
            .into_response(),
        Err(_) => (
            axum::http::StatusCode::INTERNAL_SERVER_ERROR,
            axum::Json(serde_json::json!({ "error": "Registration failed" })),
        )
            .into_response(),
    }
}

/// Authenticates a user by verifying credentials and returns a JWT token on success.
///
/// Receives a username and password, verifies the credentials against the database using Argon2 password hashing,
/// and issues a JWT token with a 24-hour expiration if authentication succeeds. Returns JSON error responses with
/// appropriate HTTP status codes for invalid credentials, database errors, or token creation failures.
///
/// # Examples
///
/// ```
/// // Example usage in an Axum route handler
/// let response = login(state, Json(LoginRequest {
///     username: "alice".to_string(),
///     password: "password123".to_string(),
/// })).await;
/// // On success, response contains a JSON object with a "token" field.
/// ```
pub async fn login(
    State(state): State<Arc<AppState>>,
    Json(payload): Json<LoginRequest>,
) -> impl IntoResponse {
    info!("Login attempt for username: {}", payload.username);
    // Fetch user from DB
    let row = sqlx::query("SELECT id, password_hash FROM users WHERE username = $1")
        .bind(&payload.username)
        .fetch_optional(&state.db)
        .await;

    let (user_id, password_hash): (Uuid, String) = match row {
        Ok(Some(record)) => (
            record.try_get("id").unwrap(),
            record.try_get("password_hash").unwrap(),
        ),
        Ok(None) => {
            info!(
                "Login failed for username: {} (user not found)",
                payload.username
            );
            return (
                axum::http::StatusCode::UNAUTHORIZED,
                axum::Json(serde_json::json!({ "error": "Invalid credentials" })),
            )
                .into_response();
        }
        Err(_) => {
            info!(
                "Login failed for username: {} (database error)",
                payload.username
            );
            return (
                axum::http::StatusCode::INTERNAL_SERVER_ERROR,
                axum::Json(serde_json::json!({ "error": "Database error" })),
            )
                .into_response();
        }
    };

    // Verify password
    let parsed_hash = match argon2::PasswordHash::new(&password_hash) {
        Ok(hash) => hash,
        Err(_) => {
            info!(
                "Login failed for username: {} (hash parse error)",
                payload.username
            );
            return (
                axum::http::StatusCode::INTERNAL_SERVER_ERROR,
                axum::Json(serde_json::json!({ "error": "Hash parse error" })),
            )
                .into_response();
        }
    };
    let argon2 = Argon2::default();
    if argon2
        .verify_password(payload.password.as_bytes(), &parsed_hash)
        .is_err()
    {
        info!(
            "Login failed for username: {} (wrong password)",
            payload.username
        );
        return (
            axum::http::StatusCode::UNAUTHORIZED,
            axum::Json(serde_json::json!({ "error": "Invalid credentials" })),
        )
            .into_response();
    }

    // Create JWT
    let expiration = chrono::Utc::now()
        .checked_add_signed(chrono::Duration::hours(24))
        .expect("valid timestamp")
        .timestamp() as usize;
    let claims = Claims {
        sub: user_id,
        exp: expiration,
    };
    let token = match encode(
        &Header::default(),
        &claims,
        &EncodingKey::from_secret(state.jwt_secret.as_bytes()),
    ) {
        Ok(t) => t,
        Err(_) => {
            return (
                axum::http::StatusCode::INTERNAL_SERVER_ERROR,
                "Token creation error",
            )
                .into_response();
        }
    };
    (
        axum::http::StatusCode::OK,
        Json(serde_json::json!({ "token": token })),
    )
        .into_response()
}

/// Retrieves the authenticated user's profile information using a JWT bearer token.
///
/// Extracts the user ID from the provided JWT in the `Authorization` header, queries the database for the user's profile, and returns the profile data as JSON. Returns appropriate HTTP status codes for missing or invalid tokens, user not found, or database errors.
///
/// # Examples
///
/// ```
/// // Example request using reqwest (assuming server is running and token is valid)
/// let client = reqwest::Client::new();
/// let res = client
///     .get("http://localhost:3000/profile")
///     .bearer_auth("your_jwt_token_here")
///     .send()
///     .await
///     .unwrap();
/// assert_eq!(res.status(), 200);
/// let profile: serde_json::Value = res.json().await.unwrap();
/// assert!(profile.get("username").is_some());
/// Retrieves the authenticated user's profile information.
///
/// Extracts and validates the JWT from the `Authorization` header, then queries the database for the user's profile fields, including ID, username, public key, creation timestamp, and optional base64-encoded avatar. Returns the profile as a JSON response or an appropriate error if authentication fails or the user is not found.
///
/// # Examples
///
/// ```
/// // Example request using an authenticated client:
/// let response = client
///     .get("/profile")
///     .bearer_auth("valid_jwt_token")
///     .send()
///     .await;
/// assert_eq!(response.status(), 200);
/// let profile: UserProfile = response.json().await.unwrap();
/// assert_eq!(profile.username, "alice");
/// ```
pub async fn get_profile(State(state): State<Arc<AppState>>, req: Request) -> impl IntoResponse {
    // Extract Authorization header
    let auth_header = req
        .headers()
        .get(AUTHORIZATION)
        .and_then(|h| h.to_str().ok());
    let token = match auth_header.and_then(|h| h.strip_prefix("Bearer ")) {
        Some(t) => t,
        None => {
            info!("Profile request failed: missing or invalid Authorization header");
            return (
                StatusCode::UNAUTHORIZED,
                "Missing or invalid Authorization header",
            )
                .into_response();
        }
    };
    // Decode JWT
    let token_data = match decode::<Claims>(
        token,
        &DecodingKey::from_secret(state.jwt_secret.as_bytes()),
        &Validation::default(),
    ) {
        Ok(data) => data,
        Err(_) => {
            info!("Profile request failed: invalid token");
            return (StatusCode::UNAUTHORIZED, "Invalid token").into_response();
        }
    };
    let user_id = token_data.claims.sub;
    info!("Profile requested for user_id: {}", user_id);
    // Fetch user from DB (include id)
    let row =
        sqlx::query("SELECT id, username, public_key, created_at, avatar FROM users WHERE id = $1")
            .bind(user_id)
            .fetch_optional(&state.db)
            .await;
    match row {
        Ok(Some(record)) => {
            let id: Uuid = record.try_get("id").unwrap();
            let username: String = record.try_get("username").unwrap();
            let public_key: String = record.try_get("public_key").unwrap();
            let created_at: DateTime<Utc> = record.try_get("created_at").unwrap_or(Utc::now());
            let avatar_bytes: Option<Vec<u8>> = record.try_get("avatar").ok();
            let avatar = avatar_bytes.map(|bytes| base64::encode(bytes));
            let profile = UserProfile {
                id: id.to_string(),
                username,
                public_key,
                created_at: created_at.to_rfc3339(),
                avatar,
            };
            (StatusCode::OK, Json(json!(profile))).into_response()
        }
        Ok(None) => {
            info!("Profile request: user '{}' not found", user_id);
            (StatusCode::NOT_FOUND, "User not found").into_response()
        }
        Err(_) => {
            info!("Profile request: database error for user '{}'", user_id);
            (StatusCode::INTERNAL_SERVER_ERROR, "Database error").into_response()
        }
    }
}

/// Updates the authenticated user's public key.
///
/// Extracts the user's UUID from a JWT in the `Authorization` header, reads the new public key from the request body, and updates it in the database. Returns an appropriate HTTP response based on the outcome.
///
/// # Examples
///
/// ```
/// // Example request (pseudo-code):
/// // PUT /update_public_key
/// // Authorization: Bearer <jwt>
/// // Body: { "public_key": "base64string" }
/// let response = update_public_key(state, request).await;
/// assert_eq!(response.status(), StatusCode::OK);
/// Updates the authenticated user's public key.
///
/// Extracts and validates the JWT from the `Authorization` header, parses the JSON body for the new public key, and updates the user's public key in the database. Returns an appropriate HTTP response based on the outcome.
///
/// # Examples
///
/// ```
/// // Example request using an HTTP client:
/// // PATCH /api/profile/key
/// // Authorization: Bearer <token>
/// // Body: { "public_key": "base64-encoded-key" }
/// ```
pub async fn update_public_key(
    State(state): State<Arc<AppState>>,
    req: Request,
) -> impl IntoResponse {
    // Extract Authorization header
    let auth_header = req
        .headers()
        .get(AUTHORIZATION)
        .and_then(|h| h.to_str().ok());
    let token = match auth_header.and_then(|h| h.strip_prefix("Bearer ")) {
        Some(t) => t,
        None => {
            info!("Update key failed: missing or invalid Authorization header");
            return (
                StatusCode::UNAUTHORIZED,
                "Missing or invalid Authorization header",
            )
                .into_response();
        }
    };
    // Decode JWT
    let token_data = match decode::<Claims>(
        token,
        &DecodingKey::from_secret(state.jwt_secret.as_bytes()),
        &Validation::default(),
    ) {
        Ok(data) => data,
        Err(_) => {
            info!("Update key failed: invalid token");
            return (StatusCode::UNAUTHORIZED, "Invalid token").into_response();
        }
    };
    let user_id = token_data.claims.sub;
    info!("Public key update requested for user_id: {}", user_id);
    // Extract JSON body
    let bytes = match body::to_bytes(req.into_body(), 64 * 1024).await {
        Ok(b) => b,
        Err(_) => {
            return (StatusCode::BAD_REQUEST, "Invalid body").into_response();
        }
    };
    let payload: UpdateKeyRequest = match serde_json::from_slice(&bytes) {
        Ok(p) => p,
        Err(_) => {
            return (StatusCode::BAD_REQUEST, "Invalid JSON").into_response();
        }
    };
    // Update public key in DB
    let res = sqlx::query("UPDATE users SET public_key = $1 WHERE id = $2")
        .bind(&payload.public_key)
        .bind(user_id)
        .execute(&state.db)
        .await;
    match res {
        Ok(_) => (StatusCode::OK, "Public key updated").into_response(),
        Err(_) => {
            info!("Update key failed: database error for user '{}'", user_id);
            (StatusCode::INTERNAL_SERVER_ERROR, "Database error").into_response()
        }
    }
}

/// Updates the authenticated user's profile fields, such as username and avatar.
///
/// Accepts a JSON body with optional `username` and/or base64-encoded `avatar` fields. Requires a valid JWT bearer token in the `Authorization` header. Returns an error if no fields are provided, the avatar encoding is invalid, or the token is missing or invalid.
///
/// # Examples
///
/// ```
/// // Example request using reqwest (pseudo-code)
/// let client = reqwest::Client::new();
/// let res = client
///     .post("http://localhost:3000/profile")
///     .bearer_auth("your_jwt_token")
///     .json(&serde_json::json!({ "username": "newname", "avatar": "base64string" }))
///     .send()
///     .await
///     .unwrap();
/// assert_eq!(res.status(), 200);
/// ```
pub async fn update_profile(State(state): State<Arc<AppState>>, req: Request) -> impl IntoResponse {
    // Extract Authorization header
    let auth_header = req
        .headers()
        .get(axum::http::header::AUTHORIZATION)
        .and_then(|h| h.to_str().ok());
    let token = match auth_header.and_then(|h| h.strip_prefix("Bearer ")) {
        Some(t) => t,
        None => {
            return (
                StatusCode::UNAUTHORIZED,
                "Missing or invalid Authorization header",
            )
                .into_response();
        }
    };
    // Decode JWT
    let token_data = match decode::<Claims>(
        token,
        &DecodingKey::from_secret(state.jwt_secret.as_bytes()),
        &Validation::default(),
    ) {
        Ok(data) => data,
        Err(_) => {
            return (StatusCode::UNAUTHORIZED, "Invalid token").into_response();
        }
    };
    let user_id = token_data.claims.sub;
    // Extract JSON body
    let bytes = match axum::body::to_bytes(req.into_body(), 64 * 1024).await {
        Ok(b) => b,
        Err(_) => {
            return (StatusCode::BAD_REQUEST, "Invalid body").into_response();
        }
    };
    let payload: UpdateProfileRequest = match serde_json::from_slice(&bytes) {
        Ok(p) => p,
        Err(_) => {
            return (StatusCode::BAD_REQUEST, "Invalid JSON").into_response();
        }
    };
    let mut set_clauses: Vec<String> = Vec::new();
    let mut log_fields = Vec::new();
    if payload.username.is_some() {
        set_clauses.push("username = $1".to_string());
        log_fields.push("username");
    }
    if payload.avatar.is_some() {
        set_clauses.push(format!("avatar = ${}", set_clauses.len() + 1));
        log_fields.push("avatar");
    }
    if set_clauses.is_empty() {
        return (StatusCode::BAD_REQUEST, "No fields to update").into_response();
    }
    let query = format!(
        "UPDATE users SET {} WHERE id = ${}",
        set_clauses.join(", "),
        set_clauses.len() + 1
    );
    info!(
        "Update profile requested for user_id: {}. Fields: {:?}",
        user_id, log_fields
    );
    let mut sql_query = sqlx::query(&query);
    let mut bind_count = 1;
    if let Some(ref username) = payload.username {
        sql_query = sql_query.bind(username);
        bind_count += 1;
    }
    if let Some(ref avatar_b64) = payload.avatar {
        let avatar_bytes = match base64::decode(avatar_b64) {
            Ok(bytes) => bytes,
            Err(_) => {
                return (StatusCode::BAD_REQUEST, "Invalid avatar encoding").into_response();
            }
        };
        sql_query = sql_query.bind(avatar_bytes);
        bind_count += 1;
    }
    sql_query = sql_query.bind(user_id);
    let res = sql_query.execute(&state.db).await;
    match res {
        Ok(_) => {
            info!(
                "Profile updated for user_id: {}. Fields: {:?}",
                user_id, log_fields
            );
            (StatusCode::OK, "Profile updated").into_response()
        }
        Err(e) => {
            info!(
                "Profile update failed for user_id: {}. Error: {}",
                user_id, e
            );
            (StatusCode::INTERNAL_SERVER_ERROR, "Database error").into_response()
        }
    }
}
