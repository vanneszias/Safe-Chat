use axum::{
    extract::{
        ws::{Message, WebSocket, WebSocketUpgrade},
        Query, State,
    },
    http::StatusCode,
    response::Response,
};
use base64::Engine;
use chrono::{Utc};
use chrono_tz::Europe::Brussels;
use dashmap::DashMap;
use futures_util::{sink::SinkExt, stream::StreamExt};
use serde::{Deserialize, Serialize};
use sqlx::Row;
use std::sync::Arc;
use std::time::Duration;
use tokio::sync::broadcast;
use tokio::time::sleep;
use tracing::{error, info, warn};
use uuid::Uuid;

use crate::{auth::decode_jwt_token, state::AppState};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct WebSocketMessage {
    pub message_type: String,
    pub data: serde_json::Value,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SendMessageData {
    pub message_id: String,
    pub receiver_id: String,
    pub r#type: String,
    pub encrypted_content: String,
    pub iv: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UpdateStatusData {
    pub message_id: String,
    pub status: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MessageNotification {
    pub id: String,
    pub timestamp: String,
    pub sender_id: String,
    pub receiver_id: String,
    pub status: String,
    pub r#type: String,
    pub encrypted_content: String,
    pub iv: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct StatusUpdate {
    pub message_id: String,
    pub status: String,
    pub updated_by: String,
}

#[derive(Debug, Clone)]
pub enum WSEvent {
    NewMessage(MessageNotification),
    StatusUpdate(StatusUpdate),
    UserOnline(String),
    UserOffline(String),
}

pub type ConnectionManager = Arc<DashMap<Uuid, broadcast::Sender<WSEvent>>>;

#[derive(Deserialize)]
pub struct WSQueryParams {
    token: String,
}

pub async fn websocket_handler(
    ws: WebSocketUpgrade,
    Query(params): Query<WSQueryParams>,
    State(state): State<Arc<AppState>>,
) -> Result<Response, StatusCode> {
    // Validate JWT token
    let user_id = match decode_jwt_token(&params.token, &state.jwt_secret) {
        Ok(claims) => claims.sub,
        Err(_) => {
            warn!("WebSocket connection attempt with invalid token");
            return Err(StatusCode::UNAUTHORIZED);
        }
    };

    info!("WebSocket connection established for user: {}", user_id);

    Ok(ws.on_upgrade(move |socket| {
        handle_websocket(socket, user_id, state)
    }))
}

async fn handle_websocket(
    socket: WebSocket,
    user_id: Uuid,
    state: Arc<AppState>,
) {
    let (sender, mut receiver) = socket.split();
    let sender = Arc::new(tokio::sync::Mutex::new(sender));

    // Create broadcast channel for this user
    let (tx, mut rx) = broadcast::channel(100);
    state.connections.insert(user_id, tx.clone());

    info!("User {} connected to WebSocket", user_id);

    // Broadcast user online status
    broadcast_to_all(&state.connections, WSEvent::UserOnline(user_id.to_string())).await;

    // Handle incoming messages from client
    let connections_clone = state.connections.clone();
    let state_clone = state.clone();
    let user_id_clone = user_id;
    let sender_clone = sender.clone();
    let incoming_task = tokio::spawn(async move {
        while let Some(msg) = receiver.next().await {
            match msg {
                Ok(Message::Text(text)) => {
                    if let Err(e) = handle_client_message(&text, user_id_clone, &connections_clone, state_clone.clone()).await {
                        error!("Error handling client message: {}", e);
                    }
                }
                Ok(Message::Close(_)) => {
                    info!("WebSocket connection closed by client for user: {}", user_id_clone);
                    break;
                }
                Ok(Message::Ping(data)) => {
                    let mut sender_guard = sender_clone.lock().await;
                    if sender_guard.send(Message::Pong(data)).await.is_err() {
                        break;
                    }
                }
                Ok(_) => {
                    // Handle other message types if needed
                }
                Err(e) => {
                    error!("WebSocket error for user {}: {}", user_id_clone, e);
                    break;
                }
            }
        }
    });

    // Handle outgoing messages to client
    let outgoing_task = tokio::spawn(async move {
        while let Ok(event) = rx.recv().await {
            let message = match event {
                WSEvent::NewMessage(msg) => WebSocketMessage {
                    message_type: "new_message".to_string(),
                    data: serde_json::to_value(msg).unwrap_or_default(),
                },
                WSEvent::StatusUpdate(update) => WebSocketMessage {
                    message_type: "status_update".to_string(),
                    data: serde_json::to_value(update).unwrap_or_default(),
                },
                WSEvent::UserOnline(user) => WebSocketMessage {
                    message_type: "user_online".to_string(),
                    data: serde_json::json!({ "user_id": user }),
                },
                WSEvent::UserOffline(user) => WebSocketMessage {
                    message_type: "user_offline".to_string(),
                    data: serde_json::json!({ "user_id": user }),
                },
            };

            let text = match serde_json::to_string(&message) {
                Ok(text) => text,
                Err(e) => {
                    error!("Failed to serialize WebSocket message: {}", e);
                    continue;
                }
            };

            let mut sender_guard = sender.lock().await;
            if sender_guard.send(Message::Text(text)).await.is_err() {
                break;
            }
        }
    });

    // Wait for either task to complete
    tokio::select! {
        _ = incoming_task => {},
        _ = outgoing_task => {},
    }

    // Clean up connection
    state.connections.remove(&user_id);
    info!("User {} disconnected from WebSocket", user_id);

    // Broadcast user offline status
    broadcast_to_all(&state.connections, WSEvent::UserOffline(user_id.to_string())).await;
}

async fn handle_client_message(
    text: &str,
    user_id: Uuid,
    connections: &ConnectionManager,
    state: Arc<AppState>,
) -> Result<(), String> {
    let message: WebSocketMessage = serde_json::from_str(text)
        .map_err(|e| format!("Failed to parse client message: {}", e))?;

    info!("Received WebSocket message from user {}: {:?}", user_id, message.message_type);

    match message.message_type.as_str() {
        "ping" => {
            // Handle ping/pong for connection health
            info!("Received ping from user: {}", user_id);
        }
        "mark_typing" => {
            // Could implement typing indicators here
            info!("User {} is typing", user_id);
        }
        "send_message" => {
            handle_send_message(user_id, message.data, connections, state).await?;
        }
        "update_status" => {
            handle_update_status(user_id, message.data, connections, state).await?;
        }
        _ => {
            warn!("Unknown message type: {}", message.message_type);
        }
    }

    Ok(())
}

async fn handle_send_message(
    sender_id: Uuid,
    data: serde_json::Value,
    connections: &ConnectionManager,
    state: Arc<AppState>,
) -> Result<(), String> {
    
    let send_data: SendMessageData = serde_json::from_value(data)
        .map_err(|e| format!("Failed to parse send_message data: {}", e))?;

    // Parse receiver_id and message_id
    let receiver_id = Uuid::parse_str(&send_data.receiver_id)
        .map_err(|_| "Invalid receiver_id format".to_string())?;
    let message_id = Uuid::parse_str(&send_data.message_id)
        .map_err(|_| "Invalid message_id format".to_string())?;

    // Generate timestamp
    let now = Utc::now().with_timezone(&Brussels);
    let timestamp_millis = now.timestamp_millis();

    // Decode base64 fields
    let encrypted_content = base64::engine::general_purpose::STANDARD.decode(&send_data.encrypted_content)
        .map_err(|_| "Invalid base64 for encrypted_content".to_string())?;
    let iv = base64::engine::general_purpose::STANDARD.decode(&send_data.iv)
        .map_err(|_| "Invalid base64 for iv".to_string())?;

    let status = "SENT";

    // Insert into database
    let res = sqlx::query(
        "INSERT INTO messages (id, timestamp, sender_id, receiver_id, status, type, encrypted_content, iv) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)"
    )
    .bind(message_id)
    .bind(timestamp_millis)
    .bind(sender_id)
    .bind(receiver_id)
    .bind(status)
    .bind(&send_data.r#type)
    .bind(&encrypted_content)
    .bind(&iv)
    .execute(&state.db)
    .await;

    if let Err(e) = res {
        return Err(format!("Database error: {}", e));
    }

    info!("Message {} stored in database with SENT status", message_id);

    // Create message notification for receiver
    let message_notification = MessageNotification {
        id: message_id.to_string(),
        timestamp: timestamp_millis.to_string(),
        sender_id: sender_id.to_string(),
        receiver_id: receiver_id.to_string(),
        status: status.to_string(),
        r#type: send_data.r#type,
        encrypted_content: send_data.encrypted_content,
        iv: send_data.iv,
    };

    // Send new message notification to receiver
    broadcast_message_to_user(connections, receiver_id, message_notification).await;
    
    // Send SENT status update to sender to confirm message was received by server
    let sent_status_update = StatusUpdate {
        message_id: message_id.to_string(),
        status: "SENT".to_string(),
        updated_by: "server".to_string(),
    };
    broadcast_status_update_to_user(connections, sender_id, sent_status_update).await;

    info!("Message sent via WebSocket: {} -> {}, sender notified of SENT status", sender_id, receiver_id);
    Ok(())
}

async fn handle_update_status(
    user_id: Uuid,
    data: serde_json::Value,
    connections: &ConnectionManager,
    state: Arc<AppState>,
) -> Result<(), String> {
    let update_data: UpdateStatusData = serde_json::from_value(data)
        .map_err(|e| format!("Failed to parse update_status data: {}", e))?;

    let message_id = Uuid::parse_str(&update_data.message_id)
        .map_err(|_| "Invalid message_id format".to_string())?;

    let status = update_data.status.trim().to_uppercase();
    if !["SENT", "DELIVERED", "READ", "FAILED"].contains(&status.as_str()) {
        return Err("Invalid status. Must be one of: SENT, DELIVERED, READ, FAILED".to_string());
    }

    info!("Processing status update: message {} to status {} by user {}", message_id, status, user_id);

    // Get message details
    let message_check = match sqlx::query("SELECT receiver_id, sender_id FROM messages WHERE id = $1")
        .bind(message_id)
        .fetch_optional(&state.db)
        .await
    {
        Ok(row) => row,
        Err(e) => {
            return Err(format!("Database error checking message: {}", e));
        }
    };

    let (receiver_id, sender_id) = match message_check {
        Some(row) => {
            let receiver_id = row.try_get::<Uuid, _>("receiver_id")
                .map_err(|_| "Invalid receiver_id in database".to_string())?;
            let sender_id = row.try_get::<Uuid, _>("sender_id")
                .map_err(|_| "Invalid sender_id in database".to_string())?;
            (receiver_id, sender_id)
        }
        None => {
            return Err("Message not found".to_string());
        }
    };

    // Only the receiver can mark a message as read
    if receiver_id != user_id && status == "READ" {
        return Err("Only the message receiver can mark it as read".to_string());
    }

    // Update the message status in database
    let update_result = sqlx::query("UPDATE messages SET status = $1 WHERE id = $2")
        .bind(&status)
        .bind(message_id)
        .execute(&state.db)
        .await;

    match update_result {
        Ok(result) => {
            if result.rows_affected() > 0 {
                info!("Message {} status updated to {} by user {}", message_id, status, user_id);

                // Create status update notification
                let status_update = StatusUpdate {
                    message_id: message_id.to_string(),
                    status: status.clone(),
                    updated_by: user_id.to_string(),
                };

                // Always notify both sender and receiver about status changes
                // This ensures both parties always know the current message status
                broadcast_status_update_to_user(connections, sender_id, status_update.clone()).await;
                broadcast_status_update_to_user(connections, receiver_id, status_update).await;
                
                info!("Broadcasted {} status update for message {} to both sender {} and receiver {}", 
                      status, message_id, sender_id, receiver_id);

                // If status is READ, schedule delayed deletion to ensure all parties received the update
                if status == "READ" {
                    let db_clone = state.db.clone();
                    let message_id_clone = message_id;
                    
                    tokio::spawn(async move {
                        // Wait 5 seconds to ensure all status updates are delivered
                        sleep(Duration::from_secs(5)).await;
                        
                        // Delete the message from database
                        match sqlx::query("DELETE FROM messages WHERE id = $1")
                            .bind(message_id_clone)
                            .execute(&db_clone)
                            .await 
                        {
                            Ok(result) => {
                                if result.rows_affected() > 0 {
                                    info!("Successfully deleted read message {} after 5-second delay", message_id_clone);
                                } else {
                                    info!("Message {} was already deleted during the delay period", message_id_clone);
                                }
                            }
                            Err(e) => {
                                error!("Failed to delete read message {} after delay: {}", message_id_clone, e);
                            }
                        }
                    });
                }
            } else {
                return Err(format!("Message {} not found for status update", message_id));
            }
        }
        Err(e) => {
            return Err(format!("Failed to update message status: {}", e));
        }
    }

    Ok(())
}

pub async fn broadcast_message_to_user(
    connections: &ConnectionManager,
    user_id: Uuid,
    message: MessageNotification,
) {
    if let Some(sender) = connections.get(&user_id) {
        if let Err(e) = sender.send(WSEvent::NewMessage(message)) {
            error!("Failed to send message to user {}: {}", user_id, e);
        }
    } else {
        info!("User {} not connected to WebSocket", user_id);
    }
}

pub async fn broadcast_status_update_to_user(
    connections: &ConnectionManager,
    user_id: Uuid,
    update: StatusUpdate,
) {
    if let Some(sender) = connections.get(&user_id) {
        if let Err(e) = sender.send(WSEvent::StatusUpdate(update.clone())) {
            error!("Failed to send status update to user {}: {}", user_id, e);
        } else {
            info!("Successfully sent status update to user {}: message {} status {}", user_id, update.message_id, update.status);
        }
    } else {
        warn!("User {} not connected to WebSocket for status update: message {} status {}", user_id, update.message_id, update.status);
    }
}

async fn broadcast_to_all(connections: &ConnectionManager, event: WSEvent) {
    for connection in connections.iter() {
        if let Err(e) = connection.value().send(event.clone()) {
            error!("Failed to broadcast to user {}: {}", connection.key(), e);
        }
    }
}

pub fn create_connection_manager() -> ConnectionManager {
    Arc::new(DashMap::new())
}