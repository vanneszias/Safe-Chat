use crate::websocket::ConnectionManager;

pub struct AppState {
    pub db: sqlx::PgPool,
    pub jwt_secret: String,
    pub connections: ConnectionManager,
}
