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
    /**
     * Registers a new user with the provided username and password, updates the user session, and
     * returns the authentication response.
     *
     * The user session is updated with the user's ID, public key, and authentication token from the
     * response.
     *
     * @param username The username for the new account.
     * @param password The password for the new account.
     * @return The authentication response containing user and session details.
     */
    suspend fun signUp(username: String, password: String): AuthResponse {
        val response = apiService.signUp(AuthRequest(username, password))
        userSession.userId = java.util.UUID.fromString(response.id)
        userSession.userPublicKey = response.public_key
        userSession.token = response.token
        // Generate and upload our own key pair after registration
        val newKeyPair = encryptionRepository.generateKeyPair()
        val myPublicKeyB64 = java.util.Base64.getEncoder().encodeToString(newKeyPair.public.encoded)
        // Update backend with our public key
        apiService.updatePublicKey(
                tech.ziasvannes.safechat.data.remote.UpdateKeyRequest(myPublicKeyB64)
        )
        userSession.userPublicKey = myPublicKeyB64
        return response
    }

    /**
     * Authenticates a user with the provided credentials and updates the user session.
     *
     * Sends a sign-in request using the given username and password. On success, stores the
     * authentication token in the user session. Attempts to retrieve the user's profile to update
     * the session with the user's ID and public key. If profile retrieval fails, the session will
     * only contain the token.
     *
     * @param username The user's username.
     * @param password The user's password.
     * @return The authentication response containing the token and related data.
     */
    suspend fun signIn(username: String, password: String): AuthResponse {
        val response = apiService.signIn(AuthRequest(username, password))
        userSession.token = response.token
        // After login, fetch profile to get id and public_key
        try {
            val profile = apiService.getProfile()
            userSession.userId = java.util.UUID.fromString(profile.id)
            userSession.userPublicKey = profile.public_key
        } catch (_: Exception) {}
        return response
    }
}
