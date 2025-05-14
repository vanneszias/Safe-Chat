package tech.ziasvannes.safechat.presentation.screens.profile

/**
 * State class for the Profile screen
 */
data class ProfileState(
    val userId: String = "",
    val userName: String = "",
    val userPublicKey: String = "",
    val avatarUrl: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false,
    val isKeyVisible: Boolean = false
)