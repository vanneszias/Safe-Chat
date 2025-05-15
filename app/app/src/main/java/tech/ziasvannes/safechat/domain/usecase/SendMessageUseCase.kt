package tech.ziasvannes.safechat.domain.usecase

import java.util.UUID
import javax.inject.Inject
import tech.ziasvannes.safechat.data.models.Message
import tech.ziasvannes.safechat.data.models.MessageStatus
import tech.ziasvannes.safechat.data.models.MessageType
import tech.ziasvannes.safechat.domain.repository.ContactRepository
import tech.ziasvannes.safechat.domain.repository.EncryptionRepository
import tech.ziasvannes.safechat.domain.repository.MessageRepository
import tech.ziasvannes.safechat.session.UserSession
import android.util.Base64

class SendMessageUseCase
@Inject
constructor(
        private val messageRepository: MessageRepository,
        private val encryptionRepository: EncryptionRepository,
        private val contactRepository: ContactRepository,
        private val userSession: UserSession
) {
    /**
     * Sends an encrypted message to a specified recipient.
     *
     * Retrieves the recipient's contact information, computes a shared encryption secret using the
     * recipient's public key, encrypts the message content, constructs a new message object, and
     * sends it via the message repository. Returns the result of the send operation.
     *
     * @param content The plaintext content of the message to send.
     * @param receiverId The UUID of the message recipient.
     * @param type The type of message to send (defaults to text).
     * @return A [Result] containing the sent [Message] on success, or an exception on failure.
     */
    /**
     * Sends a plaintext message to a specified recipient, bypassing encryption.
     *
     * Retrieves the recipient's contact information and constructs a new message using the current user's ID as the sender. The message is sent via the message repository with empty encryption fields. Throws an exception if the recipient is not found or if the user ID is not set in the session.
     *
     * @param content The plaintext content of the message to send.
     * @param receiverId The UUID of the message recipient.
     * @param type The type of the message, defaulting to text.
     * @return A [Result] containing the sent [Message] on success, or an exception on failure.
     */
    suspend operator fun invoke(
            content: String,
            receiverId: UUID,
            type: MessageType = MessageType.Text
    ): Result<Message> = runCatching {
        // Get recipient's contact (no encryption for now)
        val recipient =
                requireNotNull(contactRepository.getContactById(receiverId)) {
                    "Recipient not found"
                }

        // Bypass encryption: send plaintext, empty encryptedContent and iv
        val encryptedContent = ByteArray(0)
        val iv = ByteArray(0)

        // Create and send message
        val message =
                Message(
                        id = UUID.randomUUID(),
                        content = content,
                        timestamp = System.currentTimeMillis(),
                        senderId = userSession.userId
                                        ?: throw IllegalStateException(
                                                "User ID not set in session"
                                        ),
                        receiverId = receiverId,
                        status = MessageStatus.SENDING,
                        type = type,
                        encryptedContent = encryptedContent,
                        iv = iv
                )

        messageRepository.sendMessage(message).getOrThrow()
    }
}
