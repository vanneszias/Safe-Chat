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