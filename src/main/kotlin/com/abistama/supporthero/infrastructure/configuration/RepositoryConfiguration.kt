package com.abistama.supporthero.infrastructure.configuration

import com.abistama.supporthero.infrastructure.jira.repository.JiraCsatRepository
import com.abistama.supporthero.infrastructure.jira.repository.JiraTicketTrackingRepository
import com.abistama.supporthero.infrastructure.repository.JiraTenantRepository
import com.abistama.supporthero.infrastructure.repository.SlackTenantRepository
import com.abistama.supporthero.infrastructure.repository.SlackToJiraConfigurationRepository
import com.abistama.supporthero.infrastructure.repository.SlackToJiraRepository
import com.abistama.supporthero.infrastructure.security.TenantEncryptionService
import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.DSLContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class RepositoryConfiguration {
    @Bean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun slackTenantRepository(
        dslContext: DSLContext,
        clock: Clock,
        encryptionService: TenantEncryptionService,
    ): SlackTenantRepository = SlackTenantRepository(dslContext, clock, encryptionService)

    @Bean
    fun jiraTenantRepository(
        dslContext: DSLContext,
        clock: Clock,
        encryptionService: TenantEncryptionService,
    ): JiraTenantRepository = JiraTenantRepository(dslContext, clock, encryptionService)

    @Bean
    fun slackToJiraRepository(
        dslContext: DSLContext,
        clock: Clock,
    ): SlackToJiraRepository = SlackToJiraRepository(dslContext, clock)

    @Bean
    fun slackToJiraConfigurationRepository(
        dslContext: DSLContext,
        objectMapper: ObjectMapper,
        clock: Clock,
    ): SlackToJiraConfigurationRepository = SlackToJiraConfigurationRepository(dslContext, objectMapper, clock)

    @Bean
    fun jiraTicketTrackingRepository(
        dslContext: DSLContext,
        objectMapper: ObjectMapper,
        clock: Clock,
    ): JiraTicketTrackingRepository = JiraTicketTrackingRepository(dslContext, objectMapper, clock)

    @Bean
    fun jiraCsatRepository(
        dslContext: DSLContext,
        objectMapper: ObjectMapper,
        clock: Clock,
    ): JiraCsatRepository = JiraCsatRepository(dslContext, clock)
}
