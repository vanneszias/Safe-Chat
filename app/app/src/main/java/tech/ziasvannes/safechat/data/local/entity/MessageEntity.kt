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
        val decryptedContent: String? = null
) {
        /**
         * Converts this MessageEntity to a domain Message object.
         *
         * Transforms string-based IDs to UUIDs and copies all other fields directly.
         *
         * @return A Message instance representing the same data as this entity.
         */
        fun toMessage(): Message =
                Message(
                        id = UUID.fromString(id),
                        content =
                                when {
                                    // If we have decrypted content, use it
                                    decryptedContent != null && decryptedContent.isNotBlank() -> decryptedContent
                                    
                                    // If original content exists (e.g., for outgoing messages), use it
                                    content.isNotBlank() -> content
                                    
                                    // For any message that couldn't be decrypted
                                    else -> "[🔒 Encrypted message - Unable to decrypt. Sender may not be in your contacts.]"
                                },
                        timestamp = timestamp,
                        senderId = UUID.fromString(senderId),
                        receiverId = UUID.fromString(receiverId),
                        status = status,
                        type = type,
                        encryptedContent = encryptedContent,
                        iv = iv
                )

        companion object {
                /**
                 * Creates a [MessageEntity] from a [Message] domain model.
                 *
                 * Converts UUID fields in the [Message] to string representations and copies all
                 * other properties directly.
                 *
                 * @param message The domain model message to convert.
                 * @return A [MessageEntity] representing the given [Message] for database storage.
                 */
                fun fromMessage(message: Message): MessageEntity =
                        MessageEntity(
                                id = message.id.toString(),
                                content = message.content ?: "", // Provide default if domain content is null
                                timestamp = message.timestamp,
                                senderId = message.senderId.toString(),
                                receiverId = message.receiverId.toString(),
                                status = message.status,
                                type = message.type,
                                encryptedContent = message.encryptedContent,
                                iv = message.iv,
                                decryptedContent = message.content // Keep nullable for decryptedContent
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
         * Returns the hash code value for this entity, based solely on its unique ID.
         *
         * Entities with the same ID will have the same hash code.
         *
         * @return The hash code of the ID property.
         */
        override fun hashCode(): Int {
                return id.hashCode()
        }
}
