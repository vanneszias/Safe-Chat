package tech.ziasvannes.safechat.data.remote

data class AuthRequest(val username: String, val password: String)

data class AuthResponse(
        val token: String,
        val id: String, // UUID string
        val public_key: String
)
