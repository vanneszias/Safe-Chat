package tech.ziasvannes.safechat.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

// Request and response models
// data class AuthRequest(val username: String, val password: String)
// data class AuthResponse(val token: String)

data class ProfileResponse(
        val id: String,
        val username: String,
        val public_key: String,
        val created_at: String
)

data class UpdateKeyRequest(val public_key: String)

interface ApiService {
    @POST("/auth/register") suspend fun signUp(@Body request: AuthRequest): AuthResponse

    @POST("/auth/login") suspend fun signIn(@Body request: AuthRequest): AuthResponse

    @GET("/profile") suspend fun getProfile(): ProfileResponse

    /**
 * Updates the current user's public key.
 *
 * Sends a PUT request to update the user's public key with the provided value.
 *
 * @param request Contains the new public key to be set.
 */
@PUT("/profile/key") suspend fun updatePublicKey(@Body request: UpdateKeyRequest)
}

// Authenticated requests will have the Authorization header added by an OkHttp interceptor
