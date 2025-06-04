mod api;
mod auth;
mod crypto;
mod state;

use api::{
    db_dump, get_messages_with_user, get_user_by_id, get_user_by_public_key, send_message,
    update_message_status,
};
use auth::{get_profile, login, register, update_profile, update_public_key};
use axum::{Router, routing::get};
use dotenv::dotenv;
use sqlx::postgres::PgPoolOptions;
use state::AppState;
use std::sync::Arc;
use tower_http::services::ServeFile;
use tracing_subscriber;

/// Returns a 200 OK response for health check endpoints.
///
/// # Examples
///
/// ```
/// let response = health_check().await;
/// assert_eq!(response.into_response().status(), axum::http::StatusCode::OK);
/// ```
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
/// Starts the Axum-based asynchronous web server, initializing environment, database, and routing.
///
/// Loads environment variables, sets up logging, connects to the PostgreSQL database, and configures the application state. Defines all HTTP routes and launches the server on the specified port, handling incoming requests indefinitely.
///
/// # Examples
///
/// ```
/// // To run the server, simply execute the binary:
/// #[tokio::main]
/// async fn main() {
///     main().await;
/// }
/// Initializes and runs the Axum web server with PostgreSQL integration and JWT authentication.
///
/// Loads environment variables, sets up logging, connects to the database, configures application state, and defines all HTTP routes. The server listens on the port specified by the `SERVER_PORT` environment variable or defaults to 8080. Panics if required environment variables are missing or if the server fails to start.
///
/// # Examples
///
/// ```no_run
/// // To start the server, run the compiled binary:
/// // $ DATABASE_URL=postgres://... JWT_SECRET=... cargo run
/// // The server will be accessible at http://localhost:8080/
/// #[tokio::main]
/// async fn main() {
///     my_crate::main().await;
/// }
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
            "/messages/:message_id/status",
            axum::routing::put(update_message_status),
        )
        .route(
            "/user/:public_key",
            axum::routing::get(get_user_by_public_key),
        )
        .route("/user/by-id/:user_id", axum::routing::get(get_user_by_id))
        .route("/admin/dbdump", get(db_dump))
        .nest_service("/admin/dbtable.html", ServeFile::new("src/dbtable.html"))
        .nest_service("/", ServeFile::new("src/dbtable.html"))
        .with_state(state);

    let port = std::env::var("SERVER_PORT").unwrap_or_else(|_| "8080".to_string());
    let addr = format!("0.0.0.0:{}", port);
    tracing::info!("listening on {}", addr);
    axum::Server::bind(&addr.parse().unwrap())
        .serve(app.into_make_service())
        .await
        .unwrap();
}
