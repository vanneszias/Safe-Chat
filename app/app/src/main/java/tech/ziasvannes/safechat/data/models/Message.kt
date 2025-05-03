package tech.ziasvannes.safechat.data.models

import java.util.UUID

data class Message(
    val id: UUID,
    val content: String,
    val timestamp: Long,
    val senderId: UUID,
    val receiverId: UUID,
    val status: MessageStatus,
    val type: MessageType,
    val encryptedContent: ByteArray,
    val iv: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Message
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

sealed class MessageType {
    object Text : MessageType()
    data class Image(val url: String) : MessageType()
    data class File(val url: String, val name: String, val size: Long) : MessageType()
}

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}