package tech.ziasvannes.safechat.presentation.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.ziasvannes.safechat.data.remote.ApiService
import tech.ziasvannes.safechat.data.remote.ProfileResponse
import tech.ziasvannes.safechat.data.remote.UpdateProfileRequest
import tech.ziasvannes.safechat.domain.repository.EncryptionRepository

@HiltViewModel
open class ProfileViewModel
@Inject
constructor(
        private val apiService: ApiService,
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
     * Processes events such as loading the profile, toggling edit mode, updating the user name,
     * saving changes, selecting an avatar, toggling public key visibility, generating a new key
     * pair, and clearing errors.
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
     * Asynchronously fetches the user's profile data from the API and updates the UI state.
     *
     * Sets the loading indicator while retrieving profile information. On success, updates the state with the user's ID, username, and public key. On failure, updates the state with an error message.
     */
    private fun loadProfile() {
        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val profile: ProfileResponse = apiService.getProfile()
                _state.update {
                    it.copy(
                            userId = profile.id,
                            userName = profile.username,
                            userPublicKey = profile.public_key,
                            avatarUrl = profile.avatar, // base64 string
                            isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load profile")
                }
            }
        }
    }

    /**
     * Persists the current profile changes and updates the UI state to reflect saving progress and
     * completion.
     *
     * Sets the loading indicator while saving, disables edit mode upon success, and updates the
     * error state if saving fails.
     */
    private fun saveProfile() {
        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                apiService.updateProfile(
                        UpdateProfileRequest(
                                username = state.value.userName,
                                avatar = state.value.avatarUrl
                        )
                )
                _state.update { it.copy(isLoading = false, isEditMode = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to save profile")
                }
            }
        }
    }

    /**
     * Updates the user's avatar by setting the avatar URL in the profile state.
     *
     * Simulates avatar processing and updates the state with the new avatar URL or an error message
     * if the operation fails.
     *
     * @param uri The URI of the selected avatar image.
     */
    private fun updateAvatar(uri: Uri) {
        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                // Convert image to base64 string (simulate for now)
                // In a real app, load the image and encode as base64
                val base64 = uri.toString() // TODO: Replace with actual base64 encoding
                _state.update { it.copy(avatarUrl = base64, isLoading = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to update avatar")
                }
            }
        }
    }

    /**
     * Asynchronously generates a new encryption key pair and updates the profile state with the new
     * public key.
     *
     * If key generation fails, updates the state with an error message.
     */
    private fun generateNewKeyPair() {
        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                // Generate new keys
                val newPublicKey = encryptionRepository.generateNewKeyPair()
                // Update backend
                apiService.updatePublicKey(
                        tech.ziasvannes.safechat.data.remote.UpdateKeyRequest(newPublicKey)
                )
                _state.update { it.copy(userPublicKey = newPublicKey, isLoading = false) }
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
