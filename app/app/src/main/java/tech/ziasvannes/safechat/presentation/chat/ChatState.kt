package tech.ziasvannes.safechat.presentation.chat

import tech.ziasvannes.safechat.data.models.Contact
import tech.ziasvannes.safechat.data.models.Message

data class ChatState(
    val messages: List<Message> = emptyList(),
    val contact: Contact? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val messageText: String = "",
    val isEncrypted: Boolean = false
)