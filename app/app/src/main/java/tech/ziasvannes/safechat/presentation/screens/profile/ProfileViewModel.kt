package tech.ziasvannes.safechat.presentation.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.ziasvannes.safechat.domain.repository.EncryptionRepository
import javax.inject.Inject

@HiltViewModel
open class ProfileViewModel @Inject constructor(
    private val encryptionRepository: EncryptionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    open val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        onEvent(ProfileEvent.LoadProfile)
    }

    /**
     * Handles profile-related events and updates the profile state accordingly.
     *
     * Processes events such as loading profile data, toggling edit mode, updating the username, saving changes, selecting an avatar, toggling key visibility, generating a new key pair, and clearing errors.
     *
     * @param event The profile event to handle.
     */
    open fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.LoadProfile -> {
                loadProfile()
            }
            is ProfileEvent.ToggleEditMode -> {
                _state.update { it.copy(isEditMode = !it.isEditMode) }
            }
            is ProfileEvent.OnUserNameChanged -> {
                _state.update { it.copy(userName = event.name) }
            }
            is ProfileEvent.SaveProfile -> {
                saveProfile()
            }
            is ProfileEvent.OnAvatarSelected -> {
                updateAvatar(event.uri)
            }
            is ProfileEvent.ToggleKeyVisibility -> {
                _state.update { it.copy(isKeyVisible = !it.isKeyVisible) }
            }
            is ProfileEvent.CopyPublicKey -> {
                // Copying to clipboard will be handled by the UI
            }
            is ProfileEvent.GenerateNewKeyPair -> {
                generateNewKeyPair()
            }
            is ProfileEvent.ClearError -> {
                _state.update { it.copy(error = null) }
            }
        }
    }

    /**
     * Loads the user's profile data and updates the state with the username and public key.
     *
     * Sets the loading state while fetching data and updates the state with an error message if loading fails.
     */
    private fun loadProfile() {
        _state.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            try {
                // In a real app, this would come from a user repository
                // For now, we'll simulate loading profile data with a delay
                kotlinx.coroutines.delay(500)
                
                // Get the public key from the encryption repository
                val publicKey = encryptionRepository.getCurrentPublicKey() ?: "No key available"
                
                _state.update {
                    it.copy(
                        userName = "Current User",  // This would come from user preferences
                        userPublicKey = publicKey,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load profile"
                    )
                }
            }
        }
    }

    /**
     * Simulates saving the user's profile data and updates the state to reflect the result.
     *
     * Sets the loading state while saving, exits edit mode on success, and updates the error state if saving fails.
     */
    private fun saveProfile() {
        _state.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            try {
                // In a real app, this would save to a user repository
                // For now, we'll simulate saving with a delay
                kotlinx.coroutines.delay(500)
                
                _state.update {
                    it.copy(
                        isLoading = false,
                        isEditMode = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to save profile"
                    )
                }
            }
        }
    }

    /**
     * Updates the user's avatar by processing the provided URI and updating the profile state.
     *
     * Sets the loading state during processing and updates the avatar URL on success, or sets an error message if the operation fails.
     *
     * @param uri The URI of the new avatar image.
     */
    private fun updateAvatar(uri: Uri) {
        _state.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            try {
                // In a real app, this would upload the image to storage
                // For now, we'll simulate processing with a delay
                kotlinx.coroutines.delay(500)
                
                _state.update {
                    it.copy(
                        avatarUrl = uri.toString(),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to update avatar"
                    )
                }
            }
        }
    }

    /**
     * Asynchronously generates a new encryption key pair and updates the user's public key in the profile state.
     *
     * If key generation fails, updates the state with an error message.
     */
    private fun generateNewKeyPair() {
        _state.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            try {
                // Generate new keys
                val newPublicKey = encryptionRepository.generateNewKeyPair()
                
                _state.update {
                    it.copy(
                        userPublicKey = newPublicKey,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to generate new key pair"
                    )
                }
            }
        }
    }
}