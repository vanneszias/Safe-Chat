package tech.ziasvannes.safechat.presentation.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import tech.ziasvannes.safechat.data.remote.ApiService
import tech.ziasvannes.safechat.data.repository.AuthRepository
import tech.ziasvannes.safechat.session.UserSession

sealed class AuthResult {
    object Idle : AuthResult()
    object Loading : AuthResult()
    data class Success(val token: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

@HiltViewModel
class AuthViewModel
@Inject
constructor(
        private val authRepository: AuthRepository,
        private val userSession: UserSession,
        private val apiService: ApiService
) : ViewModel() {
    private val _authResult = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val authResult: StateFlow<AuthResult> = _authResult.asStateFlow()

    fun signUp(username: String, password: String) {
        _authResult.value = AuthResult.Loading
        viewModelScope.launch {
            try {
                val response = authRepository.signUp(username, password)
                userSession.userId = UUID.fromString(response.id)
                userSession.userPublicKey = response.public_key
                _authResult.value = AuthResult.Success(response.token)
            } catch (e: Exception) {
                val errorMessage =
                        when (e) {
                            is HttpException -> {
                                val errorBody = e.response()?.errorBody()?.string()?.trim()
                                if (!errorBody.isNullOrBlank()) {
                                    // Try to parse as JSON with 'error' field
                                    try {
                                        val json = JSONObject(errorBody)
                                        json.optString("error", errorBody)
                                    } catch (_: Exception) {
                                        errorBody
                                    }
                                } else {
                                    "HTTP ${e.code()} error"
                                }
                            }
                            is IOException -> "Network error. Please check your connection."
                            else -> e.message ?: "Unknown error"
                        }
                _authResult.value = AuthResult.Error(errorMessage)
            }
        }
    }

    fun signIn(username: String, password: String) {
        _authResult.value = AuthResult.Loading
        viewModelScope.launch {
            try {
                val response = authRepository.signIn(username, password)
                fetchAndSetUserIdAndKey()
                _authResult.value = AuthResult.Success(response.token)
            } catch (e: Exception) {
                val errorMessage =
                        when (e) {
                            is HttpException -> {
                                val errorBody = e.response()?.errorBody()?.string()?.trim()
                                if (!errorBody.isNullOrBlank()) {
                                    // Try to parse as JSON with 'error' field
                                    try {
                                        val json = JSONObject(errorBody)
                                        json.optString("error", errorBody)
                                    } catch (_: Exception) {
                                        errorBody
                                    }
                                } else {
                                    "HTTP ${e.code()} error"
                                }
                            }
                            is IOException -> "Network error. Please check your connection."
                            else -> e.message ?: "Unknown error"
                        }
                _authResult.value = AuthResult.Error(errorMessage)
            }
        }
    }

    private suspend fun fetchAndSetUserIdAndKey() {
        try {
            val profile = apiService.getProfile()
            userSession.userId = UUID.fromString(profile.id)
            userSession.userPublicKey = profile.public_key
        } catch (e: Exception) {
            // Handle error if needed
        }
    }

    fun reset() {
        _authResult.value = AuthResult.Idle
    }
}
