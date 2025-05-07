package tech.ziasvannes.safechat.domain.repository

import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey

interface EncryptionRepository {
    /**
     * Asynchronously generates a new cryptographic key pair.
     *
     * @return The generated key pair.
     */
    suspend fun generateKeyPair(): KeyPair
    /**
     * Asynchronously stores the provided cryptographic key pair for later retrieval and use.
     *
     * @param keyPair The key pair to be stored.
     */
    suspend fun storeKeyPair(keyPair: KeyPair)
    /**
     * Retrieves the stored public key for cryptographic operations.
     *
     * @return The stored public key.
     */
    suspend fun getPublicKey(): PublicKey
    /**
     * Retrieves the stored private key asynchronously.
     *
     * @return The stored private key.
     */
    suspend fun getPrivateKey(): PrivateKey
    /**
     * Computes a shared secret using the provided public key.
     *
     * @param publicKey The public key to use for shared secret computation.
     * @return The computed shared secret as a byte array.
     */
    suspend fun computeSharedSecret(publicKey: PublicKey): ByteArray
    /**
     * Encrypts a message using the provided shared secret.
     *
     * @param message The plaintext message to encrypt.
     * @param sharedSecret The shared secret used for encryption.
     * @return A triple containing the encrypted message as a byte array, the initialization vector
     * (IV), and the HMAC.
     */
    suspend fun encryptMessage(
            message: String,
            sharedSecret: ByteArray
    ): Triple<ByteArray, ByteArray, ByteArray>
    /**
     * Decrypts encrypted message data using the provided initialization vector, HMAC, and shared
     * secret.
     *
     * @param encryptedContent The encrypted message bytes to decrypt.
     * @param iv The initialization vector used during encryption.
     * @param hmac The HMAC for authentication.
     * @param sharedSecret The shared secret key for decryption.
     * @return The original plaintext message as a string.
     */
    suspend fun decryptMessage(
            encryptedContent: ByteArray,
            iv: ByteArray,
            hmac: ByteArray,
            sharedSecret: ByteArray
    ): String

    /**
     * Retrieves the current public key as a string, or null if no key is available.
     *
     * @return The string representation of the current public key, or null if no key exists.
     */
    suspend fun getCurrentPublicKey(): String?

    /**
     * Asynchronously generates a new cryptographic key pair and returns the public key as a string.
     *
     * @return The public key of the newly generated key pair, encoded as a string.
     */
    suspend fun generateNewKeyPair(): String
}
