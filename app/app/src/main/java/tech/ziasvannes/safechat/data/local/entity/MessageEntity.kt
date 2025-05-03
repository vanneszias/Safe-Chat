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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MessageEntity
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}