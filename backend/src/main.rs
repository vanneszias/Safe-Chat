mod state;
mod auth;
mod crypto;

use axum::{routing::get, Router};
use dotenv::dotenv;
use sqlx::postgres::PgPoolOptions;
use std::sync::Arc;
use tokio::net::TcpListener;
use tracing_subscriber;
use state::AppState;
use auth::{register, login};

/// ```
async fn health_check() -> impl axum::response::IntoResponse {
    (axum::http::StatusCode::OK, "OK")
}

#[tokio::main]
/// Initializes and runs the Axum web server application.
///
/// Loads environment variables, sets up logging, establishes a PostgreSQL connection pool,
/// configures shared application state, and starts the HTTP server with authentication and health check routes.
///
/// # Panics
///
/// Panics if required environment variables are missing, the database connection fails, or the server cannot bind to the specified address.
///
/// # Examples
///
/// ```no_run
/// // To run the server, simply execute the binary:
/// // $ cargo run
/// // The server will listen on the port specified by SERVER_PORT or default to 8080.
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
        .with_state(state);

    let port = std::env::var("SERVER_PORT").unwrap_or_else(|_| "8080".to_string());
    let addr = format!("0.0.0.0:{}", port);
    let listener = TcpListener::bind(&addr).await.expect("Failed to bind address");
    tracing::info!("listening on {}", addr);
    axum::serve(listener, app).await.unwrap();
}
