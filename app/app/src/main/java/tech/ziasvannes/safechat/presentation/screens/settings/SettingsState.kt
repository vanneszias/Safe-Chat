package tech.ziasvannes.safechat.presentation.screens.settings

/** State class for the Settings screen */
data class SettingsState(
        val isDarkMode: Boolean = true,
        val notificationsEnabled: Boolean = true,
        val messageRetentionPeriod: Int = 30, // in days
        val autoDeleteMessages: Boolean = false,
        val encryptDatabase: Boolean = true,
        val isLoading: Boolean = false,
        val error: String? = null
)
