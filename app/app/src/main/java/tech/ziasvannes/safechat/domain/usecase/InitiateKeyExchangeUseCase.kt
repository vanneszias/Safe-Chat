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