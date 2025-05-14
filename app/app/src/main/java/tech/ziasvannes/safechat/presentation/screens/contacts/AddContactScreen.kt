package tech.ziasvannes.safechat.presentation.screens.contacts

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.ziasvannes.safechat.data.models.Contact
import tech.ziasvannes.safechat.data.models.ContactStatus
import tech.ziasvannes.safechat.domain.repository.ContactRepository

@OptIn(ExperimentalMaterial3Api::class)
/**
 * Displays the UI for adding a new contact, including input fields for name, public key, and
 * optional avatar URL.
 *
 * Shows a loading indicator during contact creation and displays error messages in a snackbar. The
 * save button is enabled only when both the name and public key fields are filled.
 *
 * @param onNavigateBack Callback invoked to navigate back after a successful contact addition or
 * when the back button is pressed.
 */
@Composable
fun AddContactScreen(onNavigateBack: () -> Unit, viewModel: AddContactViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show error snackbar if needed
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Add Contact") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                            value = state.name,
                            onValueChange = { viewModel.onNameChange(it) },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                            value = state.publicKey,
                            onValueChange = { viewModel.onPublicKeyChange(it) },
                            label = { Text("Public Key") },
                            modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                            value = state.avatarUrl,
                            onValueChange = { viewModel.onAvatarUrlChange(it) },
                            label = { Text("Avatar URL (optional)") },
                            modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                            onClick = { viewModel.addContact(onSuccess = onNavigateBack) },
                            enabled = state.name.isNotBlank() && state.publicKey.isNotBlank()
                    ) { Text("Save Contact") }
                }
            }
        }
    }
}

// State for AddContactScreen
data class AddContactState(
        val name: String = "",
        val publicKey: String = "",
        val avatarUrl: String = "",
        val isLoading: Boolean = false,
        val error: String? = null
)

@HiltViewModel
class AddContactViewModel @Inject constructor(private val contactRepository: ContactRepository) :
        ViewModel() {
    private val _state = MutableStateFlow(AddContactState())
    val state: StateFlow<AddContactState> = _state.asStateFlow()

    /**
     * Updates the contact name in the current UI state.
     *
     * @param newName The new name to set for the contact.
     */
    fun onNameChange(newName: String) {
        _state.update { it.copy(name = newName) }
    }
    /**
     * Updates the public key field in the current add contact state.
     *
     * @param newKey The new public key value to set.
     */
    fun onPublicKeyChange(newKey: String) {
        _state.update { it.copy(publicKey = newKey) }
    }
    /**
     * Updates the avatar URL field in the current add contact state.
     *
     * @param newUrl The new avatar URL input by the user.
     */
    fun onAvatarUrlChange(newUrl: String) {
        _state.update { it.copy(avatarUrl = newUrl) }
    }
    /** Clears the current error message from the state. */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
    /**
     * Attempts to add a new contact using the current form state.
     *
     * Validates that the name and public key fields are not blank. If validation passes, creates a
     * new contact and saves it asynchronously via the repository. Updates the UI state to reflect
     * loading and error conditions. Invokes the provided callback upon successful addition.
     *
     * @param onSuccess Callback invoked when the contact is successfully added.
     */
    fun addContact(onSuccess: () -> Unit) {
        val current = _state.value
        if (current.name.isBlank() || current.publicKey.isBlank()) {
            _state.update { it.copy(error = "Name and public key are required.") }
            return
        }
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val contact =
                        Contact(
                                id = UUID.randomUUID(),
                                name = current.name,
                                publicKey = current.publicKey,
                                lastSeen = System.currentTimeMillis(),
                                status = ContactStatus.OFFLINE,
                                avatarUrl = current.avatarUrl.takeIf { it.isNotBlank() }
                        )
                contactRepository.addContact(contact)
                _state.update { it.copy(isLoading = false) }
                onSuccess()
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to add contact")
                }
            }
        }
    }
}
