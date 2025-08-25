package com.abistama.supporthero.infrastructure.configuration

import com.abistama.supporthero.infrastructure.security.TenantEncryptionService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SecurityConfiguration {
    @Bean
    fun tenantEncryptionService(
        @Value("\${app.security.encryption.master-key}") masterKey: String,
    ): TenantEncryptionService = TenantEncryptionService(masterKey)
}
