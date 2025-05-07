package tech.ziasvannes.safechat.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tech.ziasvannes.safechat.data.local.dao.MessageDao
import tech.ziasvannes.safechat.data.local.entity.MessageEntity
import tech.ziasvannes.safechat.data.models.ChatSession
import tech.ziasvannes.safechat.data.models.EncryptionStatus
import tech.ziasvannes.safechat.data.models.Message
import tech.ziasvannes.safechat.data.models.MessageStatus
import tech.ziasvannes.safechat.data.models.MessageType
import tech.ziasvannes.safechat.domain.repository.MessageRepository
import java.util.UUID
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao
) : MessageRepository {
    /**
     * Returns a flow emitting lists of messages for the specified chat session.
     *
     * @param chatSessionId The unique identifier of the chat session.
     * @return A flow that emits the current list of messages for the chat session, updating as the data changes.
     */
    override suspend fun getMessages(chatSessionId: UUID): Flow<List<Message>> =
        messageDao.getMessagesForChat(chatSessionId.toString()).map { entities ->
            entities.map { it.toMessage() }
        }

    /**
     * Inserts a new message into the database and returns a [Result] containing the original message.
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
     * Attempts to find and remove the message by collecting messages for the chat session identified by the given message ID.
     * Note: The use of message ID as a chat session ID may indicate a logic issue.
     *
     * @param messageId The unique identifier of the message to delete.
     */
    override suspend fun deleteMessage(messageId: UUID) {
        messageDao.getMessagesForChat(messageId.toString()).collect { messages ->
            messages.find { it.id == messageId.toString() }?.let {
                messageDao.deleteMessage(it)
            }
        }
    }

    /**
     * Returns a flow of chat sessions constructed from messages in the database.
     *
     * Each chat session includes participant IDs, the last message, a placeholder unread count of zero, and an encryption status set to ENCRYPTED. The session ID is randomly generated for each session. Unread count and actual encryption status are not yet implemented.
     *
     * @return A flow emitting lists of chat sessions.
     */
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

    override suspend fun getOrCreateChatSessionForContact(contactId: UUID): ChatSession {
        // For now, assume current user is always this UUID
        val currentUserId = UUID.fromString("00000000-0000-0000-0000-000000000001")
        // Get all messages between current user and contact
        val allMessages = messageDao.getMessagesForChat(currentUserId.toString())
        var sessionMessages: List<Message> = emptyList()
        allMessages.collect { messages ->
            sessionMessages = messages
                .map { it.toMessage() }
                .filter {
                    (it.senderId == currentUserId && it.receiverId == contactId) ||
                            (it.senderId == contactId && it.receiverId == currentUserId)
                }
            // Only need the first emission
            return@collect
        }
        if (sessionMessages.isEmpty()) {
            // Insert a placeholder message to start the session
            val placeholder = Message(
                id = UUID.randomUUID(),
                content = "Chat started",
                timestamp = System.currentTimeMillis(),
                senderId = currentUserId,
                receiverId = contactId,
                status = MessageStatus.DELIVERED,
                type = MessageType.Text,
                encryptedContent = ByteArray(0),
                iv = ByteArray(0),
                hmac = ByteArray(0)
            )
            messageDao.insertMessage(MessageEntity.fromMessage(placeholder))
            sessionMessages = listOf(placeholder)
        }
        // Build ChatSession
        val lastMessage = sessionMessages.maxByOrNull { it.timestamp }
        return ChatSession(
            id = UUID.nameUUIDFromBytes((currentUserId.toString() + contactId.toString()).toByteArray()),
            participantIds = listOf(currentUserId, contactId),
            lastMessage = lastMessage,
            unreadCount = 0, // Not tracked yet
            encryptionStatus = EncryptionStatus.ENCRYPTED // Default for now
        )
    }
}