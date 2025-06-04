package tech.ziasvannes.safechat.data.repository

import java.util.Base64
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import tech.ziasvannes.safechat.data.models.Message
import tech.ziasvannes.safechat.data.models.MessageStatus
import tech.ziasvannes.safechat.data.models.MessageType
import tech.ziasvannes.safechat.data.remote.ApiService
import tech.ziasvannes.safechat.data.remote.MessageResponse
import tech.ziasvannes.safechat.data.remote.SendMessageRequest
import tech.ziasvannes.safechat.domain.repository.MessageRepository
import tech.ziasvannes.safechat.session.UserSession

class MessageRepositoryImpl
@Inject
constructor(private val apiService: ApiService, private val userSession: UserSession) :
        MessageRepository {
    /**
     * Returns a flow that emits the list of messages for a given chat session, fetched from the
     * remote API.
     *
     * The flow emits the current set of messages for the specified chat session ID as retrieved
     * from the remote service. Messages are NOT automatically marked as read - this should be done
     * explicitly when the user actually views the chat.
     *
     * @param chatSessionId Unique identifier of the chat session whose messages are to be fetched.
     * @return A flow emitting the list of messages for the specified chat session.
     */
    override suspend fun getMessages(chatSessionId: UUID): Flow<List<Message>> = flow {
        val messages = apiService.getMessages(chatSessionId.toString())
        
        // Filter out messages with null/empty encrypted content that may have been
        // corrupted due to server-side deletion when receiver marked them as read
        val validMessages = messages.filter { messageResponse ->
            val hasValidEncryptedContent = !messageResponse.encrypted_content.isNullOrBlank()
            val hasValidIv = !messageResponse.iv.isNullOrBlank()
            
            if (!hasValidEncryptedContent || !hasValidIv) {
                android.util.Log.w(
                    "MessageRepository",
                    "Filtering out message ${messageResponse.id} with invalid encrypted data - likely deleted by receiver"
                )
            }
            
            hasValidEncryptedContent && hasValidIv
        }
        
        val messageList = validMessages.map { it.toMessage() }

        // DO NOT automatically mark messages as read here
        // Messages should only be marked as read when user explicitly views them
        
        emit(messageList)
    }

    /**
     * Sends a message via the remote API and returns the result.
     *
     * Converts the provided [Message] to a request, sends it using the API, and maps the response
     * back to a [Message]. Any errors encountered are captured and returned as a [Result] failure.
     *
     * @return A [Result] containing the sent message on success, or an error if the operation
     * fails.
     */
    override suspend fun sendMessage(message: Message): Result<Message> = runCatching {
        val req = message.toSendMessageRequest()
        val resp = apiService.sendMessage(req)
        resp.toMessage()
    }

    /**
     * Updates the status of a message via the remote API.
     *
     * Sends a request to update the message status on the server. When marking a message as read,
     * the server will automatically delete it to prevent duplicates.
     */
    override suspend fun updateMessageStatus(messageId: UUID, status: MessageStatus) {
        runCatching {
            val request =
                    tech.ziasvannes.safechat.data.remote.UpdateMessageStatusRequest(
                            status = status.name
                    )
            apiService.updateMessageStatus(messageId.toString(), request)
        }
    }



    /**
     * Deletes a message via the remote API.
     *
     * This implementation is a no-op since the remote API doesn't support direct message deletion.
     * Messages are deleted automatically when marked as read via updateMessageStatus.
     */
    override suspend fun deleteMessage(messageId: UUID) {
        // Remote API doesn't support direct message deletion
        // Messages are deleted automatically when marked as read
    }

    /**
     * Retrieves chat sessions from the remote API.
     *
     * This is a simplified implementation that returns an empty flow since the remote API doesn't
     * provide a dedicated chat sessions endpoint.
     */
    override suspend fun getChatSessions():
            Flow<List<tech.ziasvannes.safechat.data.models.ChatSession>> = flow {
        // Remote API doesn't provide chat sessions endpoint
        // This is handled by the local repository layer
        emit(emptyList())
    }
}

/**
 * Converts a [MessageResponse] from the remote API into a [Message] domain model.
 *
 * Decodes Base64-encoded encrypted content and IV, parses UUIDs, and maps status and type fields to
 * their corresponding enums. Unknown or unsupported message types default to [MessageType.Text].
 * Handles Base64 decoding errors gracefully for messages that may have been corrupted.
 *
 * @return The converted [Message] object.
 */
private fun MessageResponse.toMessage(): Message {
    // Safe Base64 decoding with error handling
    val decodedEncryptedContent = try {
        Base64.getDecoder().decode(encrypted_content)
    } catch (e: IllegalArgumentException) {
        android.util.Log.w("MessageRepository", "Failed to decode encrypted_content for message $id", e)
        ByteArray(0) // Return empty array if decoding fails
    }
    
    val decodedIv = try {
        Base64.getDecoder().decode(iv)
    } catch (e: IllegalArgumentException) {
        android.util.Log.w("MessageRepository", "Failed to decode IV for message $id", e)
        ByteArray(0) // Return empty array if decoding fails
    }
    
    return Message(
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
            encryptedContent = decodedEncryptedContent,
            iv = decodedIv
    )
}

/**
 * Converts a [Message] object to a [SendMessageRequest] for sending via the API.
 *
 * Encodes the encrypted content and initialization vector (IV) as Base64 strings and maps the
 * message type to its string representation. For security, only encrypted content is sent to the
 * server.
 *
 * @return A [SendMessageRequest] representing the message in the format expected by the remote API.
 */
private fun Message.toSendMessageRequest(): SendMessageRequest =
        SendMessageRequest(
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
