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

data class UpdateMessageStatusRequest(
        val status: String // "SENT", "DELIVERED", "READ", "FAILED"
)

interface ApiService {
        /**
         * Registers a new user with the provided authentication credentials.
         *
         * Sends a registration request to the server and returns authentication details for the
         * newly created user.
         *
         * @param request The authentication credentials for user registration.
         * @return The authentication response containing user and token information.
         */
        @POST("/auth/register") suspend fun signUp(@Body request: AuthRequest): AuthResponse

        /**
         * Authenticates a user and returns authentication details.
         *
         * Sends a POST request to the `/auth/login` endpoint with user credentials and returns an
         * authentication response containing tokens or user information.
         *
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
         * Updates the current user's profile with a new username and/or avatar.
         *
         * Sends a PUT request to update the user's display name and/or avatar image. The avatar
         * should be provided as a base64-encoded string if included.
         *
         * @param request Object containing optional fields for the new username and avatar.
         */
        @PUT("/profile") suspend fun updateProfile(@Body request: UpdateProfileRequest)

        /**
         * Updates the user's public key on the server.
         *
         * Sends a PUT request to the `/profile/key` endpoint with the provided public key.
         */
        @PUT("/profile/key") suspend fun updatePublicKey(@Body request: UpdateKeyRequest)

        /**
         * Retrieves user information by public key.
         *
         * @param publicKey The public key of the user to look up.
         * @return The user's details associated with the provided public key.
         */
        @GET("/user/{public_key}")
        suspend fun getUserByPublicKey(@Path("public_key") publicKey: String): UserResponse

        /**
         * Sends a message to another user.
         *
         * @param request The message details, including content, recipient, type, and encrypted
         * payload.
         * @return The details of the sent message as returned by the server.
         */
        @POST("/messages")
        suspend fun sendMessage(@Body request: SendMessageRequest): MessageResponse

        /**
         * Retrieves the list of messages exchanged with the specified user.
         *
         * @param userId The unique identifier of the user whose messages are to be fetched.
         * @return A list of message responses representing the conversation with the specified
         * user.
         */
        @GET("/messages/{user_id}")
        suspend fun getMessages(
                @retrofit2.http.Path("user_id") userId: String
        ): List<MessageResponse>

        /**
         * Updates the status of a specific message.
         *
         * @param messageId The unique identifier of the message to update.
         * @param request The new status for the message.
         */
        @PUT("/messages/{message_id}/status")
        suspend fun updateMessageStatus(
                @retrofit2.http.Path("message_id") messageId: String,
                @Body request: UpdateMessageStatusRequest
        )
}

// Authenticated requests will have the Authorization header added by an OkHttp interceptor
