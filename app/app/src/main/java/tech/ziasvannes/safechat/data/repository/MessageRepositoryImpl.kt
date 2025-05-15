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
     * Returns a flow that emits the list of messages for a given chat session, fetched from the remote API.
     *
     * The flow emits the current set of messages for the specified chat session ID as retrieved from the remote service.
     *
     * @param chatSessionId Unique identifier of the chat session whose messages are to be fetched.
     * @return A flow emitting the list of messages for the specified chat session.
     */
    override suspend fun getMessages(chatSessionId: UUID): Flow<List<Message>> = flow {
        val messages = apiService.getMessages(chatSessionId.toString())
        emit(messages.map { it.toMessage() })
    }

    /**
     * Sends a message via the remote API and returns the result.
     *
     * Converts the provided [Message] to a request, sends it using the API, and maps the response back to a [Message]. Any errors encountered are captured and returned as a [Result] failure.
     *
     * @return A [Result] containing the sent message on success, or an error if the operation fails.
     */
    override suspend fun sendMessage(message: Message): Result<Message> = runCatching {
        val req = message.toSendMessageRequest()
        val resp = apiService.sendMessage(req)
        resp.toMessage()
    }

    /**
     * Placeholder for updating the status of a message by its UUID.
     *
     * This method is not implemented in remote mode and performs no operation.
     */
    override suspend fun updateMessageStatus(messageId: UUID, status: MessageStatus) {
        // Not implemented in remote mode
    }

    /**
     * Deletes a message by its unique identifier.
     *
     * Not implemented in remote mode; this method currently has no effect.
     */
    override suspend fun deleteMessage(messageId: UUID) {
        // Not implemented in remote mode
    }

    /**
     * Returns a flow emitting an empty list of chat sessions.
     *
     * This method is a placeholder and does not retrieve chat sessions, as backend support is not implemented.
     *
     * @return A flow emitting an empty list.
     */
    override suspend fun getChatSessions(): Flow<List<ChatSession>> = flow {
        // Not implemented: would require a backend endpoint for chat sessions
        emit(emptyList())
    }
}

/**
         * Converts a [MessageResponse] from the remote API into a [Message] domain model.
         *
         * Decodes Base64-encoded encrypted content and IV, parses UUIDs, and maps status and type fields to their corresponding enums.
         * Unknown or unsupported message types default to [MessageType.Text].
         *
         * @return The converted [Message] object.
         */
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

/**
         * Converts a [Message] object to a [SendMessageRequest] for sending via the API.
         *
         * Encodes the encrypted content and initialization vector (IV) as Base64 strings and maps the message type to its string representation.
         *
         * @return A [SendMessageRequest] representing the message in the format expected by the remote API.
         */
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
