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
    /**
     * Throws a [NotImplementedError] to indicate key pair generation is not supported in preview mode.
     *
     * This method is a stub and does not perform any cryptographic operations.
     * Intended for UI preview use only.
     * @throws NotImplementedError always, as this function is not implemented.
     */
    override suspend fun generateKeyPair(): KeyPair {
        throw NotImplementedError("Preview only - not actually implemented")
    }

    /**
     * Stub method for storing a key pair; performs no operation in preview mode.
     *
     * This implementation is intended for UI previews and does not persist any data.
     */
    override suspend fun storeKeyPair(keyPair: KeyPair) {
        // No-op for previews
    }

    /**
     * Throws a [NotImplementedError] as public key retrieval is not supported in preview mode.
     *
     * This method is a stub and does not provide a real public key.
     * Intended for UI preview use only.
     * @throws NotImplementedError always.
     */
    override suspend fun getPublicKey(): PublicKey {
        throw NotImplementedError("Preview only - not actually implemented")
    }

    /**
     * Throws a [NotImplementedError] as private key retrieval is not supported in preview mode.
     *
     * @throws NotImplementedError Always thrown since this method is not implemented for previews.
     */
    override suspend fun getPrivateKey(): PrivateKey {
        throw NotImplementedError("Preview only - not actually implemented")
    }

    /**
     * Returns a fixed byte array as a fake shared secret for UI preview purposes.
     *
     * This method does not perform any real cryptographic computation and should not be used in production.
     *
     * @return A dummy byte array representing a shared secret.
     */
    override suspend fun computeSharedSecret(publicKey: PublicKey): ByteArray {
        return byteArrayOf(1, 2, 3) // Fake data for previews
    }

    /**
     * Returns fixed fake encrypted content and IV for UI preview purposes.
     *
     * Always returns a pair of byte arrays ([1, 2, 3], [4, 5, 6]) regardless of input.
     *
     * @return A pair containing fake encrypted content and initialization vector.
     */
    override suspend fun encryptMessage(message: String, sharedSecret: ByteArray): Pair<ByteArray, ByteArray> {
        return Pair(byteArrayOf(1, 2, 3), byteArrayOf(4, 5, 6)) // Fake data for previews
    }

    /**
     * Returns a fixed string simulating decrypted message content for UI previews.
     *
     * This method does not perform real decryption and always returns "Decrypted message".
     */
    override suspend fun decryptMessage(encryptedContent: ByteArray, iv: ByteArray, sharedSecret: ByteArray): String {
        return "Decrypted message" // Fake data for previews
    }

    /**
     * Returns a fixed fake public key string for UI preview purposes.
     *
     * @return A hardcoded public key string, or null if not available.
     */
    override suspend fun getCurrentPublicKey(): String? {
        return "MIIBCgKCAQEA4QGIGZgGFZ3WGgJj9S/U0UEM9nENA9aEcUbf9ToaOMP3GgG6"
    }

    /**
     * Returns a fixed string representing a newly generated fake public key for UI preview purposes.
     *
     * This method does not perform any real cryptographic key generation and should not be used in production.
     *
     * @return A hardcoded string simulating a new public key.
     */
    override suspend fun generateNewKeyPair(): String {
        return "ABCDEFG1234567890ThisIsANewlyGeneratedFakePublicKeyForPreviewsOnly"
    }
}