package tech.ziasvannes.safechat.data.models

import java.util.UUID

data class ChatSession(
    val id: UUID,
    val participantIds: List<UUID>,
    val lastMessage: Message?,
    val unreadCount: Int,
    val encryptionStatus: EncryptionStatus
)

enum class EncryptionStatus {
    NOT_ENCRYPTED,
    KEY_EXCHANGE_IN_PROGRESS,
    ENCRYPTED
}