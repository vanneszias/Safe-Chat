package tech.ziasvannes.safechat.session

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSession @Inject constructor(@ApplicationContext private val context: Context) {
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    }
    
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
        }
    
    var userPublicKey: String?
        get() = prefs.getString(KEY_USER_PUBLIC_KEY, null)
        set(value) {
            prefs.edit().putString(KEY_USER_PUBLIC_KEY, value).apply()
        }
    
    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) {
            prefs.edit().putString(KEY_TOKEN, value).apply()
        }
    
    /**
     * Checks if the user has a valid session
     */
    fun isLoggedIn(): Boolean {
        return !token.isNullOrBlank() && userId != null
    }
    
    /**
     * Clears all session data
     */
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}