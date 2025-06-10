package tech.ziasvannes.safechat.presentation.navigation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import tech.ziasvannes.safechat.data.websocket.ConnectionState
import tech.ziasvannes.safechat.data.websocket.WebSocketManager
import tech.ziasvannes.safechat.session.UserSession
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    val userSession: UserSession,
    private val webSocketManager: WebSocketManager
) : ViewModel() {
    
    val connectionState: StateFlow<ConnectionState> = webSocketManager.connectionState
    
    init {
        // Monitor user session changes and manage WebSocket connection accordingly
        viewModelScope.launch {
            userSession.isLoggedIn.collect { isLoggedIn ->
                Log.d(TAG, "User session changed: isLoggedIn=$isLoggedIn")
                webSocketManager.onUserSessionChanged(isLoggedIn)
            }
        }
    }
    
    fun connectWebSocket(serverUrl: String? = null) {
        Log.d(TAG, "Manually connecting WebSocket")
        webSocketManager.connect(serverUrl)
    }
    
    fun disconnectWebSocket() {
        Log.d(TAG, "Manually disconnecting WebSocket")
        webSocketManager.disconnect()
    }
    
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "SessionViewModel cleared, disconnecting WebSocket")
        webSocketManager.disconnect()
    }
    
    companion object {
        private const val TAG = "SessionViewModel"
    }
}