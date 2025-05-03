package tech.ziasvannes.safechat.domain.usecase

import tech.ziasvannes.safechat.data.models.Message
import tech.ziasvannes.safechat.data.models.MessageStatus
import tech.ziasvannes.safechat.data.models.MessageType
import tech.ziasvannes.safechat.domain.repository.ContactRepository
import tech.ziasvannes.safechat.domain.repository.EncryptionRepository
import tech.ziasvannes.safechat.domain.repository.MessageRepository
import java.util.UUID
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val encryptionRepository: EncryptionRepository,
    private val contactRepository: ContactRepository
) {
    /**
     * Sends an encrypted message to a specified recipient.
     *
     * Retrieves the recipient's contact and public key, computes a shared secret, encrypts the message content,
     * constructs a new message, and sends it. Returns the result of the send operation or propagates any encountered exception.
     *
     * @param content The plaintext content of the message to send.
     * @param receiverId The unique identifier of the message recipient.
     * @param type The type of message to send (defaults to text).
     * @return A [Result] containing the sent [Message] on success, or an exception on failure.
     */
    suspend operator fun invoke(
        content: String,
        receiverId: UUID,
        type: MessageType = MessageType.Text
    ): Result<Message> = runCatching {
        // Get recipient's public key
        val recipient = requireNotNull(contactRepository.getContactById(receiverId)) {
            "Recipient not found"
        }

        // Compute shared secret for encryption
        val recipientPublicKey = java.security.KeyFactory.getInstance("DH")
            .generatePublic(java.security.spec.X509EncodedKeySpec(recipient.publicKey.toByteArray()))
        val sharedSecret = encryptionRepository.computeSharedSecret(recipientPublicKey)

        // Encrypt message content
        val (encryptedContent, iv) = encryptionRepository.encryptMessage(content, sharedSecret)

        // Create and send message
        val message = Message(
            id = UUID.randomUUID(),
            content = content,
            timestamp = System.currentTimeMillis(),
            senderId = UUID.randomUUID(), // TODO: Replace with actual user ID
            receiverId = receiverId,
            status = MessageStatus.SENDING,
            type = type,
            encryptedContent = encryptedContent,
            iv = iv
        )

        messageRepository.sendMessage(message).getOrThrow()
    }
}