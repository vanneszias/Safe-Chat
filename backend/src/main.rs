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

async fn health_check() -> impl axum::response::IntoResponse {
    (axum::http::StatusCode::OK, "OK")
}

#[tokio::main]
async fn main() {
    dotenv().ok();
    tracing_subscriber::fmt::init();

    let db_url = std::env::var("DATABASE_URL")
        .map_err(|_| {
            tracing::error!("DATABASE_URL environment variable not set");
            std::process::exit(1);
        })
        .unwrap();

    let db = PgPoolOptions::new()
        .max_connections(5)
        .connect_timeout(std::time::Duration::from_secs(30))
        .connect(&db_url)
        .await
        .map_err(|err| {
            tracing::error!("Failed to connect to Postgres: {}", err);
            std::process::exit(1);
        })
        .unwrap();

    let jwt_secret = std::env::var("JWT_SECRET")
        .map_err(|_| {
            tracing::error!("JWT_SECRET environment variable not set");
            std::process::exit(1);
        })
        .unwrap();

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
