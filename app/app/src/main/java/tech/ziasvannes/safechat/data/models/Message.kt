package tech.ziasvannes.safechat.data.models

import java.util.UUID

data class Message(
        val id: UUID,
        val content: String?, // Make content nullable
        val timestamp: Long,
        val senderId: UUID,
        val receiverId: UUID,
        val status: MessageStatus,
        val type: MessageType,
        val encryptedContent: ByteArray,
        val iv: ByteArray
) {
    /**
     * Checks if this message is equal to another object based on all message fields. This ensures
     * proper Compose recomposition when any field changes, including status.
     *
     * @return `true` if the other object is a `Message` with the same field values, `false`
     * otherwise.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Message
        // Check all fields to ensure that if some field has been modified, the message is
        // considered different and will trigger recomposition.
        return id == other.id &&
                content == other.content &&
                timestamp == other.timestamp &&
                senderId == other.senderId &&
                receiverId == other.receiverId &&
                status == other.status &&
                type == other.type &&
                encryptedContent.contentEquals(other.encryptedContent) &&
                iv.contentEquals(other.iv)
    }

    /**
     * Returns the hash code of this message based on all relevant fields. This ensures proper
     * behavior in hash-based collections when any field changes.
     *
     * The hash code is computed using the standard algorithm where each field's hash is combined
     * using the formula: result = 31 * result + field.hashCode()
     *
     * Why 31?
     * - 31 is a prime number, which helps reduce hash collisions and provides better distribution
     * - It's odd, ensuring no information is lost during multiplication due to overflow
     * - The JVM can optimize "31 * i" as "(i << 5) - i" for better performance
     * - It's the standard used in Java's String.hashCode() and widely accepted across libraries
     *
     * This implementation maintains the contract with equals(): if two Message objects are equal
     * according to equals(), they will have the same hash code.
     *
     * @return The hash code computed from all message fields.
     */
    override fun hashCode(): Int {
        // Start with the first field's hash code
        var result = id.hashCode()
        
        // Combine each subsequent field using the 31 * result + field pattern
        // This creates a rolling hash that incorporates all field values
        result = 31 * result + (content?.hashCode() ?: 0)
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + senderId.hashCode()
        result = 31 * result + receiverId.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + encryptedContent.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        return result
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
