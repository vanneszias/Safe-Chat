package tech.ziasvannes.safechat.domain.repository

import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey

interface EncryptionRepository {
    /**
 * Asynchronously generates a new asymmetric key pair.
 *
 * @return The generated key pair.
 */
suspend fun generateKeyPair(): KeyPair
    /**
 * Asynchronously stores the provided asymmetric key pair for later retrieval and use.
 *
 * @param keyPair The key pair to be stored.
 */
suspend fun storeKeyPair(keyPair: KeyPair)
    /**
 * Retrieves the stored public key asynchronously.
 *
 * @return The current public key.
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
 * @param sharedSecret The shared secret key used for encryption.
 * @return A pair containing the encrypted message as a byte array and the initialization vector used for encryption.
 */
suspend fun encryptMessage(message: String, sharedSecret: ByteArray): Pair<ByteArray, ByteArray>
    /**
 * Decrypts the given encrypted content using the provided initialization vector and shared secret.
 *
 * @param encryptedContent The encrypted message data.
 * @param iv The initialization vector used during encryption.
 * @param sharedSecret The shared secret key for decryption.
 * @return The decrypted message as a string.
 */
suspend fun decryptMessage(encryptedContent: ByteArray, iv: ByteArray, sharedSecret: ByteArray): String
}