package tech.ziasvannes.safechat.presentation.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import tech.ziasvannes.safechat.data.models.Message

/**
 * Displays the chat interface, including the message list, input field, loading indicator, and error dialog.
 *
 * Observes chat state from the provided view model to render messages, handle user input, and display UI feedback such as loading and error states.
 */
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Messages List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            reverseLayout = true
        ) {
            items(state.messages) { message ->
                MessageItem(message = message)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Message Input
        MessageInput(
            text = state.messageText,
            onTextChange = { viewModel.onEvent(ChatEvent.UpdateMessageText(it)) },
            onSendClick = { viewModel.onEvent(ChatEvent.SendMessage(state.messageText)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }

    // Show loading state
    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
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
                TextButton(onClick = { viewModel.onEvent(ChatEvent.ClearError) }) {
                    Text("OK")
                }
            }
        )
    }
}

/**
 * Displays a single chat message within a card, showing its content and status.
 *
 * @param message The chat message to display.
 * @param modifier Optional modifier for customizing the layout or appearance.
 */
@Composable
private fun MessageItem(
    message: Message,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Status: ${message.status}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Displays a horizontal input row for composing and sending chat messages.
 *
 * Shows a text field for message entry and a send button, which is enabled only when the input is not blank.
 *
 * @param text The current message input text.
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
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Type a message...") }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onSendClick,
            enabled = text.isNotBlank()
        ) {
            Text("Send")
        }
    }
}