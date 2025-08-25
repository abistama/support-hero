package com.abistama.supporthero.infrastructure.slackToJira

import com.abistama.supporthero.application.jira.JiraExchangeOAuthToken
import com.abistama.supporthero.application.slack.SlackClient
import com.abistama.supporthero.application.slackJira.GetPendingTicketsUseCase
import com.abistama.supporthero.application.slackJira.LinkSlackToJira
import com.abistama.supporthero.application.slackJira.SendCsatReminderUseCase
import com.abistama.supporthero.application.slackJira.SlackToJiraOnboardingMessageService
import com.abistama.supporthero.application.slackJira.events.JiraIssueStatusUpdatedEventListener
import com.abistama.supporthero.application.slackJira.events.JiraOnboardingStartedEventListener
import com.abistama.supporthero.infrastructure.jira.JiraAutoRefreshTokenClient
import com.abistama.supporthero.infrastructure.jira.oauth.JiraOAuthProperties
import com.abistama.supporthero.infrastructure.jira.repository.JiraCsatRepository
import com.abistama.supporthero.infrastructure.jira.repository.JiraTicketTrackingRepository
import com.abistama.supporthero.infrastructure.repository.JiraTenantRepository
import com.abistama.supporthero.infrastructure.repository.SlackToJiraRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.RedisTemplate

@Configuration
class SlackToJiraBeansConfiguration {
    @Bean
    fun slackToJiraOnboardingMessageService(
        jiraOAuthProperties: JiraOAuthProperties,
        slackAutoRefreshTokenClient: SlackClient,
        redisTemplate: RedisTemplate<String, Any>,
    ): SlackToJiraOnboardingMessageService =
        SlackToJiraOnboardingMessageService(jiraOAuthProperties, slackAutoRefreshTokenClient, redisTemplate)

    @Bean
    fun jiraOnboardingStartedEventListener(
        slackToJiraRepository: SlackToJiraRepository,
        slackToJiraOnboardingMessageService: SlackToJiraOnboardingMessageService,
    ): JiraOnboardingStartedEventListener =
        JiraOnboardingStartedEventListener(
            slackToJiraRepository,
            slackToJiraOnboardingMessageService,
        )

    @Bean
    fun linkSlackToJira(
        jiraExchangeOAuthToken: JiraExchangeOAuthToken,
        jiraTenantRepository: JiraTenantRepository,
        slackToJiraRepository: SlackToJiraRepository,
        slackToJiraOnboardingMessageService: SlackToJiraOnboardingMessageService,
        slackAutoRefreshTokenClient: SlackClient,
        redisTemplate: RedisTemplate<String, Any>,
    ): LinkSlackToJira =
        LinkSlackToJira(
            jiraExchangeOAuthToken,
            jiraTenantRepository,
            slackToJiraRepository,
            slackToJiraOnboardingMessageService,
            slackAutoRefreshTokenClient,
            redisTemplate,
        )

    @Bean
    fun getPendingTicketsUseCase(
        jiraTicketTrackingRepository: JiraTicketTrackingRepository,
        jiraAutoRefreshTokenClient: JiraAutoRefreshTokenClient,
        applicationEventPublisher: ApplicationEventPublisher,
    ): GetPendingTicketsUseCase =
        GetPendingTicketsUseCase(jiraTicketTrackingRepository, jiraAutoRefreshTokenClient, applicationEventPublisher)

    @Bean
    fun sendCsatReminderUseCase(
        jiraCsatRepository: JiraCsatRepository,
        slackAutoRefreshTokenClient: SlackClient,
    ): SendCsatReminderUseCase = SendCsatReminderUseCase(jiraCsatRepository, slackAutoRefreshTokenClient)

    @Bean
    fun getPendingTicketsScheduler(getPendingTicketsUseCase: GetPendingTicketsUseCase): GetPendingTicketsScheduler =
        GetPendingTicketsScheduler(getPendingTicketsUseCase)

    @Bean
    fun sendCsatReminderScheduler(sendCsatReminderUseCase: SendCsatReminderUseCase): SendCsatReminderScheduler =
        SendCsatReminderScheduler(sendCsatReminderUseCase)

    @Bean
    fun jiraIssueStatusUpdatedEventListener(jiraCsatRepository: JiraCsatRepository) =
        JiraIssueStatusUpdatedEventListener(jiraCsatRepository)
}
