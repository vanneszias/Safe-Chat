package tech.ziasvannes.safechat.data.repository

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.google.crypto.tink.subtle.AesGcmJce
import tech.ziasvannes.safechat.domain.repository.EncryptionRepository
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement
import javax.crypto.interfaces.DHPublicKey
import javax.crypto.spec.DHParameterSpec
import javax.inject.Inject

class EncryptionRepositoryImpl @Inject constructor() : EncryptionRepository {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val KEY_ALIAS = "SafeChatKeyPair"

    /**
     * Retrieves Diffie-Hellman (DH) parameters for key generation.
     *
     * Attempts to generate secure 2048-bit DH parameters dynamically. If generation fails,
     * falls back to using the standardized 2048-bit MODP Group (RFC 3526 Group 14).
     *
     * @return A DHParameterSpec containing the prime modulus and generator for DH key exchange.
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
                BigInteger("FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3DC2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F83655D23DCA3AD961C62F356208552BB9ED529077096966D670C354E4ABC9804F1746C08CA18217C32905E462E36CE3BE39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9DE2BCBF6955817183995497CEA956AE515D2261898FA051015728E5A8AAAC42DAD33170D04507A33A85521ABDF1CBA64ECFB850458DBEF0A8AEA71575D060C7DB3970F85A6E1E4C7ABF5AE8CDB0933D71E8C94E04A25619DCEE3D2261AD2EE6BF12FFA06D98A0864D87602733EC86A64521F2B18177B200CBBE117577A615D6C770988C0BAD946E208E24FA074E5AB3143DB5BFCE0FD108E4B82D120A93AD2CAFFFFFFFFFFFFFFFF", 16),
                BigInteger("2")
            )
        }
    }

    /**
     * Generates a Diffie-Hellman key pair and stores it securely in the AndroidKeyStore.
     *
     * @return The generated DH key pair.
     */
    override suspend fun generateKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            "DH",
            "AndroidKeyStore"
        )

        val parameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).apply {
            setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            setAlgorithmParameterSpec(getDHParameters())
            setKeySize(2048)
        }.build()

        keyPairGenerator.initialize(parameterSpec)
        return keyPairGenerator.generateKeyPair()
    }

    /**
     * No-op for storing key pairs, as keys generated in AndroidKeyStore are automatically persisted.
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
     * Retrieves the Diffie-Hellman private key stored in the AndroidKeyStore.
     *
     * @return The stored private key associated with the predefined key alias.
     */
    override suspend fun getPrivateKey(): PrivateKey {
        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
        return entry.privateKey
    }

    /**
     * Computes a shared secret using the Diffie-Hellman key agreement protocol with the stored private key and a provided public key.
     *
     * @param publicKey The public key from the other party in the key exchange.
     * @return The computed shared secret as a byte array.
     */
    override suspend fun computeSharedSecret(publicKey: PublicKey): ByteArray {
        val privateKey = getPrivateKey()
        val keyAgreement = KeyAgreement.getInstance("DH")
        keyAgreement.init(privateKey)
        keyAgreement.doPhase(publicKey, true)
        return keyAgreement.generateSecret()
    }

    /**
     * Encrypts a plaintext message using AES-GCM with the provided shared secret.
     *
     * Generates a random 12-byte initialization vector (IV), encrypts the message using AES-GCM with the shared secret as the key, and returns a pair containing the encrypted bytes and the IV.
     *
     * @param message The plaintext message to encrypt.
     * @param sharedSecret The shared secret key used for AES-GCM encryption.
     * @return A pair consisting of the encrypted message bytes and the IV used for encryption.
     */
    override suspend fun encryptMessage(
        message: String,
        sharedSecret: ByteArray
    ): Pair<ByteArray, ByteArray> {
        val aesGcm = AesGcmJce(sharedSecret)
        val iv = ByteArray(12).apply {
            java.security.SecureRandom().nextBytes(this)
        }
        val encryptedBytes = aesGcm.encrypt(message.toByteArray(), iv)
        return Pair(encryptedBytes, iv)
    }

    /**
     * Decrypts an AES-GCM encrypted message using the provided shared secret and initialization vector.
     *
     * @param encryptedContent The ciphertext to decrypt.
     * @param iv The initialization vector used during encryption.
     * @param sharedSecret The shared secret key for decryption.
     * @return The decrypted plaintext message as a string.
     */
    override suspend fun decryptMessage(
        encryptedContent: ByteArray,
        iv: ByteArray,
        sharedSecret: ByteArray
    ): String {
        val aesGcm = AesGcmJce(sharedSecret)
        val decryptedBytes = aesGcm.decrypt(encryptedContent, iv)
        return String(decryptedBytes)
    }
}