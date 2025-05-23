package tech.ziasvannes.safechat.presentation.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.UUID
import tech.ziasvannes.safechat.presentation.components.EncryptionStatusIndicator
import tech.ziasvannes.safechat.presentation.components.MessageBubble

/**
 * Displays the main chat interface, showing a list of messages, an input field for composing
 * messages, and UI feedback for loading and error states.
 *
 * The chat screen observes state from the provided [ChatViewModel], rendering messages in a
 * reversed scrollable list and providing controls for message input and sending. A loading
 * indicator is shown when messages are being loaded, and an error dialog appears if an error
 * occurs.
 *
 * @param chatSessionId The UUID of the chat session being displayed
 * @param onNavigateBack Callback for when the user presses back
 * @param modifier Optional modifier for styling
 * @param viewModel The view model responsible for chat state and logic
 */
@OptIn(ExperimentalMaterial3Api::class)
/**
 * Composes the main chat screen UI for a given chat session.
 *
 * Displays the message history, input field for composing messages, navigation controls, and encryption status. Handles loading and error overlays as needed.
 *
 * @param chatSessionId The unique identifier of the chat session to display, or null to use the current context.
 * @param onNavigateBack Callback invoked when the user presses the back navigation icon.
 */
@Composable
fun ChatScreen(
        chatSessionId: UUID? = null,
        onNavigateBack: () -> Unit = {},
        modifier: Modifier = Modifier,
        viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // If chatSessionId is provided, load messages and contact for that chat session
    LaunchedEffect(chatSessionId) { chatSessionId?.let { viewModel.loadChat(it) } }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text(state.contactName.ifEmpty { "Chat" }) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Navigate back"
                                )
                            }
                        },
                        actions = {
                            // Display encryption status in the toolbar
                            state.encryptionStatus?.let { status ->
                                EncryptionStatusIndicator(
                                        status = status,
                                        modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        }
                )
            },
            modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Messages List
            LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    reverseLayout = false,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
            ) {
                items(items = state.messages, key = { message -> message.id }) { message ->
                    // Use our new MessageBubble component instead of the basic MessageItem
                    MessageBubble(
                            message = message,
                            isFromCurrentUser = message.senderId == state.currentUserId
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Message Input
            MessageInput(
                    text = state.messageText,
                    onTextChange = { viewModel.onEvent(ChatEvent.UpdateMessageText(it)) },
                    onSendClick = { viewModel.onEvent(ChatEvent.SendMessage(state.messageText)) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
        }
    }

    // Show loading state
    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }

    // Show error state
    state.error?.let { error ->
        AlertDialog(
                onDismissRequest = { viewModel.onEvent(ChatEvent.ClearError) },
                title = { Text("Error") },
                text = { Text(error) },
                confirmButton = {
                    TextButton(onClick = { viewModel.onEvent(ChatEvent.ClearError) }) { Text("OK") }
                }
        )
    }
}

/**
 * Displays a message input field with a send button for composing and sending chat messages.
 *
 * @param text The current input text.
 * @param onTextChange Callback invoked when the input text changes.
 * @param onSendClick Callback invoked when the send button is clicked.
 */
@Composable
private fun MessageInput(
        text: String,
        onTextChange: (String) -> Unit,
        onSendClick: () -> Unit,
        modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = onSendClick, enabled = text.isNotBlank()) { Text("Send") }
    }
}
