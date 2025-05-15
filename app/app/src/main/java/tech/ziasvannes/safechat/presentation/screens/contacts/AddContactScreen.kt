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
import tech.ziasvannes.safechat.data.remote.ApiService
import tech.ziasvannes.safechat.domain.repository.ContactRepository

/**
 * Displays the UI for adding a new contact by public key.
 *
 * Shows a top app bar with a back button, an input field for the contact's public key, and an "Add Contact" button. Displays a loading indicator during contact creation and shows error messages in a snackbar. The add button is enabled only when the public key field is not blank.
 *
 * @param onNavigateBack Callback invoked to navigate back after a successful contact addition or when the back button is pressed.
 */
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
                            value = state.publicKey,
                            onValueChange = { viewModel.onPublicKeyChange(it) },
                            label = { Text("Public Key") },
                            modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                            onClick = { viewModel.addContact(onSuccess = onNavigateBack) },
                            enabled = state.publicKey.isNotBlank()
                    ) { Text("Add Contact") }
                }
            }
        }
    }
}

data class AddContactState(
        val publicKey: String = "",
        val isLoading: Boolean = false,
        val error: String? = null
)

@HiltViewModel
class AddContactViewModel
@Inject
constructor(private val contactRepository: ContactRepository, private val apiService: ApiService) :
        ViewModel() {
    private val _state = MutableStateFlow(AddContactState())
    val state: StateFlow<AddContactState> = _state.asStateFlow()

    /**
     * Updates the public key value in the current UI state.
     *
     * @param newKey The new public key input from the user.
     */
    fun onPublicKeyChange(newKey: String) {
        _state.update { it.copy(publicKey = newKey) }
    }
    /**
     * Clears any error message from the current state.
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
    /**
     * Attempts to add a new contact by fetching user details using the provided public key.
     *
     * If the public key is blank, updates the state with an error message. Otherwise, retrieves user information from the API, creates a new contact, and adds it to the repository. On success, invokes the provided callback. Updates the state with error information if the operation fails.
     *
     * @param onSuccess Callback invoked when the contact is successfully added.
     */
    fun addContact(onSuccess: () -> Unit) {
        val current = _state.value
        if (current.publicKey.isBlank()) {
            _state.update { it.copy(error = "Public key is required.") }
            return
        }
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val user = apiService.getUserByPublicKey(current.publicKey)
                val contact =
                        Contact(
                                id = UUID.fromString(user.id),
                                name = user.username,
                                publicKey = user.public_key,
                                lastSeen = System.currentTimeMillis(),
                                status = ContactStatus.OFFLINE,
                                avatar = user.avatar
                        )
                contactRepository.addContact(contact)
                _state.update { it.copy(isLoading = false) }
                onSuccess()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                            isLoading = false,
                            error = e.message ?: "User not found or network error"
                    )
                }
            }
        }
    }
}
