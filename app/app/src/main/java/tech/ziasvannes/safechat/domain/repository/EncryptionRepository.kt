package tech.ziasvannes.safechat.domain.repository

import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey

interface EncryptionRepository {
    suspend fun generateKeyPair(): KeyPair
    suspend fun storeKeyPair(keyPair: KeyPair)
    suspend fun getPublicKey(): PublicKey
    suspend fun getPrivateKey(): PrivateKey
    suspend fun computeSharedSecret(publicKey: PublicKey): ByteArray
    suspend fun encryptMessage(message: String, sharedSecret: ByteArray): Pair<ByteArray, ByteArray>
    suspend fun decryptMessage(encryptedContent: ByteArray, iv: ByteArray, sharedSecret: ByteArray): String
}