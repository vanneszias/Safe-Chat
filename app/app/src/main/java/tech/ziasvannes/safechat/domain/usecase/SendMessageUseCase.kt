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
         * Retrieves the recipient's contact information, computes a shared encryption secret using
         * the recipient's public key, encrypts the message content, constructs a new message
         * object, and sends it via the message repository. Returns the result of the send
         * operation.
         *
         * @param content The plaintext content of the message to send.
         * @param receiverId The UUID of the message recipient.
         * @param type The type of message to send (defaults to text).
         * @return A [Result] containing the sent [Message] on success, or an exception on failure.
         */

        suspend operator fun invoke(
                content: String,
                receiverId: UUID,
                type: MessageType = MessageType.Text
        ): Result<Message> = runCatching {
                // Get recipient's contact (must have publicKey)
                val recipient =
                        requireNotNull(contactRepository.getContactById(receiverId)) {
                                "Recipient not found"
                        }
                val recipientPublicKeyBase64 = recipient.publicKey
                require(recipientPublicKeyBase64.isNotBlank()) { "Recipient public key missing" }

                // Reconstruct recipient's PublicKey
                val recipientPublicKey =
                        encryptionRepository.let {
                                (it as?
                                                tech.ziasvannes.safechat.data.repository.EncryptionRepositoryImpl)
                                        ?.publicKeyFromBase64(recipientPublicKeyBase64)
                        }
                                ?: throw IllegalStateException(
                                        "Failed to reconstruct recipient public key"
                                )

                // Compute shared secret
                val sharedSecret = encryptionRepository.computeSharedSecret(recipientPublicKey)

                // Encrypt the message
                val (encryptedContent, iv) =
                        encryptionRepository.encryptMessage(content, sharedSecret)

                // Create and send message (preserve plaintext content for local storage)
                val message =
                        Message(
                                id = UUID.randomUUID(),
                                content = content, // Pass the original plaintext content
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
