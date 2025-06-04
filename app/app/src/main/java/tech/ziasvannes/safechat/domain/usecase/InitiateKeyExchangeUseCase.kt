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
     * Initiates a key exchange for the specified contact by generating a new cryptographic key pair.
     *
     * Generates a new key pair and securely stores it. The contact retains their existing public key.
     * This prepares our side for encrypted communication with the specified contact.
     *
     * @param contactId The UUID of the contact for whom the key exchange is initiated.
     * @return A [Result] indicating success or failure of the operation.
     */
    suspend operator fun invoke(contactId: UUID): Result<Unit> = runCatching {
        // Verify contact exists
        val contact = requireNotNull(contactRepository.getContactById(contactId)) {
            "Contact not found"
        }

        // Verify contact has a valid public key
        require(contact.publicKey.isNotBlank()) {
            "Contact does not have a public key"
        }

        // Generate new key pair for this exchange (or ensure we have one)
        val keyPair = encryptionRepository.generateKeyPair()

        // Store the key pair securely
        encryptionRepository.storeKeyPair(keyPair)

        // Key exchange is complete - we have our keys and the contact has theirs
        // No need to update the contact's public key as it should remain unchanged
    }
}