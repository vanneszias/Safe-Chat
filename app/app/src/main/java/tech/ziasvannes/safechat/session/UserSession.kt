package tech.ziasvannes.safechat.session

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSession @Inject constructor(@ApplicationContext private val context: Context) {
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    }
    
    private val _isLoggedIn = MutableStateFlow(checkLoginState())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_PUBLIC_KEY = "user_public_key"
        private const val KEY_TOKEN = "token"
    }
    
    var userId: UUID?
        get() {
            val userIdString = prefs.getString(KEY_USER_ID, null)
            return userIdString?.let { UUID.fromString(it) }
        }
        set(value) {
            prefs.edit().putString(KEY_USER_ID, value?.toString()).apply()
            updateLoginState()
        }
    
    var userPublicKey: String?
        get() = prefs.getString(KEY_USER_PUBLIC_KEY, null)
        set(value) {
            prefs.edit().putString(KEY_USER_PUBLIC_KEY, value).apply()
            updateLoginState()
        }
    
    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) {
            prefs.edit().putString(KEY_TOKEN, value).apply()
            updateLoginState()
        }
    
    /**
     * Checks if the user has a valid session
     */
    fun checkLoginState(): Boolean {
        return !token.isNullOrBlank() && userId != null
    }
    
    private fun updateLoginState() {
        val newState = checkLoginState()
        if (_isLoggedIn.value != newState) {
            _isLoggedIn.value = newState
        }
    }
    
    /**
     * Clears all session data
     */
    fun clearSession() {
        prefs.edit().clear().apply()
        updateLoginState()
    }
}