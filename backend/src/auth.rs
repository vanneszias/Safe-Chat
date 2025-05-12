use axum::{extract::State, response::IntoResponse, Json};
use serde::Deserialize;
use serde::Serialize;
use std::sync::Arc;
use argon2::{Argon2, PasswordHasher, PasswordVerifier};
use argon2::password_hash::{SaltString, rand_core::OsRng};
use crate::state::AppState;
use crate::crypto::generate_keypair_base64;
use jsonwebtoken::{encode, EncodingKey, Header};
use sqlx::Row;

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

#[derive(Serialize)]
struct Claims {
    sub: String,
    exp: usize,
}

/// Handles user registration by creating a new user with a hashed password and generated public key.
///
/// Accepts a JSON payload with a username and password, hashes the password using Argon2, generates a base64-encoded public key, and inserts the new user into the database. Returns HTTP 201 with the public key on success, HTTP 409 if the username already exists, or HTTP 500 on other errors.
///
/// # Examples
///
/// ```
/// use axum::body::Body;
/// use axum::http::{Request, StatusCode};
/// use serde_json::json;
///
/// let app = axum::Router::new().route("/register", axum::routing::post(register));
/// let payload = json!({ "username": "alice", "password": "secret" });
/// let req = Request::builder()
///     .method("POST")
///     .uri("/register")
///     .header("content-type", "application/json")
///     .body(Body::from(payload.to_string()))
///     .unwrap();
///
/// // In a test context, call the app and assert the response:
/// // let response = app.oneshot(req).await.unwrap();
/// // assert_eq!(response.status(), StatusCode::CREATED);
/// ```
pub async fn register(
    State(state): State<Arc<AppState>>,
    Json(payload): Json<RegisterRequest>,
) -> impl IntoResponse {
    // Hash the password
    let salt = SaltString::generate(&mut OsRng);
    let argon2 = Argon2::default();
    let password_hash = match argon2.hash_password(payload.password.as_bytes(), &salt) {
        Ok(hash) => hash.to_string(),
        Err(_) => return (axum::http::StatusCode::INTERNAL_SERVER_ERROR, "Password hash error").into_response(),
    };

    // Generate key pair
    let public_key_b64 = generate_keypair_base64();
    // (In a real app, store the private key securely, e.g., in a KMS or encrypted vault)

    // Insert user into DB
    let res = sqlx::query(
        "INSERT INTO users (username, password_hash, public_key) VALUES ($1, $2, $3)"
    )
    .bind(&payload.username)
    .bind(&password_hash)
    .bind(&public_key_b64)
    .execute(&state.db)
    .await;

    match res {
        Ok(_) => (
            axum::http::StatusCode::CREATED,
            format!("User registered. Public key: {}", public_key_b64)
        ).into_response(),
        Err(e) if e.to_string().contains("duplicate key") => (axum::http::StatusCode::CONFLICT, "Username already exists").into_response(),
        Err(_) => (axum::http::StatusCode::INTERNAL_SERVER_ERROR, "Registration failed").into_response(),
    }
}

/// Authenticates a user and returns a JWT token on successful login.
///
/// Accepts a JSON payload with username and password, verifies the credentials against the database,
/// and issues a JWT token valid for 24 hours if authentication succeeds. Returns appropriate HTTP status codes
/// and error messages for invalid credentials, database errors, or token creation failures.
///
/// # Examples
///
/// ```
/// // Example Axum route usage:
/// let app = axum::Router::new().route("/login", axum::routing::post(login));
/// // Send POST request with JSON: { "username": "alice", "password": "secret" }
/// ```
pub async fn login(
    State(state): State<Arc<AppState>>,
    Json(payload): Json<LoginRequest>,
) -> impl IntoResponse {
    // Fetch user from DB
    let row = sqlx::query(
        "SELECT password_hash FROM users WHERE username = $1"
    )
    .bind(&payload.username)
    .fetch_optional(&state.db)
    .await;

    let password_hash: String = match row {
        Ok(Some(record)) => match record.try_get("password_hash") {
            Ok(hash) => hash,
            Err(_) => return (axum::http::StatusCode::INTERNAL_SERVER_ERROR, "DB row error").into_response(),
        },
        Ok(None) => return (axum::http::StatusCode::UNAUTHORIZED, "Invalid credentials").into_response(),
        Err(_) => return (axum::http::StatusCode::INTERNAL_SERVER_ERROR, "Database error").into_response(),
    };

    // Verify password
    let parsed_hash = match argon2::PasswordHash::new(&password_hash) {
        Ok(hash) => hash,
        Err(_) => return (axum::http::StatusCode::INTERNAL_SERVER_ERROR, "Hash parse error").into_response(),
    };
    let argon2 = Argon2::default();
    if argon2.verify_password(payload.password.as_bytes(), &parsed_hash).is_err() {
        return (axum::http::StatusCode::UNAUTHORIZED, "Invalid credentials").into_response();
    }

    // Create JWT
    let expiration = chrono::Utc::now()
        .checked_add_signed(chrono::Duration::hours(24))
        .expect("valid timestamp")
        .timestamp() as usize;
    let claims = Claims {
        sub: payload.username,
        exp: expiration,
    };
    let token = match encode(
        &Header::default(),
        &claims,
        &EncodingKey::from_secret(state.jwt_secret.as_bytes()),
    ) {
        Ok(t) => t,
        Err(_) => return (axum::http::StatusCode::INTERNAL_SERVER_ERROR, "Token creation error").into_response(),
    };
    (
        axum::http::StatusCode::OK,
        Json(serde_json::json!({ "token": token }))
    ).into_response()
} 