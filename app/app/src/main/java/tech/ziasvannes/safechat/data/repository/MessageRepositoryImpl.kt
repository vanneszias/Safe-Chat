package tech.ziasvannes.safechat.data.repository

import java.util.Base64
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import tech.ziasvannes.safechat.data.models.ChatSession
import tech.ziasvannes.safechat.data.models.Message
import tech.ziasvannes.safechat.data.models.MessageStatus
import tech.ziasvannes.safechat.data.models.MessageType
import tech.ziasvannes.safechat.data.remote.ApiService
import tech.ziasvannes.safechat.data.remote.MessageResponse
import tech.ziasvannes.safechat.data.remote.SendMessageRequest
import tech.ziasvannes.safechat.domain.repository.MessageRepository

class MessageRepositoryImpl @Inject constructor(private val apiService: ApiService) :
        MessageRepository {
    /**
     * Returns a flow emitting lists of messages for the specified chat session.
     *
     * @param chatSessionId The unique identifier of the chat session.
     * @return A flow that emits the current list of messages for the chat session, updating as the
     * data changes.
     */
    override suspend fun getMessages(chatSessionId: UUID): Flow<List<Message>> = flow {
        val messages = apiService.getMessages(chatSessionId.toString())
        emit(messages.map { it.toMessage() })
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
        val req = message.toSendMessageRequest()
        val resp = apiService.sendMessage(req)
        resp.toMessage()
    }

    /**
     * Updates the status of a message identified by its UUID.
     *
     * @param messageId The unique identifier of the message to update.
     * @param status The new status to set for the message.
     */
    override suspend fun updateMessageStatus(messageId: UUID, status: MessageStatus) {
        // Not implemented in remote mode
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
        // Not implemented in remote mode
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
    override suspend fun getChatSessions(): Flow<List<ChatSession>> = flow {
        // Not implemented: would require a backend endpoint for chat sessions
        emit(emptyList())
    }
}

private fun MessageResponse.toMessage(): Message =
        Message(
                id = UUID.fromString(id),
                content = content,
                timestamp = timestamp,
                senderId = UUID.fromString(sender_id),
                receiverId = UUID.fromString(receiver_id),
                status = MessageStatus.valueOf(status),
                type =
                        when (type) {
                            "Text" -> MessageType.Text
                            else -> MessageType.Text // Extend for Image/File
                        },
                encryptedContent = Base64.getDecoder().decode(encrypted_content),
                iv = Base64.getDecoder().decode(iv)
        )

private fun Message.toSendMessageRequest(): SendMessageRequest =
        SendMessageRequest(
                content = content,
                receiver_id = receiverId.toString(),
                type =
                        when (type) {
                            is MessageType.Text -> "Text"
                            is MessageType.Image -> "Image"
                            is MessageType.File -> "File"
                        },
                encrypted_content = Base64.getEncoder().encodeToString(encryptedContent),
                iv = Base64.getEncoder().encodeToString(iv)
        )
