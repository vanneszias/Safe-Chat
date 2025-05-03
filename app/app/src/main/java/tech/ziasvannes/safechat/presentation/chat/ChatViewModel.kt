package tech.ziasvannes.safechat.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.ziasvannes.safechat.data.models.Message
import tech.ziasvannes.safechat.data.models.MessageType
import tech.ziasvannes.safechat.domain.repository.ContactRepository
import tech.ziasvannes.safechat.domain.repository.EncryptionRepository
import tech.ziasvannes.safechat.domain.repository.MessageRepository
import tech.ziasvannes.safechat.domain.usecase.InitiateKeyExchangeUseCase
import tech.ziasvannes.safechat.domain.usecase.SendMessageUseCase
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository,
    private val sendMessageUseCase: SendMessageUseCase,
    private val initiateKeyExchangeUseCase: InitiateKeyExchangeUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    /**
     * Handles chat-related events and updates the UI state or triggers actions accordingly.
     *
     * Processes events such as sending messages, updating message text, initiating chat loading,
     * retrying encryption, and clearing error messages by delegating to the appropriate internal methods
     * or updating the state.
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
                loadChat(event.contactId)
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
     * Loads contact details and observes messages for the specified contact, updating the chat state accordingly.
     *
     * Updates the state to reflect loading, retrieved contact information, incoming messages, and any errors encountered during the process.
     *
     * @param contactId The unique identifier of the contact whose chat is to be loaded.
     */
    private fun loadChat(contactId: UUID) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            try {
                // Load contact information
                val contact = contactRepository.getContactById(contactId)
                _state.update { it.copy(contact = contact) }

                // Start observing messages
                messageRepository.getMessages(contactId).collect { messages ->
                    _state.update { it.copy(messages = messages) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to load chat") }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Sends a text message to the current contact and updates the UI state based on the result.
     *
     * If the message is sent successfully, clears the message input field. If sending fails, updates the state with an error message.
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
                ).onSuccess {
                    _state.update { it.copy(messageText = "") }
                }.onFailure { error ->
                    _state.update { it.copy(error = error.message) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to send message") }
            }
        }
    }

    /**
     * Attempts to re-establish end-to-end encryption with the current contact.
     *
     * Updates the UI state to reflect encryption status or any encountered errors.
     */
    private fun retryEncryption() {
        viewModelScope.launch {
            val currentState = state.value
            val contact = currentState.contact ?: return@launch
            
            try {
                initiateKeyExchangeUseCase(contact.id)
                    .onSuccess {
                        _state.update { it.copy(isEncrypted = true) }
                    }
                    .onFailure { error ->
                        _state.update { it.copy(error = error.message) }
                    }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to establish encryption") }
            }
        }
    }
}