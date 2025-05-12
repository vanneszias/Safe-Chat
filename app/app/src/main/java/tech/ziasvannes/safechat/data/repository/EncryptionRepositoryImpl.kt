package tech.ziasvannes.safechat.data.repository

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.crypto.tink.subtle.AesGcmJce
import com.google.crypto.tink.subtle.Hkdf
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.interfaces.DHPublicKey
import javax.crypto.spec.DHParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import tech.ziasvannes.safechat.domain.repository.EncryptionRepository

class EncryptionRepositoryImpl @Inject constructor() : EncryptionRepository {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val KEY_ALIAS = "SafeChatKeyPair"

    /**
     * Obtains Diffie-Hellman (DH) parameters for key generation.
     *
     * Attempts to generate secure 2048-bit DH parameters dynamically; if this fails, falls back to the standardized 2048-bit MODP Group 14 parameters (RFC 3526).
     *
     * @return DHParameterSpec containing the prime modulus and generator for DH key exchange.
     */
    private fun getDHParameters(): DHParameterSpec {
        // Use NIST's standardized 2048-bit DH parameters (SP 800-56A)
        return try {
            val dhKpg = KeyPairGenerator.getInstance("DH")
            dhKpg.initialize(2048) // This will generate secure parameters
            val kp = dhKpg.generateKeyPair()
            (kp.public as DHPublicKey).params
        } catch (e: Exception) {
            // Fallback to RFC 3526 Group 14 (2048-bit MODP Group) if generation fails
            DHParameterSpec(
                    BigInteger(
                            "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3DC2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F83655D23DCA3AD961C62F356208552BB9ED529077096966D670C354E4ABC9804F1746C08CA18217C32905E462E36CE3BE39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9DE2BCBF6955817183995497CEA956AE515D2261898FA051015728E5A8AAAC42DAD33170D04507A33A85521ABDF1CBA64ECFB850458DBEF0A8AEA71575D060C7DB3970F85A6E1E4C7ABF5AE8CDB0933D71E8C94E04A25619DCEE3D2261AD2EE6BF12FFA06D98A0864D87602733EC86A64521F2B18177B200CBBE117577A615D6C770988C0BAD946E208E24FA074E5AB3143DB5BFCE0FD108E4B82D120A93AD2CAFFFFFFFFFFFFFFFF",
                            16
                    ),
                    BigInteger("2")
            )
        }
    }

    /**
     * Generates a Diffie-Hellman key pair and securely stores it in the AndroidKeyStore.
     *
     * @return The generated Diffie-Hellman key pair.
     */
    override suspend fun generateKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("DH", "AndroidKeyStore")

        val parameterSpec =
                KeyGenParameterSpec.Builder(
                                KEY_ALIAS,
                                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                        )
                        .apply {
                            setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                            setAlgorithmParameterSpec(getDHParameters())
                            setKeySize(2048)
                        }
                        .build()

