package tech.ziasvannes.safechat.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

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

data class UserResponse(
        val id: String,
        val username: String,
        val public_key: String,
        val created_at: String,
        val avatar: String? = null
)

data class SendMessageRequest(
        val content: String,
        val receiver_id: String,
        val type: String = "Text", // or "Image", "File"
        val encrypted_content: String, // base64
        val iv: String // base64
)

data class MessageResponse(
        val id: String,
        val content: String,
        val timestamp: Long,
        val sender_id: String,
        val receiver_id: String,
        val status: String,
        val type: String,
        val encrypted_content: String, // base64
        val iv: String // base64
)

interface ApiService {
        /**
         * Registers a new user with the provided authentication credentials.
         *
         * @param request The authentication details required for registration.
         * @return The authentication response containing user and token information.
         */
        @POST("/auth/register") suspend fun signUp(@Body request: AuthRequest): AuthResponse

        /**
         * Authenticates a user with the provided credentials.
         *
         * Sends a POST request to the `/auth/login` endpoint with the authentication details and
         * returns an authentication response containing tokens or user information.
         *
         * @param request The authentication credentials for sign-in.
         * @return The authentication response with tokens or user data.
         */
        @POST("/auth/login") suspend fun signIn(@Body request: AuthRequest): AuthResponse

        /**
         * Retrieves the current user's profile information.
         *
         * @return The user's profile details, including ID, username, public key, creation date,
         * and optional avatar.
         */
        @GET("/profile") suspend fun getProfile(): ProfileResponse

        /**
         * Updates the current user's profile with the provided username and/or avatar.
         *
         * Sends a PUT request to the `/profile` endpoint with optional updates for the user's
         * display name and avatar image.
         *
         * @param request Contains the new username and/or avatar (base64-encoded), both optional.
         */
        @PUT("/profile") suspend fun updateProfile(@Body request: UpdateProfileRequest)

        /**
         * Updates the user's public key on the server.
         *
         * Sends a PUT request to the `/profile/key` endpoint with the new public key.
         */
        @PUT("/profile/key") suspend fun updatePublicKey(@Body request: UpdateKeyRequest)

        @GET("/user/{public_key}")
        suspend fun getUserByPublicKey(@Path("public_key") publicKey: String): UserResponse

        @POST("/messages")
        suspend fun sendMessage(@Body request: SendMessageRequest): MessageResponse

        @GET("/messages/{user_id}")
        suspend fun getMessages(
                @retrofit2.http.Path("user_id") userId: String
        ): List<MessageResponse>
}

// Authenticated requests will have the Authorization header added by an OkHttp interceptor
