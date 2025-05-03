package tech.ziasvannes.safechat.presentation.screens.settings

/**
 * Events for the Settings screen
 */
sealed class SettingsEvent {
    /**
     * Load settings
     */
    object LoadSettings : SettingsEvent()
    
    /**
     * Toggle dark mode setting
     */
    data class ToggleDarkMode(val enabled: Boolean) : SettingsEvent()
    
    /**
     * Toggle notifications setting
     */
    data class ToggleNotifications(val enabled: Boolean) : SettingsEvent()
    
    /**
     * Update message retention period
     */
    data class UpdateRetentionPeriod(val days: Int) : SettingsEvent()
    
    /**
     * Toggle auto-delete messages setting
     */
    data class ToggleAutoDeleteMessages(val enabled: Boolean) : SettingsEvent()
    
    /**
     * Toggle database encryption setting
     */
    data class ToggleDatabaseEncryption(val enabled: Boolean) : SettingsEvent()
    
    /**
     * Clear all messages
     */
    object ClearAllMessages : SettingsEvent()
    
    /**
     * Reset all settings to defaults
     */
    object ResetSettings : SettingsEvent()
    
    /**
     * Clear any error messages
     */
    object ClearError : SettingsEvent()
}