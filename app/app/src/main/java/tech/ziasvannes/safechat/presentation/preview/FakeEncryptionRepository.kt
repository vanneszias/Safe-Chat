package tech.ziasvannes.safechat.presentation.preview

import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import tech.ziasvannes.safechat.domain.repository.EncryptionRepository

/**
 * A fake implementation of EncryptionRepository for use in UI previews only. This should NOT be
 * used in the actual app's dependency injection system.
 */
class FakeEncryptionRepository : EncryptionRepository {
    /**
     * Throws a [NotImplementedError] to indicate key pair generation is not supported in preview
     * mode.
     *
     * This method is intended only for UI previews and does not perform any cryptographic
     * operations.
     * @throws NotImplementedError always, as this function is not implemented.
     */
    override suspend fun generateKeyPair(): KeyPair {
        throw NotImplementedError("Preview only - not actually implemented")
    }

    /**
     * Does nothing; included to satisfy the interface for UI preview purposes.
     *
     * This method does not store the provided key pair and has no effect in this fake
     * implementation.
     */
    override suspend fun storeKeyPair(keyPair: KeyPair) {
        // No-op for previews
    }

    /**
     * Throws a [NotImplementedError] as this method is not implemented in the preview repository.
     *
     * Intended only for UI preview scenarios; does not return a real public key.
     * @throws NotImplementedError always.
     */
    override suspend fun getPublicKey(): PublicKey {
        throw NotImplementedError("Preview only - not actually implemented")
    }

    /**
     * Throws a [NotImplementedError] as private key retrieval is not supported in this preview
     * implementation.
     *
     * This method is intended solely for UI preview scenarios and does not provide actual
     * cryptographic functionality.
     */
    override suspend fun getPrivateKey(): PrivateKey {
        throw NotImplementedError("Preview only - not actually implemented")
    }

    /**
     * Returns a fixed byte array as a fake shared secret for UI previews.
     *
     * @param publicKey The public key to simulate shared secret computation with.
     * @return A dummy byte array representing the shared secret.
     */
    override suspend fun computeSharedSecret(publicKey: PublicKey): ByteArray {
        return byteArrayOf(1, 2, 3) // Fake data for previews
    }

    /**
     * Returns a fixed pair of byte arrays simulating encrypted content and IV for UI preview
     * purposes.
     *
     * @return A pair of byte arrays representing fake encrypted data and initialization vector.
     */
    override suspend fun encryptMessage(
            message: String,
            sharedSecret: ByteArray
    ): Pair<ByteArray, ByteArray> {
        return Pair(byteArrayOf(1, 2, 3), byteArrayOf(4, 5, 6)) // Fake data for previews
    }

    /**
     * Returns a fixed string simulating decrypted content for UI previews.
     *
     * Always returns "Decrypted message" regardless of input.
     */
    override suspend fun decryptMessage(
            encryptedContent: ByteArray,
            iv: ByteArray,
            sharedSecret: ByteArray
    ): String {
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
     * Returns a fixed string representing a newly generated fake public key for UI preview
     * purposes.
     *
     * This method does not perform any real cryptographic key generation and should not be used in
     * production code.
     *
     * @return A hardcoded string simulating a new public key.
     */
    override suspend fun generateNewKeyPair(): String {
        return "ABCDEFG1234567890ThisIsANewlyGeneratedFakePublicKeyForPreviewsOnly"
    }
}
