package tech.ziasvannes.safechat.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.UUID
import kotlinx.coroutines.launch
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
/**
 * Composes the main chat screen UI for a given chat session.
 *
 * Displays the message history, input field for composing messages, navigation controls, and
 * encryption status. Handles loading and error overlays as needed.
 *
 * @param chatSessionId The unique identifier of the chat session to display, or null to use the
 * current context.
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
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val isKeyboardOpen = WindowInsets.isImeVisible

    // If chatSessionId is provided, load messages and contact for that chat session
    LaunchedEffect(chatSessionId) { chatSessionId?.let { viewModel.loadChat(it) } }

    // Enhanced scroll logic that prioritizes bottom-sticking during loading
    // With reverseLayout=true, index 0 is the newest message (bottom)
    LaunchedEffect(isKeyboardOpen, state.messages.size, state.isLoading) {
        if (state.messages.isNotEmpty()) {
            coroutineScope.launch {
                when {
                    // Always scroll to bottom (index 0 with reverseLayout) when loading
                    state.isLoading -> {
                        listState.scrollToItem(0)
                    }
                    // If keyboard is open, always scroll to the newest message (index 0)
                    isKeyboardOpen -> {
                        listState.scrollToItem(0)
                    }
                    // If keyboard is closed and not loading, use the smart auto-scroll logic
                    else -> {
                        val firstVisibleIndex =
                                listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
                        val isNearBottom =
                                firstVisibleIndex <= 2 // User is within 2 messages of newest

                        if (isNearBottom || state.messages.size == 1) {
                            listState.animateScrollToItem(0) // Scroll to newest message
                        }
                    }
                }
            }
        }
    }

    // Additional effect to ensure we stick to bottom when transitioning from loading to loaded
    LaunchedEffect(state.isLoading) {
        // When loading finishes and we have messages, ensure we're at the newest message
        if (!state.isLoading && state.messages.isNotEmpty()) {
            coroutineScope.launch { listState.animateScrollToItem(0) }
        }
    }

    // Mark messages as read when chat is loaded and user can see messages
    LaunchedEffect(chatSessionId, state.messages.size) {
        if (chatSessionId != null && state.messages.isNotEmpty() && !state.isLoading) {
            // Only mark as read if there are unread received messages
            val hasUnreadMessages =
                    state.messages.any { message ->
                        message.receiverId == state.currentUserId &&
                                message.status !=
                                        tech.ziasvannes.safechat.data.models.MessageStatus.READ
                    }
            if (hasUnreadMessages) {
                viewModel.onEvent(ChatEvent.MarkMessagesAsRead)
            }
        }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text(state.contactName.ifEmpty { "Chat" }) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Navigate back"
                                )
                            }
                        },
                        actions = {
                            // Display encryption status in the toolbar
                            state.encryptionStatus?.let { status ->
                                EncryptionStatusIndicator(
                                        status = status,
                                        showError = state.hasDecryptionErrors,
                                        statusMessage = state.encryptionStatusMessage,
                                        modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        }
                )
            },
            modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(paddingValues)
                                .background(
                                        Brush.verticalGradient(
                                                colors =
                                                        listOf(
                                                                MaterialTheme.colorScheme
                                                                        .background,
                                                                MaterialTheme.colorScheme.surface
                                                                        .copy(alpha = 0.3f)
                                                        )
                                        )
                                )
        ) {
            Column(modifier = Modifier.fillMaxSize().imePadding()) {
                // Show retry encryption button if there are decryption errors
                if (state.hasDecryptionErrors) {
                    Card(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            colors =
                                    CardDefaults.cardColors(
                                            containerColor =
                                                    MaterialTheme.colorScheme.errorContainer
                                    )
                    ) {
                        Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                    text = "Some messages couldn't be decrypted",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                    onClick = { viewModel.onEvent(ChatEvent.RetryEncryption) },
                                    colors =
                                            ButtonDefaults.buttonColors(
                                                    containerColor =
                                                            MaterialTheme.colorScheme.error,
                                                    contentColor = MaterialTheme.colorScheme.onError
                                            )
                            ) { Text("Retry Encryption") }
                        }
                    }
                }

                // Messages List - Reversed layout puts newest messages at bottom
                LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        reverseLayout = true, // This makes newest messages appear at bottom
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = state.messages.reversed(), key = { message -> message.id }) {
                            message ->
                        // Use our new MessageBubble component instead of the basic MessageItem
                        MessageBubble(
                                message = message,
                                isFromCurrentUser = message.senderId == state.currentUserId
                        )
                    }
                }

                // Message Input
                MessageInput(
                        text = state.messageText,
                        onTextChange = { viewModel.onEvent(ChatEvent.UpdateMessageText(it)) },
                        onSendClick = {
                            viewModel.onEvent(ChatEvent.SendMessage(state.messageText))
                        },
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(
                                                start = 16.dp,
                                                end = 16.dp,
                                                top = 8.dp,
                                                bottom = 0.dp
                                        )
                )
            }
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
fun MessageInput(
        text: String,
        onTextChange: (String) -> Unit,
        onSendClick: () -> Unit,
        modifier: Modifier = Modifier
) {
    val buttonColor =
            if (text.isNotBlank()) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }

    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                maxLines = 3,
                shape = RoundedCornerShape(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        FloatingActionButton(
                onClick = onSendClick,
                modifier = Modifier.size(48.dp),
                containerColor = buttonColor
        ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send message")
        }
    }
}
