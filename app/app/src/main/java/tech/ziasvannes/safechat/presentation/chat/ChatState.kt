package tech.ziasvannes.safechat.presentation.chat

import java.util.UUID
import tech.ziasvannes.safechat.data.models.Contact
import tech.ziasvannes.safechat.data.models.EncryptionStatus
import tech.ziasvannes.safechat.data.models.Message

data class ChatState(
        val messages: List<Message> = emptyList(),
        val contact: Contact? = null,
        val contactName: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
        val messageText: String = "",
        val encryptionStatus: EncryptionStatus? = EncryptionStatus.NOT_ENCRYPTED,
        val currentUserId: UUID,
        val isEncrypted: Boolean = encryptionStatus == EncryptionStatus.ENCRYPTED,
        val hasDecryptionErrors: Boolean = false,
        val encryptionStatusMessage: String = "End-to-end encrypted",
        val corruptedMessageIds: Set<UUID>? = null
)
