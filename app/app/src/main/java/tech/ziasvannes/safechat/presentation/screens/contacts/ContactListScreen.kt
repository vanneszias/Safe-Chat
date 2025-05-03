package tech.ziasvannes.safechat.presentation.screens.contacts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import tech.ziasvannes.safechat.data.models.Contact
import tech.ziasvannes.safechat.data.models.ContactStatus
import tech.ziasvannes.safechat.domain.repository.ContactRepository
import tech.ziasvannes.safechat.domain.usecase.GetContactsUseCase
import tech.ziasvannes.safechat.presentation.components.ContactListItem
import tech.ziasvannes.safechat.presentation.components.LoadingIndicator
import tech.ziasvannes.safechat.presentation.components.SearchTextField
import tech.ziasvannes.safechat.presentation.theme.SafeChatTheme
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(
    onNavigateToChat: (contactId: UUID) -> Unit,
    onNavigateToAddContact: () -> Unit,
    viewModel: ContactViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show error snackbar if there's an error
    LaunchedEffect(state.error) {
        state.error?.let {
            val result = snackbarHostState.showSnackbar(
                message = it,
                actionLabel = "Dismiss",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed || result == SnackbarResult.Dismissed) {
                viewModel.onEvent(ContactEvent.ClearError)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contacts") },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(ContactEvent.LoadContacts) }) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Refresh Contacts"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.onEvent(ContactEvent.OnAddContactClick)
                    onNavigateToAddContact()
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Contact")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            SearchTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onEvent(ContactEvent.OnSearchQueryChanged(it)) },
                placeholder = "Search contacts",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Loading indicator
            AnimatedVisibility(
                visible = state.isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator(message = "Loading contacts...")
                }
            }

            // Contacts list
            if (!state.isLoading) {
                if (state.filteredContacts.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.searchQuery.isNotEmpty())
                                "No contacts found matching '${state.searchQuery}'"
                            else
                                "No contacts found. Tap + to add a contact.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Contact list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            items = state.filteredContacts,
                            key = { it.id }
                        ) { contact ->
                            ContactListItem(
                                contact = contact,
                                onContactClicked = {
                                    viewModel.onEvent(ContactEvent.OnContactSelected(it))
                                    onNavigateToChat(it.id)
                                }
                            )
                            
                            Divider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ContactListScreenPreview() {
    val mockContacts = listOf(
        Contact(
            id = UUID.randomUUID(),
            name = "Alice Smith",
            publicKey = "dummy_key",
            lastSeen = System.currentTimeMillis(),
            status = ContactStatus.ONLINE,
            avatarUrl = null
        ),
        Contact(
            id = UUID.randomUUID(),
            name = "Bob Johnson",
            publicKey = "dummy_key",
            lastSeen = System.currentTimeMillis() - 5 * 60 * 1000,
            status = ContactStatus.AWAY,
            avatarUrl = null
        ),
        Contact(
            id = UUID.randomUUID(),
            name = "Charlie Brown",
            publicKey = "dummy_key",
            lastSeen = System.currentTimeMillis() - 24 * 60 * 60 * 1000,
            status = ContactStatus.OFFLINE,
            avatarUrl = null
        )
    )
    
    val state = ContactState(
        contacts = mockContacts,
        filteredContacts = mockContacts,
        isLoading = false
    )
    
    SafeChatTheme {
        Surface {
            ContactListScreen(
                onNavigateToChat = {},
                onNavigateToAddContact = {},
                viewModel = FakeContactViewModel(state)
            )
        }
    }
}

// Fake ViewModel for preview
private class FakeContactViewModel(initialState: ContactState) : ContactViewModel(
    getContactsUseCase = FakeGetContactsUseCase(
        contactRepository = TODO()
    ),
    contactRepository = FakeContactRepository()
) {
    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<ContactState> = _state.asStateFlow()

    override fun onEvent(event: ContactEvent) {
        // No-op for preview
    }
}

// Fake implementations for preview
private class FakeGetContactsUseCase(contactRepository: ContactRepository) : GetContactsUseCase(
    contactRepository
) {
    override suspend fun invoke(): Flow<List<Contact>> = flowOf(emptyList())
}

private class FakeContactRepository : ContactRepository {
    override suspend fun getContacts(): Flow<List<Contact>> = flowOf(emptyList())
    override suspend fun addContact(contact: Contact) {}
    override suspend fun updateContact(contact: Contact) {}
    override suspend fun deleteContact(contactId: UUID) {}
    override suspend fun getContactById(id: UUID): Contact? = null
    override suspend fun searchContacts(query: String): Flow<List<Contact>> = flowOf(emptyList())
}