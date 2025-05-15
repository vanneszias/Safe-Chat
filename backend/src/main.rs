mod api;
mod auth;
mod crypto;
mod state;

use api::{get_messages_with_user, get_user_by_public_key, send_message};
use auth::{get_profile, login, register, update_profile, update_public_key};
use axum::{Router, routing::get};
use dotenv::dotenv;
use sqlx::postgres::PgPoolOptions;
use state::AppState;
use std::sync::Arc;
use tokio::net::TcpListener;
use tracing_subscriber;

async fn health_check() -> impl axum::response::IntoResponse {
    (axum::http::StatusCode::OK, "OK")
}

#[tokio::main]
/// Starts the Axum web server, initializing environment, database, authentication, and HTTP routes.
///
/// Loads configuration from environment variables, connects to the PostgreSQL database, sets up JWT authentication, and configures all API routes. Binds the server to the specified port and begins serving requests asynchronously. Panics if required environment variables are missing or if the database or network binding fails.
///
/// # Examples
///
/// ```no_run
/// // Run the server (typically executed as the main entry point)
/// tokio::main(main());
/// ```
async fn main() {
    dotenv().ok();
    tracing_subscriber::fmt::init();

    let db_url = std::env::var("DATABASE_URL").expect("DATABASE_URL must be set");
    let db = PgPoolOptions::new()
        .max_connections(5)
        .connect(&db_url)
        .await
        .expect("Failed to connect to Postgres");
    let jwt_secret = std::env::var("JWT_SECRET").expect("JWT_SECRET must be set");
    let state = Arc::new(AppState { db, jwt_secret });

    let app = Router::new()
        .route("/health", get(health_check))
        .route("/auth/register", axum::routing::post(register))
        .route("/auth/login", axum::routing::post(login))
        .route("/profile", axum::routing::get(get_profile))
        .route("/profile", axum::routing::put(update_profile))
        .route("/profile/key", axum::routing::put(update_public_key))
        .route("/messages", axum::routing::post(send_message))
        .route(
            "/messages/:user_id",
            axum::routing::get(get_messages_with_user),
        )
        .route(
            "/user/:public_key",
            axum::routing::get(get_user_by_public_key),
        )
        .with_state(state);

    let port = std::env::var("SERVER_PORT").unwrap_or_else(|_| "8080".to_string());
    let addr = format!("0.0.0.0:{}", port);
    let listener = TcpListener::bind(&addr)
        .await
        .expect("Failed to bind address");
    tracing::info!("listening on {}", addr);
    axum::serve(listener, app).await.unwrap();
}
