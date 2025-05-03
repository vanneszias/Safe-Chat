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