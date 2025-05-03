package tech.ziasvannes.safechat.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tech.ziasvannes.safechat.data.local.dao.MessageDao
import tech.ziasvannes.safechat.data.local.entity.MessageEntity
import tech.ziasvannes.safechat.data.models.ChatSession
import tech.ziasvannes.safechat.data.models.EncryptionStatus
import tech.ziasvannes.safechat.data.models.Message
import tech.ziasvannes.safechat.data.models.MessageStatus
import tech.ziasvannes.safechat.domain.repository.MessageRepository
import java.util.UUID
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao
) : MessageRepository {
    override suspend fun getMessages(chatSessionId: UUID): Flow<List<Message>> =
        messageDao.getMessagesForChat(chatSessionId.toString()).map { entities ->
            entities.map { it.toMessage() }
        }

    override suspend fun sendMessage(message: Message): Result<Message> = runCatching {
        messageDao.insertMessage(MessageEntity.fromMessage(message))
        message
    }

    override suspend fun updateMessageStatus(messageId: UUID, status: MessageStatus) {
        messageDao.updateMessageStatus(messageId.toString(), status)
    }

    override suspend fun deleteMessage(messageId: UUID) {
        messageDao.getMessagesForChat(messageId.toString()).collect { messages ->
            messages.find { it.id == messageId.toString() }?.let {
                messageDao.deleteMessage(it)
            }
        }
    }

    override suspend fun getChatSessions(): Flow<List<ChatSession>> =
        messageDao.getChatSessions().map { messages ->
            messages.map { message ->
                ChatSession(
                    id = UUID.randomUUID(), // Generate a unique ID for the session
                    participantIds = listOf(
                        UUID.fromString(message.senderId),
                        UUID.fromString(message.receiverId)
                    ),
                    lastMessage = message.toMessage(),
                    unreadCount = 0, // TODO: Implement unread count
                    encryptionStatus = EncryptionStatus.ENCRYPTED // TODO: Get actual encryption status
                )
            }
        }
}