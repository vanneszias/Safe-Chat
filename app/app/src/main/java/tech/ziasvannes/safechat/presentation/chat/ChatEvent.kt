package tech.ziasvannes.safechat.presentation.chat

import java.util.UUID

sealed class ChatEvent {
    data class SendMessage(val content: String) : ChatEvent()
    data class UpdateMessageText(val text: String) : ChatEvent()
    data class InitiateChat(val chatSessionId: UUID) : ChatEvent()
    object RetryEncryption : ChatEvent()
    object ClearError : ChatEvent()
}
