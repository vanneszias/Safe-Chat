use crate::state::AppState;
use axum::{
    extract::{Json, Path, Request, State},
    http::StatusCode,
    http::header::AUTHORIZATION,
    response::IntoResponse,
};
use jsonwebtoken::{DecodingKey, Validation, decode};
use serde::Serialize;
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
/// ```
fn extract_user_id_from_auth(
    req: &Request,
    jwt_secret: &str,
) -> Result<Uuid, (StatusCode, &'static str)> {
    let auth_header = req
        .headers()
        .get(AUTHORIZATION)
        .and_then(|h| h.to_str().ok());
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
/// ```
pub async fn get_user_by_public_key(
    Path(public_key): Path<String>,
    State(state): State<Arc<AppState>>,
    req: Request,
) -> impl IntoResponse {
    let auth_result = extract_user_id_from_auth(&req, &state.jwt_secret);
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
    let user = UserResponse {
        id: row.try_get::<Uuid, _>("id").unwrap().to_string(),
        username: row.try_get::<String, _>("username").unwrap(),
        public_key: row.try_get::<String, _>("public_key").unwrap(),
        created_at: row
            .try_get::<chrono::DateTime<chrono::Utc>, _>("created_at")
            .unwrap()
            .to_rfc3339(),
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

/// ```
pub async fn send_message(
    State(_state): State<Arc<AppState>>,
    Json(_payload): Json<serde_json::Value>,
) -> impl IntoResponse {
    (
        axum::http::StatusCode::OK,
        "Send message endpoint (not implemented)",
    )
}

/// Placeholder endpoint for retrieving messages exchanged with a specific user.
///
/// Currently not implemented; always returns HTTP 200 with a not implemented message.
///
/// # Examples
///
/// ```
/// // This endpoint is not implemented and always returns a 200 OK status.
/// ```
pub async fn get_messages_with_user(
    Path(_user_id): Path<String>,
    State(_state): State<Arc<AppState>>,
) -> impl IntoResponse {
    (
        axum::http::StatusCode::OK,
        "Get messages with user endpoint (not implemented)",
    )
}
