package tech.ziasvannes.safechat.data.repository

import android.util.Log
import java.util.Base64
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import tech.ziasvannes.safechat.data.models.ChatSession
import tech.ziasvannes.safechat.data.models.Message
import tech.ziasvannes.safechat.data.models.MessageStatus
import tech.ziasvannes.safechat.data.models.MessageType
import tech.ziasvannes.safechat.data.repository.EncryptionRepositoryImpl
import tech.ziasvannes.safechat.data.websocket.WebSocketEvent
import tech.ziasvannes.safechat.data.websocket.WebSocketService
import tech.ziasvannes.safechat.domain.repository.MessageRepository
import tech.ziasvannes.safechat.session.UserSession

class WebSocketMessageRepositoryImpl
@Inject
constructor(
        private val localMessageRepository: LocalMessageRepositoryImpl,
        private val webSocketService: WebSocketService,
        private val userSession: UserSession,
        private val encryptionRepository: tech.ziasvannes.safechat.domain.repository.EncryptionRepository,
        private val contactRepository: tech.ziasvannes.safechat.domain.repository.ContactRepository,
        private val messageDao: tech.ziasvannes.safechat.data.local.dao.MessageDao
) : MessageRepository {

    // Keep track of processed message IDs to avoid duplicates
    private val processedMessageIds = mutableSetOf<UUID>()

    companion object {
        private const val TAG = "WebSocketMessageRepo"
    }

    override suspend fun getMessages(chatSessionId: UUID): Flow<List<Message>> {
        // Use the local repository as the single source of truth for messages
        // Database reflects the server state through WebSocket status updates
        return localMessageRepository.getMessages(chatSessionId)
    }

    override suspend fun sendMessage(message: Message): Result<Message> {
        return try {
            // First store the message locally with SENDING status
            val messageEntity = tech.ziasvannes.safechat.data.local.entity.MessageEntity(
                id = message.id.toString(),
                content = message.content ?: "",
                timestamp = message.timestamp,
                senderId = message.senderId.toString(),
                receiverId = message.receiverId.toString(),
                status = MessageStatus.SENDING,
                type = message.type,
                encryptedContent = message.encryptedContent,
                iv = message.iv,
                decryptedContent = message.content
            )
            messageDao.insertMessage(messageEntity)
            // Send through WebSocket
            val encryptedContent = Base64.getEncoder().encodeToString(message.encryptedContent)
            val iv = Base64.getEncoder().encodeToString(message.iv)
            
            val type = when (message.type) {
                is MessageType.Text -> "Text"
                is MessageType.Image -> "Image"
                is MessageType.File -> "File"
            }

            webSocketService.sendChatMessage(
                messageId = message.id.toString(),
                receiverId = message.receiverId.toString(),
                type = type,
                encryptedContent = encryptedContent,
                iv = iv
            )

            Result.success(message)
        } catch (e: Exception) {
            // If sending fails, update local message status to FAILED
            try {
                messageDao.updateMessageStatus(message.id.toString(), MessageStatus.FAILED)
            } catch (dbError: Exception) {
                Log.e(TAG, "Failed to update message status to FAILED: ${dbError.message}")
            }
            Log.e(TAG, "Failed to send message via WebSocket: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun updateMessageStatus(messageId: UUID, status: MessageStatus) {
        try {
            // Send status update through WebSocket - server will broadcast to both parties
            webSocketService.updateMessageStatus(messageId.toString(), status.name)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send status update via WebSocket: ${e.message}", e)
        }
    }

    override suspend fun deleteMessage(messageId: UUID) {
        localMessageRepository.deleteMessage(messageId)
    }

    override suspend fun getChatSessions(): Flow<List<ChatSession>> {
        return localMessageRepository.getChatSessions()
    }

    override suspend fun markMessagesAsRead(chatSessionId: UUID) {
        val currentUserId = userSession.userId ?: return

        try {
            // Get unread messages that were received by the current user
            val localMessages = messageDao.getMessagesForChat(chatSessionId.toString()).first()
            val unreadReceivedMessages = localMessages.filter { message ->
                UUID.fromString(message.receiverId) == currentUserId &&
                        message.status != MessageStatus.READ
            }

            if (unreadReceivedMessages.isEmpty()) {
                return
            }

            // Send READ status updates to server via WebSocket
            // Server will update the database and notify both sender and receiver
            unreadReceivedMessages.forEach { message ->
                try {
                    webSocketService.updateMessageStatus(message.id, MessageStatus.READ.name)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send READ status via WebSocket for message ${message.id}: ${e.message}")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark messages as read for chat $chatSessionId: ${e.message}", e)
        }
    }

    suspend fun startObservingWebSocketEvents() {
        webSocketService.events.collect { event ->
            when (event) {
                is WebSocketEvent.NewMessage -> {
                    val messageId = UUID.fromString(event.message.id)
                    val senderId = UUID.fromString(event.message.sender_id)
                    val currentUserId = userSession.userId
                    
                    // Prevent duplicate processing
                    if (processedMessageIds.contains(messageId)) {
                        // Continue to next event instead of exiting collection
                    } else {
                        if (senderId == currentUserId) {
                            // This is a confirmation of our sent message
                            // Update the local message status to SENT
                            try {
                                messageDao.updateMessageStatus(messageId.toString(), MessageStatus.SENT)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to update sent message status: ${e.message}")
                            }
                        } else {
                            // This is a new incoming message
                            storeWebSocketMessage(event.message)
                        }
                        
                        processedMessageIds.add(messageId)
                        
                        // Clean up old processed IDs to prevent memory leaks (keep last 1000)
                        if (processedMessageIds.size > 1000) {
                            val toRemove = processedMessageIds.take(processedMessageIds.size - 1000)
                            processedMessageIds.removeAll(toRemove.toSet())
                        }
                    }
                }
                is WebSocketEvent.StatusUpdate -> {
                    val messageId = UUID.fromString(event.update.message_id)
                    val status = MessageStatus.valueOf(event.update.status)
                    
                    // Update local database with the server status - this is the source of truth
                    try {
                        messageDao.updateMessageStatus(messageId.toString(), status)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to update local message status: ${e.message}", e)
                    }
                }
                is WebSocketEvent.UserOnline -> {
                    // User came online
                }
                is WebSocketEvent.UserOffline -> {
                    // User went offline
                }
                is WebSocketEvent.Connected -> {
                    // WebSocket connected
                }
                is WebSocketEvent.Disconnected -> {
                    // WebSocket disconnected
                }
                is WebSocketEvent.Error -> {
                    Log.e(TAG, "WebSocket error: ${event.error}")
                }
            }
        }
    }

    /**
     * Stores a WebSocket message using the local repository for consistency.
     */
    private suspend fun storeWebSocketMessage(
        wsMessage: tech.ziasvannes.safechat.data.websocket.MessageNotification
    ) {
        try {
            val currentUserId = userSession.userId ?: return

            // Only process messages that involve the current user
            val senderId = UUID.fromString(wsMessage.sender_id)
            val receiverId = UUID.fromString(wsMessage.receiver_id)

            // Skip messages that don't involve the current user
            if (senderId != currentUserId && receiverId != currentUserId) {
                return
            }

            // Determine the correct chat session ID (the other user in the conversation)
            val chatSessionId = if (senderId == currentUserId) receiverId else senderId
            
            // Check if message already exists in database
            val existingMessages = messageDao.getMessagesForChat(chatSessionId.toString()).first()
            val messageExists = existingMessages.any { it.id == wsMessage.id }
            
            if (messageExists) {
                return
            }

            // Convert WebSocket message to domain Message
            val message = convertWebSocketMessageToDomainMessage(wsMessage)
            
            // Handle message content based on whether it's sent or received
            val decryptedContent = if (senderId != currentUserId) {
                // Incoming message - decrypt it
                Log.d(TAG, "Decrypting incoming message ${wsMessage.id}")
                decryptIncomingWebSocketMessage(message)
            } else {
                // Outgoing message confirmation - use original content or try to decrypt if needed
                Log.d(TAG, "Processing sent message confirmation ${wsMessage.id}")
                // For sent messages, we might not have the original content, so we'll store a placeholder
                "[Message sent]" // This will be replaced by the UI with the actual content
            }
            
            // Store directly in database
            val messageEntity = tech.ziasvannes.safechat.data.local.entity.MessageEntity(
                id = message.id.toString(),
                content = decryptedContent ?: "[Processing...]",
                timestamp = message.timestamp,
                senderId = message.senderId.toString(),
                receiverId = message.receiverId.toString(),
                status = message.status,
                type = message.type,
                encryptedContent = message.encryptedContent,
                iv = message.iv,
                decryptedContent = decryptedContent
            )
            
            // Insert into database
            messageDao.insertMessage(messageEntity)
            
            Log.d(TAG, "WebSocket message ${wsMessage.id} stored in database for chat session: $chatSessionId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store WebSocket message ${wsMessage.id}: ${e.message}", e)
        }
    }

    /**
     * Decrypts incoming WebSocket message
     */
    private suspend fun decryptIncomingWebSocketMessage(msg: Message): String? {
        val currentUserId = userSession.userId ?: return null

        // Skip messages with no encrypted content
        if (msg.encryptedContent.isEmpty() || msg.iv.isEmpty()) {
            return "[🔒 Message could not be decrypted - missing encryption data]"
        }

        // Find sender contact
        var senderContact = contactRepository.getContactById(msg.senderId)

        if (senderContact == null) {
            return "[🔒 Message could not be decrypted - sender contact not available]"
        }

        val senderPublicKey = senderContact.publicKey
        if (senderPublicKey.isNullOrBlank()) {
            return "[🔒 Message could not be decrypted - sender key unavailable]"
        }

        // Attempt decryption
        return try {
            val result = (encryptionRepository as? EncryptionRepositoryImpl)
                ?.decryptIncomingMessage(
                    senderPublicKeyBase64 = senderPublicKey,
                    encryptedContent = msg.encryptedContent,
                    iv = msg.iv
                )

            if (result != null) {
                result
            } else {
                "[🔒 Message could not be decrypted - decryption failed]"
            }
        } catch (e: Exception) {
            "[🔒 Message could not be decrypted - ${e.message?.take(50) ?: "unknown error"}]"
        }
    }

    /**
     * Converts WebSocket message to domain Message
     */
    private fun convertWebSocketMessageToDomainMessage(
        wsMessage: tech.ziasvannes.safechat.data.websocket.MessageNotification
    ): Message {
        val decodedEncryptedContent = try {
            Base64.getDecoder().decode(wsMessage.encrypted_content)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Failed to decode encrypted_content for WebSocket message ${wsMessage.id}", e)
            ByteArray(0)
        }

        val decodedIv = try {
            Base64.getDecoder().decode(wsMessage.iv)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Failed to decode IV for WebSocket message ${wsMessage.id}", e)
            ByteArray(0)
        }

        return Message(
            id = UUID.fromString(wsMessage.id),
            content = null, // Will be set by decryption
            timestamp = wsMessage.timestamp.toLong(),
            senderId = UUID.fromString(wsMessage.sender_id),
            receiverId = UUID.fromString(wsMessage.receiver_id),
            status = MessageStatus.valueOf(wsMessage.status),
            type = when (wsMessage.type) {
                "Text" -> MessageType.Text
                "Image" -> MessageType.Image("")
                "File" -> MessageType.File("", "", 0)
                else -> MessageType.Text
            },
            encryptedContent = decodedEncryptedContent,
            iv = decodedIv
        )
    }
}