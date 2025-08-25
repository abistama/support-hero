package com.abistama.supporthero.infrastructure.slack

import com.abistama.supporthero.application.slack.DirectSlackClient
import com.abistama.supporthero.application.slack.EmailService
import com.abistama.supporthero.application.slack.SlackAutoRefreshTokenClient
import com.abistama.supporthero.application.slack.SlackClient
import com.abistama.supporthero.application.slack.SummarizeSlackThreadFromReaction
import com.abistama.supporthero.application.slack.UserGroupService
import com.abistama.supporthero.application.slack.events.SlackTeamProcessedEventListener
import com.abistama.supporthero.application.slackJira.CreateJiraFromReaction
import com.abistama.supporthero.application.slackJira.GetCurrentReactionsForJira
import com.abistama.supporthero.application.slackJira.ProcessAskForSupportAction
import com.abistama.supporthero.application.slackJira.ProcessConfigureReactionForJira
import com.abistama.supporthero.application.slackJira.ProcessCsatAction
import com.abistama.supporthero.application.slackJira.ProcessDeleteReactionForJira
import com.abistama.supporthero.application.slackJira.ProcessSlackBlockActions
import com.abistama.supporthero.application.slackJira.SlackToJiraOnboardingMessageService
import com.abistama.supporthero.application.summarizer.Summarizer
import com.abistama.supporthero.domain.slack.events.SlackTs
import com.abistama.supporthero.infrastructure.jira.JiraAutoRefreshTokenClient
import com.abistama.supporthero.infrastructure.jira.repository.JiraCsatRepository
import com.abistama.supporthero.infrastructure.repository.JiraTenantRepository
import com.abistama.supporthero.infrastructure.repository.SlackTenantRepository
import com.abistama.supporthero.infrastructure.repository.SlackToJiraConfigurationRepository
import com.abistama.supporthero.infrastructure.repository.SlackToJiraRepository
import com.abistama.supporthero.infrastructure.slack.events.adapter.ApplicationJsonAdapter
import com.abistama.supporthero.infrastructure.slack.events.adapter.FormUrlEncodedAdapter
import com.abistama.supporthero.infrastructure.slack.events.adapter.ShortcutHandler
import com.abistama.supporthero.infrastructure.slack.events.adapter.ViewSubmissionHandler
import com.abistama.supporthero.infrastructure.slack.oauth.SlackOAuthProperties
import com.abistama.supporthero.infrastructure.slack.oauth.adapter.AddSlackTenant
import com.fasterxml.jackson.databind.ObjectMapper
import com.slack.api.Slack
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.support.collections.RedisSet
import java.time.Clock

@Configuration
class SlackBeansConfiguration {
    @Bean
    fun slack(): Slack = Slack.getInstance()

    @Bean
    fun directSlackClient(slack: Slack): SlackClient = DirectSlackClient(slack)

    @Bean
    fun slackAutoRefreshTokenClient(
        slackTenantRepository: SlackTenantRepository,
        slackOAuthProperties: SlackOAuthProperties,
        directSlackClient: SlackClient,
        clock: Clock,
    ): SlackClient = SlackAutoRefreshTokenClient(slackTenantRepository, slackOAuthProperties, directSlackClient, clock)

    @Bean
    fun addSlackTenant(
        slackTenantRepository: SlackTenantRepository,
        clock: Clock,
        applicationEventPublisher: ApplicationEventPublisher,
    ): AddSlackTenant =
        AddSlackTenant(
            slackTenantRepository,
            clock,
            applicationEventPublisher,
        )

    @Bean
    fun userGroupService(
        slackTenantRepository: SlackTenantRepository,
        slackAutoRefreshTokenClient: SlackClient,
    ): UserGroupService = UserGroupService(slackTenantRepository, slackAutoRefreshTokenClient)

    @Bean
    fun summarizeThreadFromReaction(
        aiSummarizer: Summarizer,
        slackAutoRefreshTokenClient: SlackClient,
        redisTemplate: RedisTemplate<String, Any>,
    ) = SummarizeSlackThreadFromReaction(aiSummarizer, slackAutoRefreshTokenClient, redisTemplate)

    @Bean
    fun createJiraFromReaction(
        slackToJiraConfigurationRepository: SlackToJiraConfigurationRepository,
        userGroupService: UserGroupService,
        slackAutoRefreshTokenClient: SlackClient,
        jiraAutoRefreshTokenClient: JiraAutoRefreshTokenClient,
        applicationEventPublisher: ApplicationEventPublisher,
        viewedEvents: RedisSet<SlackTs>,
        ticketCreatedMessage: RedisSet<SlackTs>,
        aiSummarizer: Summarizer,
    ): CreateJiraFromReaction =
        CreateJiraFromReaction(
            slackToJiraConfigurationRepository,
            userGroupService,
            slackAutoRefreshTokenClient,
            jiraAutoRefreshTokenClient,
            applicationEventPublisher,
            viewedEvents,
            ticketCreatedMessage,
            aiSummarizer,
        )

