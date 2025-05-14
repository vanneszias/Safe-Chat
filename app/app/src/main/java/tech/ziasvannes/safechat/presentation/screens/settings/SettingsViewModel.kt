package tech.ziasvannes.safechat.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
open class SettingsViewModel
@Inject
constructor(
// In a real app, this would depend on repositories or use cases
// private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    open val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        onEvent(SettingsEvent.LoadSettings)
    }

    /**
     * Handles settings-related events and updates the settings state accordingly.
     *
     * Processes events such as loading settings, toggling options, updating retention periods,
     * clearing messages, resetting settings, and clearing errors. Triggers asynchronous operations
     * for actions that require simulated data persistence or state reset.
     *
     * @param event The settings event to handle.
     */
    open fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.LoadSettings -> {
                loadSettings()
            }
            is SettingsEvent.ToggleDarkMode -> {
                _state.update { it.copy(isDarkMode = event.enabled) }
                saveSettings()
            }
            is SettingsEvent.ToggleNotifications -> {
                _state.update { it.copy(notificationsEnabled = event.enabled) }
                saveSettings()
            }
            is SettingsEvent.UpdateRetentionPeriod -> {
                _state.update { it.copy(messageRetentionPeriod = event.days) }
                saveSettings()
            }
            is SettingsEvent.ToggleAutoDeleteMessages -> {
                _state.update { it.copy(autoDeleteMessages = event.enabled) }
                saveSettings()
            }
            is SettingsEvent.ToggleDatabaseEncryption -> {
                _state.update { it.copy(encryptDatabase = event.enabled) }
                saveSettings()
            }
            is SettingsEvent.ClearAllMessages -> {
                clearAllMessages()
            }
            is SettingsEvent.ResetSettings -> {
                resetSettings()
            }
            is SettingsEvent.ClearError -> {
                _state.update { it.copy(error = null) }
            }
        }
    }

    /**
     * Loads the current settings and updates the state with default values or an error message.
     *
     * Sets the loading flag while retrieving settings, then updates the state with default settings
     * values. If an error occurs during loading, updates the state with an error message.
     */
    private fun loadSettings() {
        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                // In a real app, we would load settings from a repository
                // For now, we'll simulate loading with a delay
                kotlinx.coroutines.delay(300)

                // Use default values for now
                _state.update {
                    it.copy(
                            isDarkMode = true,
                            notificationsEnabled = true,
                            messageRetentionPeriod = 30,
                            autoDeleteMessages = false,
                            encryptDatabase = true,
                            isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load settings")
                }
            }
        }
    }

    /**
     * Simulates saving the current settings asynchronously.
     *
     * If an error occurs during the simulated save, updates the state with an error message.
     */
    private fun saveSettings() {
        viewModelScope.launch {
            try {
                // In a real app, we would save settings to a repository
                // For now, we'll just simulate saving with a delay
                kotlinx.coroutines.delay(100)

                // No need to update state since we already updated it in the event handler
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to save settings") }
            }
        }
    }

    /**
     * Simulates clearing all stored messages and updates the loading state.
     *
     * If an error occurs during the operation, updates the state with an error message.
     */
    private fun clearAllMessages() {
        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                // In a real app, we would clear messages through a repository
                // For now, we'll simulate clearing with a delay
                kotlinx.coroutines.delay(500)

                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to clear messages")
                }
            }
        }
    }

    /**
     * Resets the settings state to default values, simulating a reset operation with a delay.
     *
     * Updates the state to indicate loading during the operation. If an error occurs, the state is
     * updated with an error message.
     */
    private fun resetSettings() {
        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                // In a real app, we would reset settings through a repository
                // For now, we'll simulate resetting with a delay
                kotlinx.coroutines.delay(300)

                _state.update {
                    SettingsState(isLoading = false) // Reset to defaults
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to reset settings")
                }
            }
        }
    }
}
