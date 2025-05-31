package tech.ziasvannes.safechat.presentation.chat

import android.util.Log
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
import tech.ziasvannes.safechat.data.models.MessageType
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
        private val encryptionRepository: EncryptionRepository
) : ViewModel() {

    private val currentUserId: UUID
        get() = userSession.userId ?: throw IllegalStateException("User ID not set in session")

    private val _state = MutableStateFlow(ChatState(currentUserId = currentUserId))
    val state: StateFlow<ChatState> = _state.asStateFlow()

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
        }
    }

    /**
     * Loads contact details and observes messages for the specified chat session, updating the UI
     * state.
     *
     * Updates the state with contact information and continuously collects messages for the given
     * chat session. If an error occurs, sets a user-friendly error message in the state.
     *
     * @param chatSessionId The unique identifier of the chat session to load.
     */
    fun loadChat(chatSessionId: UUID) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val contact = contactRepository.getContactById(chatSessionId)
                _state.update {
                    it.copy(contact = contact, contactName = contact?.name ?: "", isLoading = false)
                }
                // Start observing messages for the chat session
                messageRepository.getMessages(chatSessionId).collect { messages ->
                    // No need to decrypt here; messages already have decrypted content
                    _state.update {
                        it.copy(
                                messages = messages,
                                encryptionStatus =
                                        if (messages.any { it.content.isNotBlank() })
                                                tech.ziasvannes.safechat.data.models
                                                        .EncryptionStatus.ENCRYPTED
                                        else
                                                tech.ziasvannes.safechat.data.models
                                                        .EncryptionStatus.NOT_ENCRYPTED
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error loading chat", e)
                _state.update {
                    it.copy(error = "Failed to load chat. Please try again.", isLoading = false)
                }
            }
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
                            loadChat(contact.id)
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

            try {
                initiateKeyExchangeUseCase(contact.id)
                        .onSuccess { _state.update { it.copy(isEncrypted = true) } }
                        .onFailure { error -> _state.update { it.copy(error = error.message) } }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to establish encryption") }
            }
        }
    }
}