    @Bean
    fun processCsatAction(
        jiraCsatRepository: JiraCsatRepository,
        slackAutoRefreshTokenClient: SlackClient,
    ): ProcessCsatAction =
        ProcessCsatAction(
            jiraCsatRepository,
            slackAutoRefreshTokenClient,
        )

    @Bean
    fun processAskForSupportAction(slackAutoRefreshTokenClient: SlackClient): ProcessAskForSupportAction =
        ProcessAskForSupportAction(
            slackAutoRefreshTokenClient,
        )

    @Bean
    fun processConfigureReactionForJira(
        jiraTenantRepository: JiraTenantRepository,
        slackAutoRefreshTokenClient: SlackClient,
        jiraAutoRefreshTokenClient: JiraAutoRefreshTokenClient,
    ): ProcessConfigureReactionForJira =
        ProcessConfigureReactionForJira(
            jiraTenantRepository,
            slackAutoRefreshTokenClient,
            jiraAutoRefreshTokenClient,
        )

    @Bean
    fun processDeleteReactionForJira(
        slackToJiraConfigurationRepository: SlackToJiraConfigurationRepository,
        slackAutoRefreshTokenClient: SlackClient,
    ): ProcessDeleteReactionForJira =
        ProcessDeleteReactionForJira(
            slackToJiraConfigurationRepository,
            slackAutoRefreshTokenClient,
        )

    @Bean
    fun getCurrentReactionsForJira(
        slackToJiraRepository: SlackToJiraRepository,
        slackToJiraConfigurationRepository: SlackToJiraConfigurationRepository,
        slackAutoRefreshTokenClient: SlackClient,
        slackToJiraOnboardingMessageService: SlackToJiraOnboardingMessageService,
    ): GetCurrentReactionsForJira =
        GetCurrentReactionsForJira(
            slackToJiraRepository,
            slackToJiraConfigurationRepository,
            slackAutoRefreshTokenClient,
            slackToJiraOnboardingMessageService,
        )

    @Bean
    fun processSlackBlockActions(
        processCsatAction: ProcessCsatAction,
        processAskForSupportAction: ProcessAskForSupportAction,
        processConfigureReactionForJira: ProcessConfigureReactionForJira,
        processDeleteReactionForJira: ProcessDeleteReactionForJira,
        applicationEventPublisher: ApplicationEventPublisher,
        redisTemplate: RedisTemplate<String, Any>,
    ): ProcessSlackBlockActions =
        ProcessSlackBlockActions(
            processCsatAction,
            processAskForSupportAction,
            processConfigureReactionForJira,
            processDeleteReactionForJira,
            applicationEventPublisher,
            redisTemplate,
        )

    @Bean
    fun viewSubmissionHandler(
        slackToJiraConfigurationRepository: SlackToJiraConfigurationRepository,
        slackAutoRefreshTokenClient: SlackClient,
        emailService: EmailService,
    ) = ViewSubmissionHandler(slackToJiraConfigurationRepository, slackAutoRefreshTokenClient, emailService)

    @Bean
    fun shortcutHandler(
        slackToJiraRepository: SlackToJiraRepository,
        processConfigureReactionForJira: ProcessConfigureReactionForJira,
        getCurrentReactionsForJira: GetCurrentReactionsForJira,
    ) = ShortcutHandler(slackToJiraRepository, processConfigureReactionForJira, getCurrentReactionsForJira)

    @Bean
    fun formUrlEncodedAdapter() = FormUrlEncodedAdapter()

    @Bean
    fun applicationJsonAdapter(
        summarizeSlackThreadFromReaction: SummarizeSlackThreadFromReaction,
        createJiraFromReaction: CreateJiraFromReaction,
        processSlackBlockActions: ProcessSlackBlockActions,
        viewSubmissionHandler: ViewSubmissionHandler,
        shortcutHandler: ShortcutHandler,
        objectMapper: ObjectMapper,
    ) = ApplicationJsonAdapter(
        summarizeSlackThreadFromReaction,
        createJiraFromReaction,
        processSlackBlockActions,
        viewSubmissionHandler,
        shortcutHandler,
        objectMapper,
    )

    @Bean
    fun slackTeamProcessedEventListener(slackTenantRepository: SlackTenantRepository) =
        SlackTeamProcessedEventListener(
            slackTenantRepository,
        )
}
