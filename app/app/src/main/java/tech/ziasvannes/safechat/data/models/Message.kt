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
    /**
     * Checks if this message is equal to another object based on the message's unique identifier.
     *
     * @return `true` if the other object is a `Message` with the same `id`, `false` otherwise.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Message
        return id == other.id
    }

    /**
     * Returns the hash code of this message based on its unique identifier.
     *
     * Only the `id` property is used to compute the hash code, ensuring consistency with the
     * `equals` method.
     *
     * @return The hash code of the message's `id`.
     */
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
