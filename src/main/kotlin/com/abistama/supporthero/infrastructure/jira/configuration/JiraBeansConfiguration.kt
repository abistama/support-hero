package com.abistama.supporthero.infrastructure.jira.configuration

import com.abistama.supporthero.application.jira.JiraExchangeOAuthToken
import com.abistama.supporthero.infrastructure.jira.JiraAPIClient
import com.abistama.supporthero.infrastructure.jira.JiraAutoRefreshTokenClient
import com.abistama.supporthero.infrastructure.jira.events.JiraIssueCreatedEventListener
import com.abistama.supporthero.infrastructure.jira.oauth.JiraOAuthAPIClient
import com.abistama.supporthero.infrastructure.jira.oauth.JiraOAuthProperties
import com.abistama.supporthero.infrastructure.jira.repository.JiraTicketTrackingRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class JiraBeansConfiguration {
    @Bean
    fun jiraExchangeOAuthToken(
        jiraOAuthProperties: JiraOAuthProperties,
        jiraOAuthAPIClient: JiraOAuthAPIClient,
        jiraAPIClient: JiraAPIClient,
        clock: Clock,
    ): JiraExchangeOAuthToken = JiraExchangeOAuthToken(jiraOAuthProperties, jiraOAuthAPIClient, jiraAPIClient, clock)

    @Bean
    fun jiraIssueCreatedEventListener(
        jiraAutoRefreshTokenClient: JiraAutoRefreshTokenClient,
        jiraTicketTrackingRepository: JiraTicketTrackingRepository,
    ): JiraIssueCreatedEventListener = JiraIssueCreatedEventListener(jiraAutoRefreshTokenClient, jiraTicketTrackingRepository)
}
