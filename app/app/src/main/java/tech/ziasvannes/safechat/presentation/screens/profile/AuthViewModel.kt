package tech.ziasvannes.safechat.presentation.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.ziasvannes.safechat.data.remote.ApiService
import tech.ziasvannes.safechat.data.repository.AuthRepository
import tech.ziasvannes.safechat.session.UserSession

sealed class AuthResult {
    object Idle : AuthResult()
    object Loading : AuthResult()
    data class Success(val token: String) : AuthResult()
    data class Registered(val message: String) : AuthResult()
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
                response.id?.let { userSession.userId = UUID.fromString(it) }
                response.public_key?.let { userSession.userPublicKey = it }
                _authResult.value = AuthResult.Registered(response.token)
            } catch (e: Exception) {
                _authResult.value = AuthResult.Error(e.message ?: "Unknown error")
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
                _authResult.value = AuthResult.Error(e.message ?: "Unknown error")
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
