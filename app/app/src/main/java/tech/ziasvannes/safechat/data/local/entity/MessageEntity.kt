package tech.ziasvannes.safechat.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import tech.ziasvannes.safechat.data.models.Message
import tech.ziasvannes.safechat.data.models.MessageStatus
import tech.ziasvannes.safechat.data.models.MessageType
import java.util.UUID

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val content: String,
    val timestamp: Long,
    val senderId: String,
    val receiverId: String,
    val status: MessageStatus,
    val type: MessageType,
    val encryptedContent: ByteArray,
    val iv: ByteArray
) {
    /**
     * Converts this MessageEntity to a Message domain model object.
     *
     * Parses string-based UUID fields and copies all other properties directly.
     *
     * @return The corresponding Message object.
     */
    fun toMessage(): Message = Message(
        id = UUID.fromString(id),
        content = content,
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
         * Creates a MessageEntity from a Message domain model.
         *
         * Converts UUID fields in the Message to their string representations and copies all other properties directly.
         *
         * @param message The Message object to convert.
         * @return A MessageEntity representing the given Message for database storage.
         */
        fun fromMessage(message: Message): MessageEntity = MessageEntity(
            id = message.id.toString(),
            content = message.content,
            timestamp = message.timestamp,
            senderId = message.senderId.toString(),
            receiverId = message.receiverId.toString(),
            status = message.status,
            type = message.type,
            encryptedContent = message.encryptedContent,
            iv = message.iv
        )
    }

    /**
     * Checks equality based on the message ID.
     *
     * Returns true if the other object is a `MessageEntity` with the same `id`, or if it is the same instance.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MessageEntity
        return id == other.id
    }

    /**
     * Returns the hash code of this message entity, based solely on its unique ID.
     *
     * Ensures consistent hashing aligned with the equality check, which also relies on the ID field.
     *
     * @return The hash code of the message ID.
     */
    override fun hashCode(): Int {
        return id.hashCode()
    }
}