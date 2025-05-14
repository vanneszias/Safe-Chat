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
use chrono::{DateTime, Utc};
use jsonwebtoken::{DecodingKey, EncodingKey, Header, Validation, decode, encode};
use serde::Deserialize;
use serde::Serialize;
use serde_json::json;
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
}

#[derive(Deserialize)]
pub struct UpdateKeyRequest {
    pub public_key: String,
}

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
    let row = sqlx::query("SELECT id, username, public_key, created_at FROM users WHERE id = $1")
        .bind(user_id)
        .fetch_optional(&state.db)
        .await;
    match row {
        Ok(Some(record)) => {
            let id: Uuid = record.try_get("id").unwrap();
            let username: String = record.try_get("username").unwrap();
            let public_key: String = record.try_get("public_key").unwrap();
            let created_at: DateTime<Utc> = record.try_get("created_at").unwrap_or(Utc::now());
            let profile = UserProfile {
                id: id.to_string(),
                username,
                public_key,
                created_at: created_at.to_rfc3339(),
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
