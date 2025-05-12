package tech.ziasvannes.safechat.data.repository

import android.util.Log
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import tech.ziasvannes.safechat.data.local.dao.MessageDao
import tech.ziasvannes.safechat.data.local.entity.MessageEntity
import tech.ziasvannes.safechat.data.models.ChatSession
import tech.ziasvannes.safechat.data.models.EncryptionStatus
import tech.ziasvannes.safechat.data.models.Message
import tech.ziasvannes.safechat.data.models.MessageStatus
import tech.ziasvannes.safechat.data.models.MessageType
import tech.ziasvannes.safechat.domain.repository.MessageRepository

class MessageRepositoryImpl @Inject constructor(private val messageDao: MessageDao) :
        MessageRepository {
        init {
                Log.d("MessageRepositoryImpl", "MessageRepositoryImpl instantiated")
        }

        /**
                 * Returns a flow that emits the list of messages for a given chat session, updating in real time as the data changes.
                 *
                 * @param chatSessionId The unique identifier of the chat session.
                 * @return A flow emitting lists of messages associated with the specified chat session.
                 */
        override suspend fun getMessages(chatSessionId: UUID): Flow<List<Message>> =
                messageDao.getMessagesForChat(chatSessionId.toString()).map { entities ->
                        entities.map { it.toMessage() }
                }

        /**
         * Attempts to insert a new message into the database and returns a [Result] containing the original message or an error.
         *
         * Any exceptions during insertion are captured and wrapped in the [Result].
         *
         * @return A [Result] containing the original message if successful, or an error if insertion fails.
         */
        override suspend fun sendMessage(message: Message): Result<Message> = runCatching {
                messageDao.insertMessage(MessageEntity.fromMessage(message))
                message
        }

        /**
         * Updates the status of a message with the specified UUID.
         *
         * @param messageId The UUID of the message to update.
         * @param status The new status to assign to the message.
         */
        override suspend fun updateMessageStatus(messageId: UUID, status: MessageStatus) {
                messageDao.updateMessageStatus(messageId.toString(), status)
        }

        /**
         * Deletes a message with the specified ID from the database.
         *
         * Searches for a message matching the given ID among messages associated with the provided ID as a chat session, and deletes it if found.
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
                 * Emits a flow of chat sessions, each constructed from messages in the database.
                 *
                 * For each message, creates a `ChatSession` with a randomly generated session ID, participant IDs from sender and receiver, the message as the last message, unread count set to zero, and encryption status set to ENCRYPTED. Unread count and actual encryption status are placeholders and not yet implemented.
                 *
                 * @return A flow emitting lists of chat sessions.
                 */
        override suspend fun getChatSessions(): Flow<List<ChatSession>> =
                messageDao.getChatSessions().map { messages ->
                        messages.map { message ->
                                ChatSession(
                                        id = UUID.randomUUID(), // Generate a unique ID for the
                                        // session
                                        participantIds =
                                                listOf(
                                                        UUID.fromString(message.senderId),
                                                        UUID.fromString(message.receiverId)
                                                ),
                                        lastMessage = message.toMessage(),
                                        unreadCount = 0, // TODO: Implement unread count
                                        encryptionStatus =
                                                EncryptionStatus
                                                        .ENCRYPTED // TODO: Get actual encryption
                                        // status
                                        )
                        }
                }

        /**
         * Retrieves an existing chat session between the current user and the specified contact, or creates a new one if none exists.
         *
         * If no messages are found between the users, a placeholder message is inserted to initiate the session. The chat session is constructed with a deterministic UUID based on the user and contact IDs, includes both participants, and sets the last message to the most recent message exchanged.
         *
         * @param contactId The UUID of the contact to retrieve or create a chat session with.
         * @return The corresponding ChatSession object.
         */
        override suspend fun getOrCreateChatSessionForContact(contactId: UUID): ChatSession {
                Log.d(
                        "MessageRepositoryImpl",
                        "getOrCreateChatSessionForContact called for contactId: $contactId"
                )
                val currentUserId = UUID.fromString("00000000-0000-0000-0000-000000000001")
                // Get all messages between current user and contact
                val messages =
                        messageDao
                                .getMessagesBetween(currentUserId.toString(), contactId.toString())
                                .first()
                val sessionMessages = messages.map { it.toMessage() }
                var sessionMessagesMutable = sessionMessages.toMutableList()
                if (sessionMessagesMutable.isEmpty()) {
                        // Insert a placeholder message to start the session
                        val placeholder =
                                Message(
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
                        sessionMessagesMutable = mutableListOf(placeholder)
                }
                // Build ChatSession
                val lastMessage = sessionMessagesMutable.maxByOrNull { it.timestamp }
                val chatSession =
                        ChatSession(
                                id =
                                        UUID.nameUUIDFromBytes(
                                                (currentUserId.toString() + contactId.toString())
                                                        .toByteArray()
                                        ),
                                participantIds = listOf(currentUserId, contactId),
                                lastMessage = lastMessage,
                                unreadCount = 0, // Not tracked yet
                                encryptionStatus = EncryptionStatus.ENCRYPTED // Default for now
                        )
                Log.d("MessageRepositoryImpl", "Returning ChatSession: $chatSession")
                return chatSession
        }
}