        keyPairGenerator.initialize(parameterSpec)
        return keyPairGenerator.generateKeyPair()
    }

    /**
     * Does nothing, as key pairs generated in AndroidKeyStore are automatically persisted.
     */
    override suspend fun storeKeyPair(keyPair: KeyPair) {
        // Keys are automatically stored in AndroidKeyStore when generated
    }

    /**
     * Retrieves the stored Diffie-Hellman public key from the AndroidKeyStore.
     *
     * @return The public key associated with the key alias.
     */
    override suspend fun getPublicKey(): PublicKey {
        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
        return entry.certificate.publicKey
    }

    /**
     * Retrieves the Diffie-Hellman private key from the AndroidKeyStore.
     *
     * @return The private key associated with the predefined key alias, or throws if not found.
     */
    override suspend fun getPrivateKey(): PrivateKey {
        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
        return entry.privateKey
    }

    /**
     * Computes the Diffie-Hellman shared secret using the stored private key and a given public key.
     *
     * @param publicKey The public key from the other participant in the key exchange.
     * @return The resulting shared secret as a byte array.
     */
    override suspend fun computeSharedSecret(publicKey: PublicKey): ByteArray {
        val privateKey = getPrivateKey()
        val keyAgreement = KeyAgreement.getInstance("DH")
        keyAgreement.init(privateKey)
        keyAgreement.doPhase(publicKey, true)
        return keyAgreement.generateSecret()
    }

    /**
     * Derives separate 256-bit AES and HMAC keys from a shared secret using HKDF with HMAC-SHA256.
     *
     * @param sharedSecret The input byte array from which to derive keys.
     * @return A pair containing the derived AES key and HMAC key, each as a 32-byte array.
     */
    private fun deriveKeys(sharedSecret: ByteArray): Pair<ByteArray, ByteArray> {
        // Use HKDF to derive two keys: one for AES, one for HMAC
        val salt = ByteArray(32) // Could be a static or random salt; for now, all zeros
        val infoAes = "AES_KEY".toByteArray()
        val infoHmac = "HMAC_KEY".toByteArray()
        val aesKey =
                Hkdf.computeHkdf("HmacSha256", sharedSecret, salt, infoAes, 32) // 256-bit AES key
        val hmacKey =
                Hkdf.computeHkdf("HmacSha256", sharedSecret, salt, infoHmac, 32) // 256-bit HMAC key
        return Pair(aesKey, hmacKey)
    }

    /**
     * Encrypts a plaintext message using AES-GCM and authenticates it with HMAC-SHA256.
     *
     * Derives separate AES and HMAC keys from the provided shared secret. Encrypts the message using AES-GCM with a randomly generated 12-byte IV, then computes an HMAC-SHA256 over the ciphertext and IV. Returns a triple containing the encrypted message bytes, the IV, and the HMAC.
     *
     * @param message The plaintext message to encrypt.
     * @param sharedSecret The shared secret from which encryption and authentication keys are derived.
     * @return A triple containing the encrypted message bytes, the IV used for encryption, and the HMAC for authentication.
     */
    override suspend fun encryptMessage(
            message: String,
            sharedSecret: ByteArray
    ): Triple<ByteArray, ByteArray, ByteArray> {
        try {
            val (aesKey, hmacKey) = deriveKeys(sharedSecret)
            val aesGcm = AesGcmJce(aesKey)
            val iv = ByteArray(12).apply { java.security.SecureRandom().nextBytes(this) }
            val encryptedBytes = aesGcm.encrypt(message.toByteArray(), iv)
            // HMAC over (encryptedBytes || iv)
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(hmacKey, "HmacSHA256"))
            mac.update(encryptedBytes)
            mac.update(iv)
            val hmac = mac.doFinal()
            return Triple(encryptedBytes, iv, hmac)
        } catch (e: Exception) {
            throw RuntimeException("Encryption failed: ${e.message}", e)
        }
    }

    /**
     * Decrypts AES-GCM encrypted data using a shared secret, verifying integrity with HMAC.
     *
     * Verifies the provided HMAC over the encrypted content and IV using a key derived from the shared secret.
     * If verification succeeds, decrypts the content using AES-GCM and returns the plaintext as a UTF-8 string.
     * Throws a SecurityException if HMAC verification fails, or a RuntimeException if decryption encounters an error.
     *
     * @param encryptedContent The ciphertext to decrypt.
     * @param iv The initialization vector used during encryption.
     * @param hmac The HMAC for integrity verification.
     * @param sharedSecret The shared secret from which encryption and HMAC keys are derived.
     * @return The decrypted plaintext message as a UTF-8 string.
     */
    override suspend fun decryptMessage(
            encryptedContent: ByteArray,
            iv: ByteArray,
            hmac: ByteArray,
            sharedSecret: ByteArray
    ): String {
        try {
            val (aesKey, hmacKey) = deriveKeys(sharedSecret)
            // Verify HMAC
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(hmacKey, "HmacSHA256"))
            mac.update(encryptedContent)
            mac.update(iv)
            val expectedHmac = mac.doFinal()
            if (!expectedHmac.contentEquals(hmac)) {
                throw SecurityException(
                        "HMAC verification failed: message may have been tampered with."
                )
            }
            val aesGcm = AesGcmJce(aesKey)
            val decryptedBytes = aesGcm.decrypt(encryptedContent, iv)
            return String(decryptedBytes)
        } catch (e: Exception) {
            throw RuntimeException("Decryption failed: ${e.message}", e)
        }
    }

    /**
     * Returns the stored public key as a Base64-encoded string, or null if unavailable.
     *
     * Attempts to retrieve the public key from secure storage and encodes it using Base64 without line wraps.
     * Returns null if the key cannot be retrieved or an error occurs.
     *
     * @return The Base64-encoded public key string, or null if not found.
     */
    override suspend fun getCurrentPublicKey(): String? {
        return try {
            val publicKey = getPublicKey()
            Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Generates a new Diffie-Hellman key pair and returns the public key as a Base64-encoded string.
     *
     * @return The Base64-encoded public key of the newly generated key pair, without line wraps.
     */
    override suspend fun generateNewKeyPair(): String {
        val keyPair = generateKeyPair()
        val publicKey = keyPair.public
        return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    }
}
