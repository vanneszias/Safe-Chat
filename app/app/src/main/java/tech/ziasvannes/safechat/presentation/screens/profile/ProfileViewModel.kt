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
import tech.ziasvannes.safechat.data.models.Contact
import tech.ziasvannes.safechat.domain.repository.ContactRepository
import tech.ziasvannes.safechat.domain.repository.EncryptionRepository
import java.util.UUID

@HiltViewModel
open class ProfileViewModel
@Inject
constructor(
        private val encryptionRepository: EncryptionRepository,
        private val contactRepository: ContactRepository
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
     * Asynchronously loads the user's profile data and updates the UI state.
     *
     * Fetches the user's contact information and public key from the repositories, updating the state with the retrieved name, public key, and avatar URL. If loading fails, updates the state with an error message.
     */
    private fun loadProfile() {
        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val selfId = UUID.fromString("00000000-0000-0000-0000-000000000000")
                val contact = contactRepository.getContactById(selfId)
                val publicKey =
                        encryptionRepository.getCurrentPublicKey()
                                ?: contact?.publicKey ?: "No key available"
                _state.update {
                    it.copy(
                            userName = contact?.name ?: "",
                            userPublicKey = publicKey,
                            avatarUrl = contact?.avatarUrl,
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
     * Saves the current profile information to the contact repository and updates the UI state.
     *
     * Sets the loading state during the save operation, disables edit mode upon successful save, and updates the error state if saving fails.
     */
    private fun saveProfile() {
        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val selfId = UUID.fromString("00000000-0000-0000-0000-000000000000")
                val contact =
                        Contact(
                                id = selfId,
                                name = state.value.userName,
                                publicKey = state.value.userPublicKey,
                                lastSeen = System.currentTimeMillis(),
                                status = tech.ziasvannes.safechat.data.models.ContactStatus.ONLINE,
                                avatarUrl = state.value.avatarUrl
                        )
                contactRepository.updateContact(contact)
                _state.update { it.copy(isLoading = false, isEditMode = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to save profile")
                }
            }
        }
    }

    /**
     * Updates the user's avatar in the profile state using the provided image URI.
     *
     * Simulates avatar processing and sets the avatar URL in the state, or updates the state with an error message if the operation fails.
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

                _state.update { it.copy(avatarUrl = uri.toString(), isLoading = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to update avatar")
                }
            }
        }
    }

    /**
     * Initiates asynchronous generation of a new encryption key pair and updates the profile state with the resulting public key.
     *
     * If key generation fails, the profile state is updated with an error message.
     */
    private fun generateNewKeyPair() {
        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                // Generate new keys
                val newPublicKey = encryptionRepository.generateNewKeyPair()

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
