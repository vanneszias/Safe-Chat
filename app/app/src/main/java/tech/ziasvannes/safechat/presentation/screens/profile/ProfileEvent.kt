package tech.ziasvannes.safechat.presentation.screens.profile

import android.net.Uri

/**
 * Events for the Profile screen
 */
sealed class ProfileEvent {
    /**
     * Load user profile data
     */
    object LoadProfile : ProfileEvent()
    
    /**
     * Toggle edit mode on/off
     */
    object ToggleEditMode : ProfileEvent()
    
    /**
     * Update the user name
     */
    data class OnUserNameChanged(val name: String) : ProfileEvent()
    
    /**
     * Save profile changes
     */
    object SaveProfile : ProfileEvent()
    
    /**
     * Select a new avatar image
     */
    data class OnAvatarSelected(val uri: Uri) : ProfileEvent()
    
    /**
     * Toggle public key visibility
     */
    object ToggleKeyVisibility : ProfileEvent()
    
    /**
     * Copy public key to clipboard
     */
    object CopyPublicKey : ProfileEvent()
    
    /**
     * Generate a new key pair
     */
    object GenerateNewKeyPair : ProfileEvent()
    
    /**
     * Clear any error messages
     */
    object ClearError : ProfileEvent()
}