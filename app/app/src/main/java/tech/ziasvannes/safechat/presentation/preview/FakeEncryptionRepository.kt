package tech.ziasvannes.safechat.presentation.preview

import tech.ziasvannes.safechat.domain.repository.EncryptionRepository
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey

/**
 * A fake implementation of EncryptionRepository for use in UI previews only.
 * This should NOT be used in the actual app's dependency injection system.
 */
class FakeEncryptionRepository : EncryptionRepository {
    override suspend fun generateKeyPair(): KeyPair {
        throw NotImplementedError("Preview only - not actually implemented")
    }

    override suspend fun storeKeyPair(keyPair: KeyPair) {
        // No-op for previews
    }

    override suspend fun getPublicKey(): PublicKey {
        throw NotImplementedError("Preview only - not actually implemented")
    }

    override suspend fun getPrivateKey(): PrivateKey {
        throw NotImplementedError("Preview only - not actually implemented")
    }

    override suspend fun computeSharedSecret(publicKey: PublicKey): ByteArray {
        return byteArrayOf(1, 2, 3) // Fake data for previews
    }

    override suspend fun encryptMessage(message: String, sharedSecret: ByteArray): Pair<ByteArray, ByteArray> {
        return Pair(byteArrayOf(1, 2, 3), byteArrayOf(4, 5, 6)) // Fake data for previews
    }

    override suspend fun decryptMessage(encryptedContent: ByteArray, iv: ByteArray, sharedSecret: ByteArray): String {
        return "Decrypted message" // Fake data for previews
    }

    override suspend fun getCurrentPublicKey(): String? {
        return "MIIBCgKCAQEA4QGIGZgGFZ3WGgJj9S/U0UEM9nENA9aEcUbf9ToaOMP3GgG6"
    }

    override suspend fun generateNewKeyPair(): String {
        return "ABCDEFG1234567890ThisIsANewlyGeneratedFakePublicKeyForPreviewsOnly"
    }
}