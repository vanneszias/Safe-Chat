package tech.ziasvannes.safechat.data.websocket

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import tech.ziasvannes.safechat.session.UserSession
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketManager @Inject constructor(
    private val webSocketService: WebSocketService,
    private val userSession: UserSession
) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var connectionJob: Job? = null
    private var heartbeatJob: Job? = null
    
    val connectionState: StateFlow<ConnectionState> = webSocketService.connectionState
    
    private var isManuallyDisconnected = false
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val baseReconnectDelay = 5000L // 5 seconds
    
    fun connect(serverUrl: String? = null) {
        if (connectionJob?.isActive == true) {
            Log.d(TAG, "Connection already in progress")
            return
        }
        
        val token = userSession.token
        if (token.isNullOrBlank()) {
            Log.e(TAG, "Cannot connect: No valid token available")
            return
        }
        
        isManuallyDisconnected = false
        reconnectAttempts = 0
        
        connectionJob = scope.launch {
            try {
                Log.d(TAG, "Initiating WebSocket connection")
                webSocketService.connect(token, serverUrl)
                
                // Start heartbeat after successful connection
                startHeartbeat()
                
                // Monitor connection state for auto-reconnection
                monitorConnection()
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initiate WebSocket connection", e)
                scheduleReconnect()
            }
        }
    }
    
    fun disconnect() {
        Log.d(TAG, "Manually disconnecting WebSocket")
        isManuallyDisconnected = true
        reconnectAttempts = 0
        
        stopHeartbeat()
        connectionJob?.cancel()
        connectionJob = null
        
        webSocketService.disconnect()
    }
    
    fun sendPing() {
        webSocketService.sendPing()
    }
    
    fun markTyping(recipientId: String) {
        webSocketService.markTyping(recipientId)
    }
    
    private suspend fun monitorConnection() {
        webSocketService.connectionState.collect { state ->
            when (state) {
                ConnectionState.CONNECTED -> {
                    Log.d(TAG, "WebSocket connected successfully")
                    reconnectAttempts = 0
                }
                
                ConnectionState.DISCONNECTED -> {
                    if (!isManuallyDisconnected) {
                        Log.w(TAG, "WebSocket disconnected unexpectedly, scheduling reconnect")
                        stopHeartbeat()
                        scheduleReconnect()
                    }
                }
                
                ConnectionState.CONNECTING -> {
                    Log.d(TAG, "WebSocket connecting...")
                }
                
                ConnectionState.RECONNECTING -> {
                    Log.d(TAG, "WebSocket reconnecting...")
                }
            }
        }
    }
    
    private fun startHeartbeat() {
        stopHeartbeat() // Stop any existing heartbeat
        
        heartbeatJob = scope.launch {
            while (webSocketService.connectionState.value == ConnectionState.CONNECTED) {
                try {
                    delay(HEARTBEAT_INTERVAL)
                    if (webSocketService.connectionState.value == ConnectionState.CONNECTED) {
                        webSocketService.sendPing()
                        Log.d(TAG, "Sent heartbeat ping")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending heartbeat", e)
                    break
                }
            }
        }
    }
    
    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        Log.d(TAG, "Stopped heartbeat")
    }
    
    private fun scheduleReconnect() {
        if (isManuallyDisconnected || reconnectAttempts >= maxReconnectAttempts) {
            Log.w(TAG, "Not scheduling reconnect: manually disconnected=$isManuallyDisconnected, attempts=$reconnectAttempts")
            return
        }
        
        reconnectAttempts++
        val delay = calculateReconnectDelay()
        
        Log.d(TAG, "Scheduling reconnect attempt $reconnectAttempts in ${delay}ms")
        
        scope.launch {
            delay(delay)
            
            if (!isManuallyDisconnected && webSocketService.connectionState.value == ConnectionState.DISCONNECTED) {
                val token = userSession.token
                if (!token.isNullOrBlank()) {
                    Log.d(TAG, "Attempting automatic reconnect (attempt $reconnectAttempts)")
                    webSocketService.connect(token)
                } else {
                    Log.e(TAG, "Cannot reconnect: No valid token available")
                }
            }
        }
    }
    
    private fun calculateReconnectDelay(): Long {
        // Exponential backoff with jitter
        val exponentialDelay = baseReconnectDelay * (1 shl (reconnectAttempts - 1))
        val maxDelay = 60000L // Max 60 seconds
        val delay = minOf(exponentialDelay, maxDelay)
        
        // Add some jitter (random delay between 0-20% of calculated delay)
        val jitter = (delay * 0.2 * Math.random()).toLong()
        return delay + jitter
    }
    
    fun onUserSessionChanged(hasValidSession: Boolean) {
        if (hasValidSession) {
            Log.d(TAG, "User session established, initiating WebSocket connection")
            connect()
        } else {
            Log.d(TAG, "User session invalidated, disconnecting WebSocket")
            disconnect()
        }
    }
    
    fun getConnectionStats(): ConnectionStats {
        return ConnectionStats(
            currentState = webSocketService.connectionState.value,
            reconnectAttempts = reconnectAttempts,
            isManuallyDisconnected = isManuallyDisconnected
        )
    }
    
    companion object {
        private const val TAG = "WebSocketManager"
        private const val HEARTBEAT_INTERVAL = 30000L // 30 seconds
    }
}

data class ConnectionStats(
    val currentState: ConnectionState,
    val reconnectAttempts: Int,
    val isManuallyDisconnected: Boolean
)