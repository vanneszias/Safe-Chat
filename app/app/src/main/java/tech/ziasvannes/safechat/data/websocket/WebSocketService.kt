package tech.ziasvannes.safechat.data.websocket

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

data class WebSocketMessage(val message_type: String, val data: Map<String, Any>)

data class MessageNotification(
        val id: String,
        val timestamp: String,
        val sender_id: String,
        val receiver_id: String,
        val status: String,
        val type: String,
        val encrypted_content: String,
        val iv: String
)

data class StatusUpdateData(val message_id: String, val status: String, val updated_by: String)

data class UserStatus(val user_id: String)

sealed class WebSocketEvent {
    data class NewMessage(val message: MessageNotification) : WebSocketEvent()
    data class StatusUpdate(val update: StatusUpdateData) : WebSocketEvent()
    data class UserOnline(val user: UserStatus) : WebSocketEvent()
    data class UserOffline(val user: UserStatus) : WebSocketEvent()
    object Connected : WebSocketEvent()
    object Disconnected : WebSocketEvent()
    data class Error(val error: String) : WebSocketEvent()
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}

@Singleton
class WebSocketService @Inject constructor() {

    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private var currentToken: String? = null
    private var baseUrl: String = "wss://safechat.ziasvannes.tech"

    private val okHttpClient =
            OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .build()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _events = Channel<WebSocketEvent>(Channel.UNLIMITED)
    val events: Flow<WebSocketEvent> = _events.receiveAsFlow()

    private val webSocketListener =
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "WebSocket connection opened")
                    _connectionState.value = ConnectionState.CONNECTED
                    _events.trySend(WebSocketEvent.Connected)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    Log.d(TAG, "Received WebSocket message: $text")
                    handleMessage(text)
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    Log.d(TAG, "Received WebSocket binary message")
                    // Handle binary messages if needed
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WebSocket closing: $code - $reason")
                    webSocket.close(1000, null)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WebSocket closed: $code - $reason")
                    _connectionState.value = ConnectionState.DISCONNECTED
                    _events.trySend(WebSocketEvent.Disconnected)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "WebSocket failure", t)
                    _connectionState.value = ConnectionState.DISCONNECTED
                    _events.trySend(WebSocketEvent.Error(t.message ?: "Unknown WebSocket error"))

                    // Attempt to reconnect after a delay
                    attemptReconnect()
                }
            }

    fun connect(token: String, serverUrl: String? = null) {
        if (_connectionState.value == ConnectionState.CONNECTED) {
            Log.d(TAG, "Already connected to WebSocket")
            return
        }

        currentToken = token
        if (serverUrl != null) {
            baseUrl = serverUrl.replace("http://", "ws://").replace("https://", "wss://")
        }

        Log.d(TAG, "Connecting to WebSocket: $baseUrl/ws?token=$token")
        _connectionState.value = ConnectionState.CONNECTING

        val request = Request.Builder().url("$baseUrl/ws?token=$token").build()

        webSocket = okHttpClient.newWebSocket(request, webSocketListener)
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting WebSocket")
        webSocket?.close(1000, "Disconnecting")
        webSocket = null
        currentToken = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun sendMessage(messageType: String, data: Map<String, Any> = emptyMap()) {
        val message = WebSocketMessage(messageType, data)
        val json = gson.toJson(message)

        val success = webSocket?.send(json) ?: false
        if (!success) {
            Log.e(TAG, "Failed to send WebSocket message: $json")
            _events.trySend(WebSocketEvent.Error("Failed to send message"))
        }
    }

    fun sendPing() {
        sendMessage("ping")
    }

    fun markTyping(recipientId: String) {
        sendMessage("mark_typing", mapOf("recipient_id" to recipientId))
    }

    fun sendChatMessage(receiverId: String, type: String, encryptedContent: String, iv: String) {
        sendMessage("send_message", mapOf(
            "receiver_id" to receiverId,
            "type" to type,
            "encrypted_content" to encryptedContent,
            "iv" to iv
        ))
    }

    fun updateMessageStatus(messageId: String, status: String) {
        sendMessage("update_status", mapOf(
            "message_id" to messageId,
            "status" to status
        ))
    }

    private fun handleMessage(text: String) {
        try {
            val message = gson.fromJson(text, WebSocketMessage::class.java)

            when (message.message_type) {
                "new_message" -> {
                    val messageData =
                            gson.fromJson(
                                    gson.toJsonTree(message.data),
                                    MessageNotification::class.java
                            )
                    _events.trySend(WebSocketEvent.NewMessage(messageData))
                }
                "status_update" -> {
                    val statusData =
                            gson.fromJson(
                                    gson.toJsonTree(message.data),
                                    StatusUpdateData::class.java
                            )
                    _events.trySend(WebSocketEvent.StatusUpdate(statusData))
                }
                "user_online" -> {
                    val userData = message.data["user_id"] as? String
                    if (userData != null) {
                        _events.trySend(WebSocketEvent.UserOnline(UserStatus(userData)))
                    }
                }
                "user_offline" -> {
                    val userData = message.data["user_id"] as? String
                    if (userData != null) {
                        _events.trySend(WebSocketEvent.UserOffline(UserStatus(userData)))
                    }
                }
                else -> {
                    Log.w(TAG, "Unknown message type: ${message.message_type}")
                }
            }
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Failed to parse WebSocket message: $text", e)
            _events.trySend(WebSocketEvent.Error("Failed to parse message"))
        } catch (e: Exception) {
            Log.e(TAG, "Error handling WebSocket message: $text", e)
            _events.trySend(WebSocketEvent.Error("Error handling message: ${e.message}"))
        }
    }

    private fun attemptReconnect() {
        val token = currentToken
        if (token != null && _connectionState.value != ConnectionState.CONNECTED) {
            Log.d(TAG, "Attempting to reconnect WebSocket")
            _connectionState.value = ConnectionState.RECONNECTING

            // Delay before reconnecting
            okHttpClient.dispatcher.executorService.execute {
                Thread.sleep(5000) // 5 second delay
                if (_connectionState.value == ConnectionState.RECONNECTING) {
                    connect(token)
                }
            }
        }
    }

    companion object {
        private const val TAG = "WebSocketService"
    }
}
