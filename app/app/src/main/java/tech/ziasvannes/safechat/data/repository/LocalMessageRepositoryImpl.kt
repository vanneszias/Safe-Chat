package tech.ziasvannes.safechat.data.repository

import java.util.Base64
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import tech.ziasvannes.safechat.data.local.dao.MessageDao
import tech.ziasvannes.safechat.data.local.entity.MessageEntity
import tech.ziasvannes.safechat.data.models.ChatSession
import tech.ziasvannes.safechat.data.models.ContactStatus
import tech.ziasvannes.safechat.data.models.EncryptionStatus
import tech.ziasvannes.safechat.data.models.Message
import tech.ziasvannes.safechat.data.models.MessageStatus
import tech.ziasvannes.safechat.data.models.MessageType
import tech.ziasvannes.safechat.data.remote.ApiService
import tech.ziasvannes.safechat.data.remote.MessageResponse
import tech.ziasvannes.safechat.domain.repository.ContactRepository
import tech.ziasvannes.safechat.domain.repository.EncryptionRepository
import tech.ziasvannes.safechat.domain.repository.MessageRepository
import tech.ziasvannes.safechat.session.UserSession

class LocalMessageRepositoryImpl
@Inject
constructor(
        private val messageDao: MessageDao,
        private val encryptionRepository: EncryptionRepository,
        private val contactRepository: ContactRepository,
        private val userSession: UserSession,
        private val apiService: ApiService
) : MessageRepository {
    override suspend fun getMessages(chatSessionId: UUID): Flow<List<Message>> {
        return flow {
            // 1. First, fetch remote messages and update local DB if new (one-time sync)
            syncRemoteMessages(chatSessionId)
            
            // 2. Then start observing database changes and emit all updates
            messageDao
                .getMessagesForChat(chatSessionId.toString())
                .map { entities -> entities.map { entity -> entity.toMessage() } }
                .collect { emit(it) }
        }
    }
    
    private suspend fun syncRemoteMessages(chatSessionId: UUID) {
        val remoteApiMessages =
                try {
                    apiService.getMessages(chatSessionId.toString())
                } catch (e: Exception) {
                    android.util.Log.w(
                            "LocalMessageRepository",
                            "Failed to fetch remote messages: ${e.message}"
                    )
                    return
                }

        val localMessages = messageDao.getMessagesForChat(chatSessionId.toString()).first()
        val localIds = localMessages.map { it.id }.toSet()
        val currentUserId = userSession.userId ?: UUID(0, 0)

        // Convert API messages to domain messages and filter new ones
        val remoteMessages = remoteApiMessages.map { it.toMessage() }
        val newMessages = remoteMessages.filter { msg -> msg.id.toString() !in localIds }

        android.util.Log.d(
                "LocalMessageRepository",
                "Found ${newMessages.size} new messages out of ${remoteMessages.size} remote messages"
        )

        for (msg in newMessages) {
            // Double-check for existence (safety net)
            val exists =
                    messageDao.getMessagesForChat(chatSessionId.toString()).first().any {
                        it.id == msg.id.toString()
                    }
            if (exists) {
                android.util.Log.d(
                        "LocalMessageRepository",
                        "Message ${msg.id} already exists, skipping"
                )
                continue
            }

            val isIncomingMessage = msg.receiverId == currentUserId
            val isOutgoingMessage = msg.senderId == currentUserId

            android.util.Log.d(
                    "LocalMessageRepository",
                    "Processing message ${msg.id}: incoming=$isIncomingMessage, outgoing=$isOutgoingMessage"
            )

            var decryptedContent: String? = null
            var shouldStoreMessage = true

            when {
                isIncomingMessage -> {
                    // Handle incoming messages
                    decryptedContent = handleIncomingMessage(msg)
                    // Always store incoming messages, even if decryption fails
                    // We'll show an error state in the UI
                }
                isOutgoingMessage -> {
                    // Handle outgoing messages
                    decryptedContent = msg.content
                    // Only skip outgoing messages if they're completely corrupted
                    if (msg.content.isNullOrBlank() && msg.encryptedContent.isEmpty()) {
                        android.util.Log.w(
                                "LocalMessageRepository",
                                "Skipping completely corrupted outgoing message ${msg.id}"
                        )
                        shouldStoreMessage = false
                    }
                }
                else -> {
                    android.util.Log.w(
                            "LocalMessageRepository",
                            "Message ${msg.id} has unclear direction: senderId=${msg.senderId}, receiverId=${msg.receiverId}, currentUserId=$currentUserId"
                    )
                    // Still try to store it
                    decryptedContent = msg.content
                }
            }

            if (shouldStoreMessage) {
                val entity =
                        MessageEntity(
                                id = msg.id.toString(),
                                content = msg.content ?: "",
                                timestamp = msg.timestamp,
                                senderId = msg.senderId.toString(),
                                receiverId = msg.receiverId.toString(),
                                status = msg.status,
                                type = msg.type,
                                encryptedContent = msg.encryptedContent,
                                iv = msg.iv,
                                decryptedContent =
                                        decryptedContent // Store with decrypted content like static
                                // routes
                                )

                android.util.Log.d(
                        "LocalMessageRepository",
                        "Storing message ${msg.id} - decrypted: ${decryptedContent != null}"
                )
                messageDao.insertMessage(entity)
            }
        }
    }

    private suspend fun handleIncomingMessage(msg: Message): String? {
        val currentUserId = userSession.userId ?: return null

        // Skip messages with no encrypted content (these are definitely corrupted)
        if (msg.encryptedContent.isEmpty() || msg.iv.isEmpty()) {
            android.util.Log.w(
                    "LocalMessageRepository",
                    "Message ${msg.id} has no encrypted content - storing with error state"
            )
            return "[🔒 Message could not be decrypted - missing encryption data]"
        }

        // Find or fetch sender contact
        var senderContact = contactRepository.getContactById(msg.senderId)

        if (senderContact == null) {
            android.util.Log.d(
                    "LocalMessageRepository",
                    "Sender contact not found for ID ${msg.senderId}, fetching from server"
            )

            senderContact = fetchContactFromServer(msg.senderId)
        }

        val senderPublicKey = senderContact?.publicKey
        if (senderPublicKey.isNullOrBlank()) {
            android.util.Log.w(
                    "LocalMessageRepository",
                    "No public key available for sender ${msg.senderId} - storing with error state"
            )
            return "[🔒 Message could not be decrypted - sender key unavailable]"
        }

        // Attempt decryption
        return try {
            android.util.Log.d(
                    "LocalMessageRepository",
                    "Attempting to decrypt incoming message ${msg.id}"
            )

            val result =
                    (encryptionRepository as?
                                    tech.ziasvannes.safechat.data.repository.EncryptionRepositoryImpl)
                            ?.decryptIncomingMessage(
                                    senderPublicKeyBase64 = senderPublicKey,
                                    encryptedContent = msg.encryptedContent,
                                    iv = msg.iv
                            )

            if (result != null) {
                android.util.Log.d(
                        "LocalMessageRepository",
                        "Successfully decrypted incoming message ${msg.id}"
                )
                result
            } else {
                android.util.Log.w(
                        "LocalMessageRepository",
                        "Decryption returned null for message ${msg.id}"
                )
                "[🔒 Message could not be decrypted - decryption failed]"
            }
        } catch (e: Exception) {
            android.util.Log.e(
                    "LocalMessageRepository",
                    "Failed to decrypt incoming message ${msg.id}: ${e.message}",
                    e
            )
            "[🔒 Message could not be decrypted - ${e.message?.take(50) ?: "unknown error"}]"
        }
    }

    private suspend fun fetchContactFromServer(
            senderId: UUID
    ): tech.ziasvannes.safechat.data.models.Contact? {
        return try {
            val userResponse = apiService.getUserById(senderId.toString())

            val newContact =
                    tech.ziasvannes.safechat.data.models.Contact(
                            id = UUID.fromString(userResponse.id),
                            name = userResponse.username,
                            publicKey = userResponse.public_key,
                            lastSeen = System.currentTimeMillis(),
                            status = ContactStatus.ONLINE,
                            avatar = userResponse.avatar
                    )

            contactRepository.addContact(newContact)
            android.util.Log.d(
                    "LocalMessageRepository",
                    "Added new contact: ${newContact.name} with ID ${newContact.id}"
            )

            newContact
        } catch (e: Exception) {
            android.util.Log.e(
                    "LocalMessageRepository",
                    "Failed to fetch contact info for $senderId: ${e.message}",
                    e
            )
            null
        }
    }

    override suspend fun sendMessage(message: Message): Result<Message> {
        // This method is only used for local storage - WebSocketMessageRepositoryImpl handles actual sending
        // Save locally with SENDING status - WebSocket layer will update to SENT when server confirms
        messageDao.insertMessage(
                MessageEntity(
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
        )

        return Result.success(message)
    }

    override suspend fun updateMessageStatus(messageId: UUID, status: MessageStatus) {
        messageDao.updateMessageStatus(messageId.toString(), status)
    }

    override suspend fun deleteMessage(messageId: UUID) {
        val local = messageDao.getMessagesForChat("").first().find { it.id == messageId.toString() }
        if (local != null) messageDao.deleteMessage(local)
    }

    override suspend fun getChatSessions(): Flow<List<ChatSession>> = flow {
        messageDao
                .getChatSessions()
                .map { entities ->
                    entities.map { entity ->
                        ChatSession(
                                id = UUID.fromString(entity.id),
                                participantIds =
                                        listOf(
                                                UUID.fromString(entity.senderId),
                                                UUID.fromString(entity.receiverId)
                                        ),
                                lastMessage = entity.toMessage(),
                                unreadCount = 0,
                                encryptionStatus = EncryptionStatus.ENCRYPTED
                        )
                    }
                }
                .collect { emit(it) }
    }

    /**
     * Marks received messages as read for the specified chat session. Updates status locally
     * and notifies the server. Messages are kept locally for chat history.
     */
    override suspend fun markMessagesAsRead(chatSessionId: UUID) {
        val currentUserId = userSession.userId ?: return

        try {
            val localMessages = messageDao.getMessagesForChat(chatSessionId.toString()).first()
            val unreadReceivedMessages =
                    localMessages.filter { message ->
                        UUID.fromString(message.receiverId) == currentUserId &&
                                message.status != MessageStatus.READ
                    }

            if (unreadReceivedMessages.isEmpty()) {
                android.util.Log.d(
                        "LocalMessageRepository",
                        "No unread messages for chat $chatSessionId"
                )
                return
            }

            // Update local database status to READ first (for immediate UI update)
            unreadReceivedMessages.forEach { message ->
                messageDao.updateMessageStatus(message.id, MessageStatus.READ)
            }

            android.util.Log.d(
                    "LocalMessageRepository",
                    "Marked ${unreadReceivedMessages.size} messages as read locally for chat $chatSessionId"
            )

            // WebSocket will handle server-side status updates and deletion
            android.util.Log.d(
                    "LocalMessageRepository",
                    "Messages marked as read - WebSocket will handle server notification and deletion."
            )

            // Messages are kept locally after being marked as read
            // They will remain visible in the chat history

            android.util.Log.d(
                    "LocalMessageRepository",
                    "Completed read processing for ${unreadReceivedMessages.size} messages in chat $chatSessionId - messages marked as read and kept locally"
            )
        } catch (e: Exception) {
            android.util.Log.w(
                    "LocalMessageRepository",
                    "Failed to mark messages as read for chat $chatSessionId",
                    e
            )
        }
    }

    /**
     * Optional: Delete messages that have been read (if this behavior is desired) Call this
     * separately from markMessagesAsRead if you want to delete read messages
     */
    suspend fun deleteReadMessages(chatSessionId: UUID) {
        val currentUserId = userSession.userId ?: return

        try {
            val localMessages = messageDao.getMessagesForChat(chatSessionId.toString()).first()
            val readReceivedMessages =
                    localMessages.filter { message ->
                        UUID.fromString(message.receiverId) == currentUserId &&
                                message.status == MessageStatus.READ
                    }

            readReceivedMessages.forEach { message ->
                messageDao.deleteMessage(message)
                android.util.Log.d("LocalMessageRepository", "Deleted read message ${message.id}")
            }

            if (readReceivedMessages.isNotEmpty()) {
                android.util.Log.d(
                        "LocalMessageRepository",
                        "Deleted ${readReceivedMessages.size} read messages for chat $chatSessionId"
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(
                    "LocalMessageRepository",
                    "Failed to delete read messages for chat $chatSessionId",
                    e
            )
        }
    }

    /** Cleans up only truly corrupted messages that cannot be displayed */
    suspend fun cleanupCorruptedMessages() {
        try {
            val allMessages = messageDao.getAllMessages().first()
            val corruptedMessages =
                    allMessages.filter { message ->
                        // Only delete messages that are completely unusable
                        val hasNoContent =
                                message.content.isBlank() &&
                                        message.decryptedContent.isNullOrBlank() &&
                                        message.encryptedContent.isEmpty()

                        val hasDuplicateId = allMessages.count { it.id == message.id } > 1

                        hasNoContent || hasDuplicateId
                    }

            if (corruptedMessages.isNotEmpty()) {
                android.util.Log.i(
                        "LocalMessageRepository",
                        "Cleaning up ${corruptedMessages.size} truly corrupted messages"
                )

                corruptedMessages.forEach { message ->
                    android.util.Log.d(
                            "LocalMessageRepository",
                            "Removing corrupted message ${message.id}"
                    )
                    messageDao.deleteMessage(message)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("LocalMessageRepository", "Failed to cleanup corrupted messages", e)
        }
    }
}

/** Converts a [MessageResponse] from the remote API into a [Message] domain model. */
private fun MessageResponse.toMessage(): Message {
    // Safe Base64 decoding with error handling
    val decodedEncryptedContent =
            try {
                Base64.getDecoder().decode(encrypted_content)
            } catch (e: IllegalArgumentException) {
                android.util.Log.w(
                        "LocalMessageRepository",
                        "Failed to decode encrypted_content for message $id",
                        e
                )
                ByteArray(0) // Return empty array if decoding fails
            }

    val decodedIv =
            try {
                Base64.getDecoder().decode(iv)
            } catch (e: IllegalArgumentException) {
                android.util.Log.w(
                        "LocalMessageRepository",
                        "Failed to decode IV for message $id",
                        e
                )
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
                        "Image" -> MessageType.Image("")
                        "File" -> MessageType.File("", "", 0)
                        else -> MessageType.Text
                    },
            encryptedContent = decodedEncryptedContent,
            iv = decodedIv
    )
}
