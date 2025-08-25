package com.abistama.supporthero.infrastructure.security

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith

class TenantEncryptionServiceTest : FunSpec({
    
    val masterKey = "test-master-key-for-encryption-123456789"
    val encryptionService = TenantEncryptionService(masterKey)
    
    test("should encrypt and decrypt tokens successfully") {
        // Given
        val token = "xoxb-slack-token-example"
        val tenantId = "T12345"
        
        // When
        val encryptedToken = encryptionService.encryptToken(token, tenantId)
        val decryptedToken = encryptionService.decryptToken(encryptedToken, tenantId)
        
        // Then
        decryptedToken shouldBe token
        encryptedToken shouldStartWith "AES256-GCM:"
        encryptedToken shouldNotBe token
    }
    
    test("should produce different encrypted values for same token with different tenants") {
        // Given
        val token = "xoxb-same-token"
        val tenantId1 = "T12345"
        val tenantId2 = "T67890"
        
        // When
        val encrypted1 = encryptionService.encryptToken(token, tenantId1)
        val encrypted2 = encryptionService.encryptToken(token, tenantId2)
        
        // Then
        encrypted1 shouldNotBe encrypted2
        encryptionService.decryptToken(encrypted1, tenantId1) shouldBe token
        encryptionService.decryptToken(encrypted2, tenantId2) shouldBe token
    }
    
    test("should produce different encrypted values for same token and tenant due to IV") {
        // Given
        val token = "xoxb-token"
        val tenantId = "T12345"
        
        // When
        val encrypted1 = encryptionService.encryptToken(token, tenantId)
        val encrypted2 = encryptionService.encryptToken(token, tenantId)
        
        // Then
        encrypted1 shouldNotBe encrypted2 // Different IVs should produce different ciphertexts
        encryptionService.decryptToken(encrypted1, tenantId) shouldBe token
        encryptionService.decryptToken(encrypted2, tenantId) shouldBe token
    }
    
    test("should fail to decrypt token with wrong tenant ID") {
        // Given
        val token = "xoxb-token"
        val correctTenantId = "T12345"
        val wrongTenantId = "T67890"
        val encryptedToken = encryptionService.encryptToken(token, correctTenantId)
        
        // When & Then
        shouldThrow<Exception> {
            encryptionService.decryptToken(encryptedToken, wrongTenantId)
        }
    }
    
    test("should handle long tokens") {
        // Given
        val longToken = "x".repeat(1000)
        val tenantId = "T12345"
        
        // When
        val encrypted = encryptionService.encryptToken(longToken, tenantId)
        val decrypted = encryptionService.decryptToken(encrypted, tenantId)
        
        // Then
        decrypted shouldBe longToken
    }
    
    test("should handle special characters in tokens") {
        // Given
        val specialToken = "token-with-special-chars!@#$%^&*()_+={}[]|\\:;\"'<>,.?/"
        val tenantId = "T12345"
        
        // When
        val encrypted = encryptionService.encryptToken(specialToken, tenantId)
        val decrypted = encryptionService.decryptToken(encrypted, tenantId)
        
        // Then
        decrypted shouldBe specialToken
    }
    
    test("should correctly identify encrypted values") {
        // Given
        val token = "xoxb-token"
        val tenantId = "T12345"
        
        // When
        val encrypted = encryptionService.encryptToken(token, tenantId)
        
        // Then
        encryptionService.isEncrypted(encrypted) shouldBe true
        encryptionService.isEncrypted(token) shouldBe false
        encryptionService.isEncrypted("random-string") shouldBe false
    }
    
    test("should fail on invalid encrypted token format") {
        // Given
        val invalidFormat = "invalid-format:some-data"
        val tenantId = "T12345"
        
        // When & Then
        shouldThrow<IllegalArgumentException> {
            encryptionService.decryptToken(invalidFormat, tenantId)
        }
    }
    
    test("should fail on corrupted encrypted token") {
        // Given
        val validToken = "xoxb-token"
        val tenantId = "T12345"
        val encrypted = encryptionService.encryptToken(validToken, tenantId)
        val corrupted = encrypted.dropLast(5) + "XXXXX" // Corrupt the end
        
        // When & Then
        shouldThrow<Exception> {
            encryptionService.decryptToken(corrupted, tenantId)
        }
    }
    
    test("should handle unicode characters") {
        // Given
        val unicodeToken = "token-with-unicode-🔐-characters-測試"
        val tenantId = "T12345"
        
        // When
        val encrypted = encryptionService.encryptToken(unicodeToken, tenantId)
        val decrypted = encryptionService.decryptToken(encrypted, tenantId)
        
        // Then
        decrypted shouldBe unicodeToken
    }
})