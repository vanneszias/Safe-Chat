package tech.ziasvannes.safechat.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import android.util.Log
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
                // Log the detailed error for debugging
                Log.e("ChatViewModel", "Error loading chat", e)
                // Present a sanitized message to the user
                _state.update { it.copy(error = "Failed to load chat. Please try again.") }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

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