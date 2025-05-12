package tech.ziasvannes.safechat.domain.repository

import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey

interface EncryptionRepository {
    /**
 * Generates a new cryptographic key pair asynchronously.
 *
 * @return The newly generated KeyPair.
 */
    suspend fun generateKeyPair(): KeyPair
    /**
 * Stores the given cryptographic key pair for future retrieval and use.
 *
 * @param keyPair The cryptographic key pair to store.
 */
    suspend fun storeKeyPair(keyPair: KeyPair)
    /**
 * Asynchronously retrieves the stored public key used for cryptographic operations.
 *
 * @return The stored public key.
 */
    suspend fun getPublicKey(): PublicKey
    /**
 * Asynchronously retrieves the stored private key.
 *
 * @return The currently stored private key.
 */
    suspend fun getPrivateKey(): PrivateKey
    /**
 * Computes a shared secret from the given public key for secure communication.
 *
 * @param publicKey The public key used to derive the shared secret.
 * @return A byte array representing the computed shared secret.
 */
    suspend fun computeSharedSecret(publicKey: PublicKey): ByteArray
    /**
     * Encrypts a plaintext message using the provided shared secret.
     *
     * @param message The message to encrypt.
     * @param sharedSecret The shared secret key used for encryption.
     * @return A triple containing the encrypted message bytes, the initialization vector (IV), and the HMAC for authentication.
     */
    suspend fun encryptMessage(
            message: String,
            sharedSecret: ByteArray
    ): Triple<ByteArray, ByteArray, ByteArray>
    /**
     * Decrypts an encrypted message using the provided initialization vector, HMAC, and shared secret.
     *
     * Verifies the HMAC for message integrity before decrypting the content.
     *
     * @param encryptedContent The encrypted message bytes.
     * @param iv The initialization vector used during encryption.
     * @param hmac The HMAC for authentication and integrity verification.
     * @param sharedSecret The shared secret key used for decryption.
     * @return The decrypted plaintext message.
     */
    suspend fun decryptMessage(
            encryptedContent: ByteArray,
            iv: ByteArray,
            hmac: ByteArray,
            sharedSecret: ByteArray
    ): String

    /**
 * Returns the current public key as a string, or null if no key is available.
 *
 * @return The encoded public key string, or null if no key is present.
 */
    suspend fun getCurrentPublicKey(): String?

    /**
 * Generates a new cryptographic key pair and returns the public key as a string.
 *
 * The public key is encoded as a string for external use or storage.
 *
 * @return The encoded public key of the newly generated key pair.
 */
    suspend fun generateNewKeyPair(): String
}
