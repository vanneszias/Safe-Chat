package tech.ziasvannes.safechat.presentation.screens.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.ByteArrayOutputStream
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
        private val encryptionRepository: EncryptionRepository,
        private val userSession: tech.ziasvannes.safechat.session.UserSession
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    open val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        onEvent(ProfileEvent.LoadProfile)
    }

    /**
     * Processes profile-related events and updates the profile state based on the event type.
     *
     * Handles actions such as loading profile data, toggling edit mode, updating the username,
     * saving changes, selecting a new avatar, toggling public key visibility, generating a new key
     * pair, and clearing error messages.
     *
     * @param event The profile event to process.
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
                updateAvatar(event.context, event.uri)
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
            is ProfileEvent.Logout -> {
                logout()
            }
        }
    }

    /**
     * Fetches the user's profile data from the API and updates the UI state.
     *
     * Initiates an asynchronous request to retrieve the user's ID, username, public key, and avatar
     * (as a base64 string). Updates the state to indicate loading, success, or error based on the
     * outcome.
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
                            avatar = profile.avatar, // base64 string
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
     * Persists the current user's profile changes to the backend and updates the UI state.
     *
     * Sets a loading state during the operation. On success, disables edit mode; on failure,
     * updates the error message in the state.
     */
    private fun saveProfile() {
        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                apiService.updateProfile(
                        UpdateProfileRequest(
                                username = state.value.userName,
                                avatar = state.value.avatar
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
     * Converts the provided image URI to a base64-encoded string and updates the user's avatar in
     * the profile state.
     *
     * If encoding fails, updates the state with an error message.
     *
     * @param context Context used to access image decoding resources.
     * @param uri URI of the selected avatar image.
     */
    private fun updateAvatar(context: Context, uri: Uri) {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val base64 = uriToBase64(context, uri)
                if (base64 != null) {
                    _state.update { it.copy(avatar = base64, isLoading = false) }
                } else {
                    _state.update { it.copy(isLoading = false, error = "Failed to encode image") }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to update avatar")
                }
            }
        }
    }

    /**
     * Generates a new encryption key pair and updates the user's public key in both the backend and
     * local profile state.
     *
     * Sets an error message in the profile state if key generation or backend update fails.
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

    /**
     * Converts an image at the specified URI to a base64-encoded JPEG string.
     *
     * Loads the image from the provided URI, compresses it as a JPEG at 90% quality, and encodes
     * the result as a base64 string without line wraps.
     *
     * @param context Context used to access the content resolver.
     * @param uri URI of the image to convert.
     * @return Base64-encoded JPEG string, or null if the conversion fails.
     */
    @Suppress("DEPRECATION")
    private fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val bitmap =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        // Use ImageDecoder for API 28+
                        val source = ImageDecoder.createSource(context.contentResolver, uri)
                        ImageDecoder.decodeBitmap(source)
                    } else {
                        // Use deprecated method for older versions
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    /** Logs out the current user by clearing the user session data. */
    private fun logout() {
        userSession.clearSession()
    }
}
