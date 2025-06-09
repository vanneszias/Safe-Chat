package tech.ziasvannes.safechat.presentation.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.ziasvannes.safechat.data.models.MessageType
import tech.ziasvannes.safechat.data.repository.LocalMessageRepositoryImpl
import tech.ziasvannes.safechat.data.repository.WebSocketMessageRepositoryImpl
import tech.ziasvannes.safechat.data.websocket.WebSocketManager
import tech.ziasvannes.safechat.domain.repository.ContactRepository
import tech.ziasvannes.safechat.domain.repository.EncryptionRepository
import tech.ziasvannes.safechat.domain.repository.MessageRepository
import tech.ziasvannes.safechat.domain.usecase.InitiateKeyExchangeUseCase
import tech.ziasvannes.safechat.domain.usecase.SendMessageUseCase
import tech.ziasvannes.safechat.session.UserSession

@HiltViewModel
class ChatViewModel
@Inject
constructor(
        private val messageRepository: MessageRepository,
        private val contactRepository: ContactRepository,
        private val sendMessageUseCase: SendMessageUseCase,
        private val initiateKeyExchangeUseCase: InitiateKeyExchangeUseCase,
        private val userSession: UserSession,
        private val encryptionRepository: EncryptionRepository,
        private val webSocketManager: WebSocketManager
) : ViewModel() {

    private val currentUserId: UUID
        get() = userSession.userId ?: throw IllegalStateException("User ID not set in session")

    private val _state = MutableStateFlow(ChatState(currentUserId = currentUserId))
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private var currentChatJob: Job? = null

    companion object {
        private const val TAG = "ChatViewModel"
    }

    init {
        // Start observing WebSocket events for real-time messaging
        viewModelScope.launch {
            if (messageRepository is WebSocketMessageRepositoryImpl) {
                messageRepository.startObservingWebSocketEvents()
            }
        }
    }

    /**
     * Handles chat-related events and updates the UI state or triggers actions accordingly.
     *
     * Processes events such as sending messages, updating message text, initiating chat loading,
     * retrying encryption, and clearing error messages by delegating to the appropriate methods or
     * updating state.
     *
     * @param event The chat event to handle.
     */
    fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.SendMessage -> {
                sendMessage(event.content)
            }
            is ChatEvent.UpdateMessageText -> {
                _state.update { it.copy(messageText = event.text) }
            }
            is ChatEvent.InitiateChat -> {
                loadChat(event.chatSessionId)
            }
            ChatEvent.RetryEncryption -> {
                retryEncryption()
            }
            ChatEvent.ClearError -> {
                _state.update { it.copy(error = null) }
            }
            ChatEvent.MarkMessagesAsRead -> {
                markMessagesAsRead()
            }
        }
    }

    /**
     * Loads contact details and observes messages for the specified chat session, updating the UI
     * state.
     *
     * Updates the state with contact information and continuously collects messages for the given
     * chat session. Implements intelligent loading that prioritizes local data and handles
     * server sync gracefully to prevent decryption issues with deleted messages.
     * Also marks received messages as read when the chat is opened.
     *
     * @param chatSessionId The unique identifier of the chat session to load.
     */
    fun loadChat(chatSessionId: UUID) {
        // Cancel any existing message collection
        currentChatJob?.cancel()
        
        currentChatJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val contact = contactRepository.getContactById(chatSessionId)
                _state.update {
                    it.copy(contact = contact, contactName = contact?.name ?: "", isLoading = false)
                }

                // Start observing messages for the chat session with intelligent loading
                messageRepository.getMessages(chatSessionId).collect { messages ->
                    processMessages(messages)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading chat", e)
                _state.update {
                    it.copy(
                            error =
                                    "Failed to load chat. Please check your connection and try again.",
                            isLoading = false
                    )
                }
            }
        }
    }

    private fun processMessages(messages: List<tech.ziasvannes.safechat.data.models.Message>) {
        // Keep track of corrupted message IDs to avoid repeated logging
        val currentCorruptedIds = _state.value.corruptedMessageIds ?: emptySet()
        val newCorruptedIds = mutableSetOf<UUID>()
        
        // Filter out messages with decryption errors or corrupted data
        val validMessages = messages.filter { message ->
            val hasDecryptionError = message.content?.startsWith("[🔒 Encrypted message") == true
            val hasEmptyContent = message.content.isNullOrBlank()
            val isProcessing = message.content == "[🔄 Processing encrypted message...]"
            val isOutgoingMessage = message.senderId == currentUserId
            val isCorrupted = hasDecryptionError || (hasEmptyContent && !isOutgoingMessage)
            
            if (isCorrupted && !isProcessing) {
                newCorruptedIds.add(message.id)
                // Only log if we haven't seen this corrupted message before
                if (!currentCorruptedIds.contains(message.id)) {
                    Log.w(TAG, "Filtering out corrupted message ${message.id}")
                }
                false
            } else {
                true
            }
        }.sortedBy { it.timestamp }

        // Check if any messages contain decryption errors
        val hasDecryptionErrors = validMessages.any { 
            it.content?.startsWith("[🔒 Encrypted message") == true 
        }
        
        // Determine encryption status based on valid messages
        val encryptionStatus = if (validMessages.isNotEmpty()) {
            tech.ziasvannes.safechat.data.models.EncryptionStatus.ENCRYPTED
        } else {
            tech.ziasvannes.safechat.data.models.EncryptionStatus.NOT_ENCRYPTED
        }
        
        // Create a user-friendly status message
        val statusMessage = when {
            hasDecryptionErrors -> "Some messages couldn't be decrypted. This may happen if messages were deleted by the receiver."
            encryptionStatus == tech.ziasvannes.safechat.data.models.EncryptionStatus.ENCRYPTED -> 
                "Messages are end-to-end encrypted"
            else -> "No messages yet"
        }
        
        // Update the state with messages and corrupted IDs
        _state.update { currentState ->
            currentState.copy(
                messages = validMessages,
                encryptionStatus = encryptionStatus,
                hasDecryptionErrors = hasDecryptionErrors,
                encryptionStatusMessage = statusMessage,
                corruptedMessageIds = currentCorruptedIds + newCorruptedIds,
                isLoading = false
            )
        }
    }

    /**
     * Sends a text message to the current contact and updates the chat state accordingly.
     *
     * If the message is sent successfully, clears the message input and reloads the chat. If
     * sending fails, updates the state with an error message.
     *
     * @param content The text content of the message to send.
     */
    private fun sendMessage(content: String) {
        viewModelScope.launch {
            val currentState = state.value
            val contact = currentState.contact ?: return@launch

            try {
                sendMessageUseCase(
                                content = content,
                                receiverId = contact.id,
                                type = MessageType.Text
                        )
                        .onSuccess {
                            _state.update { it.copy(messageText = "") }
                            // Message will appear through the existing flow collection
                            // No need to manually refresh here
                        }
                        .onFailure { error -> _state.update { it.copy(error = error.message) } }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to send message") }
            }
        }
    }

    /**
     * Attempts to re-establish end-to-end encryption with the current contact.
     *
     * Updates the UI state to reflect encryption status or any encountered error. If no contact is
     * selected, the operation is skipped.
     */
    private fun retryEncryption() {
        viewModelScope.launch {
            val currentState = state.value
            val contact = currentState.contact ?: return@launch

            _state.update { it.copy(isLoading = true) }
            try {
                initiateKeyExchangeUseCase(contact.id)
                        .onSuccess {
                            _state.update {
                                it.copy(
                                        isEncrypted = true,
                                        isLoading = false,
                                        encryptionStatusMessage =
                                                "Encryption re-established with ${contact.name}",
                                        hasDecryptionErrors = false
                                )
                            }
                        }
                        .onFailure { error ->
                            _state.update {
                                it.copy(
                                        error = error.message ?: "Failed to establish encryption",
                                        isLoading = false,
                                        encryptionStatusMessage =
                                                "Encryption setup failed. Try again."
                                )
                            }
                        }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                            error = e.message ?: "Failed to establish encryption",
                            isLoading = false,
                            encryptionStatusMessage = "Encryption setup failed. Try again."
                    )
                }
            }
        }
    }

    /**
     * Marks received messages as read for the current chat session. This should be called when the
     * user explicitly views the chat.
     */
    private fun markMessagesAsRead() {
        viewModelScope.launch {
            val currentState = state.value
            val contact = currentState.contact ?: return@launch

            try {
                Log.d(TAG, "Marking messages as read for contact: ${contact.id}")
                messageRepository.markMessagesAsRead(contact.id)
                
                // No need to call loadChat again - the flow collection will automatically 
                // update when message statuses change in the repository
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark messages as read", e)
                _state.update { it.copy(error = "Failed to mark messages as read: ${e.message}") }
            }
        }
    }
}
