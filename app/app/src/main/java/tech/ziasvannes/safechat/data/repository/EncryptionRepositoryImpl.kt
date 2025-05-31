package tech.ziasvannes.safechat.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement
import javax.inject.Inject
import tech.ziasvannes.safechat.domain.repository.EncryptionRepository

class EncryptionRepositoryImpl @Inject constructor(private val context: Context) :
        EncryptionRepository {
        private val PREFS_NAME = "safechat_x25519_keys"
        private val PRIVATE_KEY_PREF = "private_key"
        private val PUBLIC_KEY_PREF = "public_key"

        private val prefs: SharedPreferences by lazy {
                val masterKey =
                        MasterKey.Builder(context)
                                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                                .build()
                EncryptedSharedPreferences.create(
                        context,
                        PREFS_NAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
        }

        private suspend fun ensureKeyPairExists() {
                if (prefs.getString(PRIVATE_KEY_PREF, null) == null ||
                                prefs.getString(PUBLIC_KEY_PREF, null) == null
                ) {
                        Log.w("EncryptionRepo", "Key pair missing, generating new key pair.")
                        generateKeyPair()
                }
        }

        override suspend fun generateKeyPair(): KeyPair {
                val keyPairGenerator = KeyPairGenerator.getInstance("X25519")
                val keyPair = keyPairGenerator.generateKeyPair()
                val privBytes = keyPair.private.encoded
                val pubBytes = keyPair.public.encoded
                Log.d(
                        "EncryptionRepo",
                        "Generated Public Key (Base64): ${Base64.encodeToString(pubBytes, Base64.NO_WRAP)}"
                )
                Log.d(
                        "EncryptionRepo",
                        "Generated Private Key (Base64): ${Base64.encodeToString(privBytes, Base64.NO_WRAP)}"
                )
                // Store keys
                prefs.edit()
                        .putString(
                                PRIVATE_KEY_PREF,
                                Base64.encodeToString(privBytes, Base64.NO_WRAP)
                        )
                        .apply()
                prefs.edit()
                        .putString(PUBLIC_KEY_PREF, Base64.encodeToString(pubBytes, Base64.NO_WRAP))
                        .apply()
                return keyPair
        }

        override suspend fun storeKeyPair(keyPair: KeyPair) {
                // Not used; keys are stored on generation
        }

        override suspend fun getPublicKey(): PublicKey {
                ensureKeyPairExists()
                val pubB64 =
                        prefs.getString(PUBLIC_KEY_PREF, null)
                                ?: throw IllegalStateException("No public key found")
                Log.d("EncryptionRepo", "Loaded Public Key (Base64): $pubB64")
                val pubBytes = Base64.decode(pubB64, Base64.NO_WRAP)
                val keyFactory = KeyFactory.getInstance("X25519")
                return keyFactory.generatePublic(X509EncodedKeySpec(pubBytes))
        }

        override suspend fun getPrivateKey(): PrivateKey {
                ensureKeyPairExists()
                val privB64 =
                        prefs.getString(PRIVATE_KEY_PREF, null)
                                ?: throw IllegalStateException("No private key found")
                Log.d("EncryptionRepo", "Loaded Private Key (Base64): $privB64")
                val privBytes = Base64.decode(privB64, Base64.NO_WRAP)
                val keyFactory = KeyFactory.getInstance("X25519")
                return keyFactory.generatePrivate(PKCS8EncodedKeySpec(privBytes))
        }

        override suspend fun computeSharedSecret(publicKey: PublicKey): ByteArray {
                ensureKeyPairExists()
                val privateKey = getPrivateKey()
                val keyAgreement = KeyAgreement.getInstance("X25519")
                keyAgreement.init(privateKey)
                keyAgreement.doPhase(publicKey, true)
                val sharedSecret = keyAgreement.generateSecret()
                Log.d(
                        "EncryptionRepo",
                        "Computed Shared Secret (Base64): ${Base64.encodeToString(sharedSecret, Base64.NO_WRAP)}"
                )
                return sharedSecret
        }

        override suspend fun encryptMessage(
                message: String,
                sharedSecret: ByteArray
        ): Pair<ByteArray, ByteArray> {
                val key = javax.crypto.spec.SecretKeySpec(sharedSecret, "AES")
                val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }
                val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
                val gcmSpec = javax.crypto.spec.GCMParameterSpec(128, iv)
                cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key, gcmSpec)
                val ciphertext = cipher.doFinal(message.toByteArray(Charsets.UTF_8))
                Log.d("EncryptionRepo", "Encrypting message: $message")
                Log.d(
                        "EncryptionRepo",
                        "Ciphertext (Base64): ${Base64.encodeToString(ciphertext, Base64.NO_WRAP)}"
                )
                Log.d("EncryptionRepo", "IV (Base64): ${Base64.encodeToString(iv, Base64.NO_WRAP)}")
                return Pair(ciphertext, iv)
        }

        override suspend fun decryptMessage(
                encryptedContent: ByteArray,
                iv: ByteArray,
                sharedSecret: ByteArray
        ): String {
                val key = javax.crypto.spec.SecretKeySpec(sharedSecret, "AES")
                val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
                val gcmSpec = javax.crypto.spec.GCMParameterSpec(128, iv)
                cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key, gcmSpec)
                Log.d(
                        "EncryptionRepo",
                        "Decrypting Ciphertext (Base64): ${Base64.encodeToString(encryptedContent, Base64.NO_WRAP)}"
                )
                Log.d("EncryptionRepo", "IV (Base64): ${Base64.encodeToString(iv, Base64.NO_WRAP)}")
                val plaintext = cipher.doFinal(encryptedContent)
                val result = String(plaintext, Charsets.UTF_8)
                Log.d("EncryptionRepo", "Decrypted message: $result")
                return result
        }

        override suspend fun getCurrentPublicKey(): String? {
                ensureKeyPairExists()
                // Always export the X.509-encoded public key
                val pubKey = getPublicKey()
                val pubB64 = Base64.encodeToString(pubKey.encoded, Base64.NO_WRAP)
                Log.d("EncryptionRepo", "getCurrentPublicKey (Base64, X.509): $pubB64")
                return pubB64
        }

        override suspend fun generateNewKeyPair(): String {
                generateKeyPair()
                return getCurrentPublicKey()
                        ?: throw IllegalStateException("Failed to generate new key pair")
        }

        suspend fun publicKeyFromBase64(base64: String): PublicKey {
                Log.d("EncryptionRepo", "publicKeyFromBase64 input: $base64")
                val pubBytes = Base64.decode(base64, Base64.NO_WRAP)
                val keyFactory = KeyFactory.getInstance("X25519")
                // If the input is 32 bytes, it's a raw key, so wrap it in X.509 format
                return if (pubBytes.size == 32) {
                        Log.w(
                                "EncryptionRepo",
                                "publicKeyFromBase64: Detected raw 32-byte key, wrapping as X.509"
                        )
                        val x509Header =
                                byteArrayOf(
                                        0x30.toByte(),
                                        0x2a.toByte(),
                                        0x30.toByte(),
                                        0x05.toByte(),
                                        0x06.toByte(),
                                        0x03.toByte(),
                                        0x2b.toByte(),
                                        0x65.toByte(),
                                        0x6e.toByte(),
                                        0x03.toByte(),
                                        0x21.toByte(),
                                        0x00.toByte()
                                )
                        val x509Bytes = x509Header + pubBytes
                        keyFactory.generatePublic(X509EncodedKeySpec(x509Bytes))
                } else {
                        keyFactory.generatePublic(X509EncodedKeySpec(pubBytes))
                }
        }

        suspend fun decryptIncomingMessage(
                senderPublicKeyBase64: String,
                encryptedContent: ByteArray,
                iv: ByteArray
        ): String {
                val senderPublicKey = publicKeyFromBase64(senderPublicKeyBase64)
                val sharedSecret = computeSharedSecret(senderPublicKey)
                return decryptMessage(encryptedContent, iv, sharedSecret)
        }
}
