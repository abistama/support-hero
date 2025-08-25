package com.abistama.supporthero.infrastructure.jira.oauth

import com.abistama.supporthero.infrastructure.jira.JiraAPIClient
import com.abistama.supporthero.infrastructure.jira.JiraAutoRefreshTokenClient
import com.abistama.supporthero.infrastructure.jira.JiraErrorDecoder
import com.abistama.supporthero.infrastructure.repository.JiraTenantRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import feign.Feign
import feign.codec.Decoder
import feign.codec.Encoder
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import feign.optionals.OptionalDecoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class JiraAPIClientConfiguration {
    @Bean
    fun feignDecoder(): Decoder = OptionalDecoder(JacksonDecoder(ObjectMapper().registerModule(KotlinModule.Builder().build())))

    @Bean
    fun feignEncoder(): Encoder = JacksonEncoder(ObjectMapper().registerModule(KotlinModule.Builder().build()))

    @Bean
    fun jiraAPIClient(
        jiraOAuthProperties: JiraOAuthProperties,
        decoder: Decoder,
        encoder: Encoder,
    ): JiraAPIClient =
        Feign
            .builder()
            .encoder(encoder)
            .decoder(decoder)
            .errorDecoder(JiraErrorDecoder())
            .target(JiraAPIClient::class.java, "https://api.atlassian.com")

    @Bean
    fun jiraAutoRefreshTokenClient(
        jiraTenantRepository: JiraTenantRepository,
        jiraOAuthProperties: JiraOAuthProperties,
        jiraOAuthAPIClient: JiraOAuthAPIClient,
        jiraAPIClient: JiraAPIClient,
        clock: Clock,
    ): JiraAutoRefreshTokenClient =
        JiraAutoRefreshTokenClient(
            jiraTenantRepository,
            jiraOAuthProperties,
            jiraOAuthAPIClient,
            jiraAPIClient,
            clock,
        )
}
