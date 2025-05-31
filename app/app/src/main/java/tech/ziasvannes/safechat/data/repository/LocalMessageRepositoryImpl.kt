package tech.ziasvannes.safechat.data.repository

import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import tech.ziasvannes.safechat.data.local.dao.MessageDao
import tech.ziasvannes.safechat.data.local.entity.MessageEntity
import tech.ziasvannes.safechat.data.models.ChatSession
import tech.ziasvannes.safechat.data.models.Message
import tech.ziasvannes.safechat.data.models.MessageStatus
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
        private val remoteRepository: MessageRepository // For remote sync
) : MessageRepository {
        override suspend fun getMessages(chatSessionId: UUID): Flow<List<Message>> = flow {
                // 1. Fetch remote messages and update local DB if new
                val remoteMessages = remoteRepository.getMessages(chatSessionId).first()
                val localMessages = messageDao.getMessagesForChat(chatSessionId.toString()).first()
                val localIds = localMessages.map { it.id }.toSet()
                
                // Filter out self-sent messages to prevent duplicates
                val currentUserId = userSession.userId ?: UUID(0, 0)
                val newMessages = remoteMessages.filter { msg ->
                    msg.id.toString() !in localIds && msg.senderId != currentUserId
                }
                
                for (msg in newMessages) {
                        // Deduplication: check if message already exists
                        val exists =
                                messageDao
                                        .getMessagesForChat(chatSessionId.toString())
                                        .first()
                                        .any { it.id == msg.id.toString() }
                        if (exists) continue
                        
                        // For received messages, decrypt using sender's public key
                        val contact = contactRepository.getContactById(msg.senderId)
                        val senderPublicKey = contact?.publicKey

                        val decrypted = try {
                                if (senderPublicKey != null) {
                                        (encryptionRepository as?
                                                        tech.ziasvannes.safechat.data.repository.EncryptionRepositoryImpl)
                                                ?.decryptIncomingMessage(
                                                        senderPublicKeyBase64 =
                                                                senderPublicKey,
                                                        encryptedContent =
                                                                msg.encryptedContent,
                                                        iv = msg.iv
                                                )
                                } else {
                                        android.util.Log.w(
                                                "LocalMessageRepository",
                                                "Missing public key for message ${msg.id} from sender ${msg.senderId}. " +
                                                        "Contact may need to be added manually to decrypt messages."
                                        )
                                        null
                                }
                        } catch (e: Exception) {
                                android.util.Log.e(
                                        "LocalMessageRepository",
                                        "Failed to decrypt message ${msg.id} from sender ${msg.senderId}",
                                        e
                                )
                                null
                        }
                        
                        val entity =
                                MessageEntity(
                                        id = msg.id.toString(),
                                        content = msg.content,
                                        timestamp = msg.timestamp,
                                        senderId = msg.senderId.toString(),
                                        receiverId = msg.receiverId.toString(),
                                        status = msg.status,
                                        type = msg.type,
                                        encryptedContent = msg.encryptedContent,
                                        iv = msg.iv,
                                        decryptedContent = decrypted
                                )
                        messageDao.insertMessage(entity)
                }
                // 2. Only emit all local messages as domain models (never remote)
                messageDao
                        .getMessagesForChat(chatSessionId.toString())
                        .map { list -> list.map { it.toMessage() } }
                        .collect { emit(it) }
        }

        override suspend fun sendMessage(message: Message): Result<Message> {
                // Save locally with original plaintext for the sender's copy
                messageDao.insertMessage(
                        MessageEntity(
                                id = message.id.toString(),
                                content = message.content, // This is now the original plaintext
                                timestamp = message.timestamp,
                                senderId = message.senderId.toString(),
                                receiverId = message.receiverId.toString(),
                                status = message.status,
                                type = message.type,
                                encryptedContent = message.encryptedContent,
                                iv = message.iv,
                                decryptedContent =
                                        message.content // Use original plaintext for local storage
                        )
                )
                // Then send remotely
                return remoteRepository.sendMessage(message)
        }

        override suspend fun updateMessageStatus(messageId: UUID, status: MessageStatus) {
                messageDao.updateMessageStatus(messageId.toString(), status)
        }

        override suspend fun deleteMessage(messageId: UUID) {
                val local =
                        messageDao.getMessagesForChat("").first().find {
                                it.id == messageId.toString()
                        }
                if (local != null) messageDao.deleteMessage(local)
        }

        override suspend fun getChatSessions(): Flow<List<ChatSession>> = flow {
                // Map MessageEntity to ChatSession
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
                                                unreadCount = 0, // Not tracked locally
                                                encryptionStatus =
                                                        tech.ziasvannes.safechat.data.models
                                                                .EncryptionStatus
                                                                .ENCRYPTED // Assume encrypted
                                        )
                                }
                        }
                        .collect { emit(it) }
        }
}
