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

@HiltViewModel
class ChatViewModel
@Inject
constructor(
        private val messageRepository: MessageRepository,
        private val contactRepository: ContactRepository,
        private val sendMessageUseCase: SendMessageUseCase,
        private val initiateKeyExchangeUseCase: InitiateKeyExchangeUseCase,
        private val encryptionRepository: EncryptionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState(isEncrypted = true))
    val state: StateFlow<ChatState> = _state.asStateFlow()

    /**
     * Handles chat events by updating chat UI state or triggering related actions.
     *
     * Processes events such as sending messages, updating the message input, loading chat data for a session, retrying encryption, and clearing error messages.
     *
     * @param event The chat event to process.
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
     * Loads contact details and observes messages for the given chat session, updating the UI state with contact information and decrypted messages.
     *
     * Attempts to decrypt each message using the contact's public key and a computed shared secret. If decryption fails for a message, its content is replaced with a placeholder. Updates the UI state with any errors encountered during loading or message processing.
     *
     * @param chatSessionId The unique identifier of the chat session to load.
     */
    private fun loadChat(chatSessionId: UUID) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                // In real mode, assume chatSessionId is the contactId for now (fallback)
                val contactId = chatSessionId
                val session: Any? = null // No session object in real mode

                // Load contact information
                val contact = contactRepository.getContactById(contactId)
                _state.update { it.copy(contact = contact, contactName = contact?.name ?: "") }

                // Start observing messages
                messageRepository.getMessages(chatSessionId).collect { messages ->
                    val decryptedMessages =
                        if (contact != null) {
                            val recipientPublicKey =
                                java.security.KeyFactory.getInstance("DH")
                                    .generatePublic(
                                        java.security.spec.X509EncodedKeySpec(
                                            contact.publicKey.toByteArray()
                                        )
                                    )
                            val sharedSecret =
                                encryptionRepository.computeSharedSecret(recipientPublicKey)
                            messages.map { msg ->
                                try {
                                    val decryptedContent =
                                        encryptionRepository.decryptMessage(
                                            msg.encryptedContent,
                                            msg.iv,
                                            msg.hmac,
                                            sharedSecret
                                        )
                                    msg.copy(content = decryptedContent)
                                } catch (e: Exception) {
                                    msg.copy(content = "[Decryption failed]")
                                }
                            }
                        } else messages
                    _state.update { it.copy(messages = decryptedMessages) }
                }
            } catch (e: Exception) {
                // Log the detailed error for debugging
                Log.e("ChatViewModel", "Error loading chat", e)
                // Present a sanitized message to the user
                _state.update { it.copy(error = "Failed to load chat. Please try again.") }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Attempts to send a text message to the current contact and updates the chat UI state accordingly.
     *
     * If the message is sent successfully, the message input field is cleared. If sending fails, the error message is reflected in the UI state.
     *
     * @param content The text content of the message to be sent.
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
                        .onSuccess { _state.update { it.copy(messageText = "") } }
                        .onFailure { error -> _state.update { it.copy(error = error.message) } }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to send message") }
            }
        }
    }

    /**
     * Re-initiates end-to-end encryption with the currently selected contact.
     *
     * If a contact is selected, attempts to perform a key exchange and updates the UI state to reflect the encryption status or any error encountered. Does nothing if no contact is selected.
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
