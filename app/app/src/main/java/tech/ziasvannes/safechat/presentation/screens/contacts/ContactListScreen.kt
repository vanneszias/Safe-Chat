package tech.ziasvannes.safechat.presentation.screens.contacts

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.*
import tech.ziasvannes.safechat.presentation.components.ContactListItem
import tech.ziasvannes.safechat.presentation.components.LoadingIndicator
import tech.ziasvannes.safechat.presentation.components.SearchTextField

/**
 * Displays the contact list screen with search, loading, error handling, and navigation actions.
 *
 * Shows a searchable list of contacts, a loading indicator while contacts are loading, and error
 * messages via a snackbar. Allows navigation to a chat screen when a contact is selected and to an
 * add-contact screen via a floating action button.
 *
 * @param onNavigateToChat Callback invoked with the contact ID when a contact is selected.
 * @param onNavigateToAddContact Callback invoked when the add contact button is pressed.
 * @param onNavigateToMe Callback invoked when the 'Me' contact is selected.
 * @param onAddContactClick Callback invoked when the add contact button is pressed.
 */
/**
 * Displays the contact list screen with search, loading, error handling, and navigation actions.
 *
 * Shows a searchable list of contacts, a loading indicator while contacts are loading, and error messages via a snackbar. Provides actions to navigate to the user's profile, refresh the contact list, add a new contact, or open a chat with a selected contact.
 *
 * @param onNavigateToChat Callback invoked with the chat session ID when a contact is selected.
 * @param onNavigateToAddContact Callback invoked to navigate to the add contact screen.
 * @param onNavigateToMe Callback invoked to navigate to the user's profile.
 * @param onAddContactClick Callback invoked when the add contact action is triggered.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(
        onNavigateToChat: (chatSessionId: UUID) -> Unit,
        onNavigateToAddContact: () -> Unit,
        onNavigateToMe: () -> Unit,
        onAddContactClick: () -> Unit,
        viewModel: ContactViewModel = hiltViewModel()
) {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }
        val context = LocalContext.current

        // Show error snackbar if there's an error
        LaunchedEffect(state.error) {
                state.error?.let {
                        val result =
                                snackbarHostState.showSnackbar(
                                        message = it,
                                        actionLabel = "Dismiss",
                                        duration = SnackbarDuration.Short
                                )
                        if (result == SnackbarResult.ActionPerformed ||
                                        result == SnackbarResult.Dismissed
                        ) {
                                viewModel.onEvent(ContactEvent.ClearError)
                        }
                }
        }

        Scaffold(
                topBar = {
                        TopAppBar(
                                title = { Text("Contacts") },
                                actions = {
                                        IconButton(
                                                onClick = { onNavigateToMe() }
                                        ) {
                                                Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = "Profile"
                                                )
                                        }
                                        IconButton(
                                                onClick = {
                                                        viewModel.onEvent(ContactEvent.LoadContacts)
                                                }
                                        ) {
                                                Icon(
                                                        imageVector = Icons.Default.Refresh,
                                                        contentDescription = "Refresh Contacts"
                                                )
                                        }
                                }
                        )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
                Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        // Search bar
                        SearchTextField(
                                value = state.searchQuery,
                                onValueChange = {
                                        viewModel.onEvent(ContactEvent.OnSearchQueryChanged(it))
                                },
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
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                ) { LoadingIndicator(message = "Loading contacts...") }
                        }

                        // Contacts list
                        if (!state.isLoading) {
                                if (state.filteredContacts.isEmpty()) {
                                        // Empty state
                                        Box(
                                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                                contentAlignment = Alignment.Center
                                        ) {
                                                Text(
                                                        text =
                                                                if (state.searchQuery.isNotEmpty())
                                                                        "No contacts found matching '${state.searchQuery}'"
                                                                else
                                                                        "No contacts found. Tap + to add a contact.",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        textAlign = TextAlign.Center,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
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
                                                                        viewModel.onEvent(
                                                                                ContactEvent
                                                                                        .OnContactSelected(
                                                                                                it
                                                                                        )
                                                                        )
                                                                        viewModel
                                                                                .startChatWithContact(
                                                                                        it,
                                                                                        onNavigateToChat
                                                                                )
                                                                }
                                                        )

                                                        Divider(
                                                                modifier =
                                                                        Modifier.padding(
                                                                                horizontal = 16.dp
                                                                        ),
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .surfaceVariant
                                                        )
                                                }
                                        }
                                }
                        }
                }
        }
}
