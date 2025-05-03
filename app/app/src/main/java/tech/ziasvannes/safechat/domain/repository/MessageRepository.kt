package tech.ziasvannes.safechat.domain.repository

import kotlinx.coroutines.flow.Flow
import tech.ziasvannes.safechat.data.models.ChatSession
import tech.ziasvannes.safechat.data.models.Message
import tech.ziasvannes.safechat.data.models.MessageStatus
import java.util.UUID

interface MessageRepository {
    /**
 * Returns a flow emitting lists of messages for the specified chat session.
 *
 * @param chatSessionId The unique identifier of the chat session whose messages are to be retrieved.
 * @return A [Flow] that emits updated lists of [Message] objects for the given chat session.
 */
suspend fun getMessages(chatSessionId: UUID): Flow<List<Message>>
    /**
 * Sends a message and returns the result.
 *
 * @param message The message to be sent.
 * @return A [Result] containing the sent [Message] on success, or an error on failure.
 */
suspend fun sendMessage(message: Message): Result<Message>
    /**
 * Updates the status of a message identified by its UUID.
 *
 * @param messageId The unique identifier of the message to update.
 * @param status The new status to assign to the message.
 */
suspend fun updateMessageStatus(messageId: UUID, status: MessageStatus)
    /**
 * Deletes the message identified by the given UUID.
 *
 * @param messageId The unique identifier of the message to delete.
 */
suspend fun deleteMessage(messageId: UUID)
    /**
 * Returns a flow that emits lists of all chat sessions.
 *
 * The flow updates whenever the set of chat sessions changes.
 *
 * @return A [Flow] emitting lists of [ChatSession] objects.
 */
suspend fun getChatSessions(): Flow<List<ChatSession>>
}