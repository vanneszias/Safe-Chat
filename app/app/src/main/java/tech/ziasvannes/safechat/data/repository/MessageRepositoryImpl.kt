package tech.ziasvannes.safechat.data.repository

import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tech.ziasvannes.safechat.data.local.dao.MessageDao
import tech.ziasvannes.safechat.data.local.entity.MessageEntity
import tech.ziasvannes.safechat.data.models.ChatSession
import tech.ziasvannes.safechat.data.models.EncryptionStatus
import tech.ziasvannes.safechat.data.models.Message
import tech.ziasvannes.safechat.data.models.MessageStatus
import tech.ziasvannes.safechat.domain.repository.MessageRepository

class MessageRepositoryImpl @Inject constructor(private val messageDao: MessageDao) :
        MessageRepository {
    /**
     * Returns a flow emitting lists of messages for the specified chat session.
     *
     * @param chatSessionId The unique identifier of the chat session.
     * @return A flow that emits the current list of messages for the chat session, updating as the
     * data changes.
     */
    override suspend fun getMessages(chatSessionId: UUID): Flow<List<Message>> =
            messageDao.getMessagesForChat(chatSessionId.toString()).map { entities ->
                entities.map { it.toMessage() }
            }

    /**
     * Inserts a new message into the database and returns a [Result] containing the original
     * message.
     *
     * Any exceptions during insertion are captured and wrapped in the [Result].
     *
     * @return A [Result] containing the inserted message, or an error if insertion fails.
     */
    override suspend fun sendMessage(message: Message): Result<Message> = runCatching {
        messageDao.insertMessage(MessageEntity.fromMessage(message))
        message
    }

    /**
     * Updates the status of a message identified by its UUID.
     *
     * @param messageId The unique identifier of the message to update.
     * @param status The new status to set for the message.
     */
    override suspend fun updateMessageStatus(messageId: UUID, status: MessageStatus) {
        messageDao.updateMessageStatus(messageId.toString(), status)
    }

    /**
     * Deletes a message with the specified ID from the database.
     *
     * Attempts to find and remove the message by collecting messages for the chat session
     * identified by the given message ID. Note: The use of message ID as a chat session ID may
     * indicate a logic issue.
     *
     * @param messageId The unique identifier of the message to delete.
     */
    override suspend fun deleteMessage(messageId: UUID) {
        messageDao.getMessagesForChat(messageId.toString()).collect { messages ->
            messages.find { it.id == messageId.toString() }?.let { messageDao.deleteMessage(it) }
        }
    }

    /**
     * Returns a flow of chat sessions constructed from messages in the database.
     *
     * Each chat session includes participant IDs, the last message, a placeholder unread count of
     * zero, and an encryption status set to ENCRYPTED. The session ID is randomly generated for
     * each session. Unread count and actual encryption status are not yet implemented.
     *
     * @return A flow emitting lists of chat sessions.
     */
    override suspend fun getChatSessions(): Flow<List<ChatSession>> =
            messageDao.getChatSessions().map { messages ->
                messages.map { message ->
                    ChatSession(
                            id = UUID.randomUUID(), // Generate a unique ID for the session
                            participantIds =
                                    listOf(
                                            UUID.fromString(message.senderId),
                                            UUID.fromString(message.receiverId)
                                    ),
                            lastMessage = message.toMessage(),
                            unreadCount = 0, // TODO: Implement unread count
                            encryptionStatus =
                                    EncryptionStatus.ENCRYPTED // TODO: Get actual encryption status
                    )
                }
            }
}
