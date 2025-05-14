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
        val created_at: String,
        val avatar: String? = null // base64-encoded
)

data class UpdateKeyRequest(val public_key: String)

data class UpdateProfileRequest(
    val username: String? = null,
    val avatar: String? = null // base64-encoded
)

interface ApiService {
    @POST("/auth/register") suspend fun signUp(@Body request: AuthRequest): AuthResponse

    @POST("/auth/login") suspend fun signIn(@Body request: AuthRequest): AuthResponse

    @GET("/profile") suspend fun getProfile(): ProfileResponse

    @PUT("/profile") suspend fun updateProfile(@Body request: UpdateProfileRequest)

    @PUT("/profile/key") suspend fun updatePublicKey(@Body request: UpdateKeyRequest)
}

// Authenticated requests will have the Authorization header added by an OkHttp interceptor
