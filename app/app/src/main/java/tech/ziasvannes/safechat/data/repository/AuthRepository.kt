package tech.ziasvannes.safechat.data.repository

import javax.inject.Inject
import tech.ziasvannes.safechat.data.remote.ApiService
import tech.ziasvannes.safechat.data.remote.AuthRequest
import tech.ziasvannes.safechat.data.remote.AuthResponse
import tech.ziasvannes.safechat.domain.repository.EncryptionRepository
import tech.ziasvannes.safechat.session.UserSession

class AuthRepository
@Inject
constructor(
        private val apiService: ApiService,
        private val encryptionRepository: EncryptionRepository,
        private val userSession: UserSession
) {
    suspend fun signUp(username: String, password: String): AuthResponse {
        val response = apiService.signUp(AuthRequest(username, password))
        userSession.userId = java.util.UUID.fromString(response.id)
        userSession.userPublicKey = response.public_key
        return response
    }

    suspend fun signIn(username: String, password: String): AuthResponse {
        val response = apiService.signIn(AuthRequest(username, password))
        // After login, fetch profile to get id and public_key
        try {
            val profile = apiService.getProfile()
            userSession.userId = java.util.UUID.fromString(profile.id)
            userSession.userPublicKey = profile.public_key
        } catch (_: Exception) {}
        return response
    }
}
