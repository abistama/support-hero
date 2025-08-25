package com.abistama.supporthero.infrastructure.security

import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class TenantEncryptionService(
    private val masterKey: String,
) {
    companion object {
        private const val ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_LENGTH = 256
        private const val IV_LENGTH = 12
        private const val TAG_LENGTH = 16
        private const val ITERATIONS = 100000
        private const val SALT_PREFIX = "supporthero-tenant-encryption"
    }

    /**
     * Encrypts a token for a specific tenant using a tenant-derived encryption key
     */
    fun encryptToken(
        token: String,
        tenantId: String,
    ): String {
        val tenantKey = deriveEncryptionKey(tenantId)
        val cipher = Cipher.getInstance(TRANSFORMATION)

        // Generate random IV for each encryption
        val iv = ByteArray(IV_LENGTH)
        SecureRandom().nextBytes(iv)
        val gcmSpec = GCMParameterSpec(TAG_LENGTH * 8, iv)

        cipher.init(Cipher.ENCRYPT_MODE, tenantKey, gcmSpec)
        val encryptedData = cipher.doFinal(token.toByteArray(StandardCharsets.UTF_8))

        // Combine IV + encrypted data
        val combined = iv + encryptedData

        // Return base64 encoded with prefix to identify encryption method
        return "AES256-GCM:" + Base64.getEncoder().encodeToString(combined)
    }

    /**
     * Decrypts a token for a specific tenant using a tenant-derived encryption key
     */
    fun decryptToken(
        encryptedToken: String,
        tenantId: String,
    ): String {
        if (!encryptedToken.startsWith("AES256-GCM:")) {
            throw IllegalArgumentException("Invalid encrypted token format")
        }

        val encryptedData = Base64.getDecoder().decode(encryptedToken.substring(11))

        if (encryptedData.size < IV_LENGTH + TAG_LENGTH) {
            throw IllegalArgumentException("Invalid encrypted token length")
        }

        val tenantKey = deriveEncryptionKey(tenantId)
        val cipher = Cipher.getInstance(TRANSFORMATION)

        // Extract IV and encrypted content
        val iv = encryptedData.sliceArray(0 until IV_LENGTH)
        val cipherText = encryptedData.sliceArray(IV_LENGTH until encryptedData.size)

        val gcmSpec = GCMParameterSpec(TAG_LENGTH * 8, iv)
        cipher.init(Cipher.DECRYPT_MODE, tenantKey, gcmSpec)

        val decryptedData = cipher.doFinal(cipherText)
        return String(decryptedData, StandardCharsets.UTF_8)
    }

    /**
     * Derives a unique encryption key for each tenant using PBKDF2
     */
    private fun deriveEncryptionKey(tenantId: String): SecretKeySpec {
        val salt = (SALT_PREFIX + tenantId).toByteArray(StandardCharsets.UTF_8)
        val spec = PBEKeySpec(masterKey.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val key = factory.generateSecret(spec)
        return SecretKeySpec(key.encoded, ALGORITHM)
    }

    /**
     * Checks if a string is encrypted by our system
     */
    fun isEncrypted(value: String): Boolean = value.startsWith("AES256-GCM:")
}
