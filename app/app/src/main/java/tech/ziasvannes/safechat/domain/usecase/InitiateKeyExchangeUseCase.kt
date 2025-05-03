package tech.ziasvannes.safechat.domain.usecase

import tech.ziasvannes.safechat.data.models.EncryptionStatus
import tech.ziasvannes.safechat.domain.repository.ContactRepository
import tech.ziasvannes.safechat.domain.repository.EncryptionRepository
import java.util.UUID
import javax.inject.Inject

class InitiateKeyExchangeUseCase @Inject constructor(
    private val encryptionRepository: EncryptionRepository,
    private val contactRepository: ContactRepository
) {
    /**
     * Initiates a key exchange process for the specified contact.
     *
     * Generates a new cryptographic key pair, securely stores it, and updates the contact's public key with the newly generated public key. Returns a [Result] indicating success or failure of the operation.
     *
     * @param contactId The unique identifier of the contact for whom the key exchange is initiated.
     * @return A [Result] containing [Unit] on success, or an exception if the process fails.
     */
    suspend operator fun invoke(contactId: UUID): Result<Unit> = runCatching {
        // Generate new key pair for this exchange
        val keyPair = encryptionRepository.generateKeyPair()

        // Store the key pair securely
        encryptionRepository.storeKeyPair(keyPair)

        // Update contact with our public key and encryption status
        val contact = requireNotNull(contactRepository.getContactById(contactId)) {
            "Contact not found"
        }

        contactRepository.updateContact(
            contact.copy(
                publicKey = keyPair.public.encoded.toString(),
            )
        )
    }
}