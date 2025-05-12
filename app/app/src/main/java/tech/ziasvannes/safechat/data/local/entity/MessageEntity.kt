package tech.ziasvannes.safechat.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import tech.ziasvannes.safechat.data.models.Message
import tech.ziasvannes.safechat.data.models.MessageStatus
import tech.ziasvannes.safechat.data.models.MessageType

@Entity(tableName = "messages")
data class MessageEntity(
        @PrimaryKey val id: String,
        val content: String,
        val timestamp: Long,
        val senderId: String,
        val receiverId: String,
        val status: MessageStatus,
        val type: MessageType,
        val encryptedContent: ByteArray,
        val iv: ByteArray,
        val hmac: ByteArray
) {
    /**
             * Converts this MessageEntity into a domain Message object.
             *
             * Maps string-based UUID fields to UUID objects and copies all other properties, including encryption and integrity fields.
             *
             * @return A Message object containing the data from this entity.
             */
    fun toMessage(): Message =
            Message(
                    id = UUID.fromString(id),
                    content = content,
                    timestamp = timestamp,
                    senderId = UUID.fromString(senderId),
                    receiverId = UUID.fromString(receiverId),
                    status = status,
                    type = type,
                    encryptedContent = encryptedContent,
                    iv = iv,
                    hmac = hmac
            )

    companion object {
        /**
                 * Converts a [Message] domain model into a [MessageEntity] for database storage.
                 *
                 * Transforms UUID fields to string representations and copies all other properties, including encryption and integrity fields.
                 *
                 * @param message The domain model message to convert.
                 * @return A [MessageEntity] representing the provided [Message].
                 */
        fun fromMessage(message: Message): MessageEntity =
                MessageEntity(
                        id = message.id.toString(),
                        content = message.content,
                        timestamp = message.timestamp,
                        senderId = message.senderId.toString(),
                        receiverId = message.receiverId.toString(),
                        status = message.status,
                        type = message.type,
                        encryptedContent = message.encryptedContent,
                        iv = message.iv,
                        hmac = message.hmac
                )
    }

    /**
     * Checks if this `MessageEntity` is equal to another object based on the `id` property.
     *
     * Returns true if the other object is a `MessageEntity` with the same `id`, or if both
     * references point to the same instance.
     *
     * @return `true` if the objects are considered equal, `false` otherwise.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MessageEntity
        return id == other.id
    }

    /**
     * Returns a hash code based on the entity's unique ID.
     *
     * Entities with the same ID will produce the same hash code.
     *
     * @return The hash code of the ID.
     */
    override fun hashCode(): Int {
        return id.hashCode()
    }
}
