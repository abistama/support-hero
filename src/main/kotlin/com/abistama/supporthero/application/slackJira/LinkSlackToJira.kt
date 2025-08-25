package com.abistama.supporthero.application.slackJira

import com.abistama.supporthero.application.jira.JiraExchangeOAuthToken
import com.abistama.supporthero.application.slack.SlackClient
import com.abistama.supporthero.domain.slack.SlackChannelId
import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.SlackUpdateMessage
import com.abistama.supporthero.domain.slackToJira.JiraOnboardingInProgress
import com.abistama.supporthero.infrastructure.repository.JiraTenantRepository
import com.abistama.supporthero.infrastructure.repository.SlackToJiraRepository
import mu.KLogging
import org.springframework.data.redis.core.RedisTemplate

class LinkSlackToJira(
    private val jiraExchangeOAuthToken: JiraExchangeOAuthToken,
    private val jiraTenantRepository: JiraTenantRepository,
    private val slackToJiraRepository: SlackToJiraRepository,
    private val slackToJiraOnboardingMessageService: SlackToJiraOnboardingMessageService,
    private val autoRefreshTokenClient: SlackClient,
    private val redisTemplate: RedisTemplate<String, Any>,
) {
    companion object : KLogging()

    fun link(
        code: String,
        state: String,
    ): UseCaseResult =
        jiraExchangeOAuthToken.getCredentials(code).fold(
            {
                logger.error("Error exchanging Jira credentials: $it")
                UseCaseResult.RESULT_ERROR
            },
            {
                logger.info { "Got credentials for ${it.jiraCloudId}" }
                val teamId = SlackTeamId(state)
                jiraTenantRepository.add(it)
                slackToJiraRepository.add(teamId, it.jiraCloudId)?.let { slackToJiraId ->
                    val onboardingInProgress =
                        redisTemplate.opsForValue().getAndDelete(
                            "onboarding:${teamId.id}",
                        ) as? JiraOnboardingInProgress
                    val onboardingChannel = redisTemplate.opsForValue().getAndDelete("onboarding-dm:${teamId.id}") as? SlackChannelId
                    onboardingInProgress?.let {
                        slackToJiraOnboardingMessageService.alreadyConnected(
                            slackToJiraId,
                            onboardingInProgress.startedBy,
                            onboardingInProgress.teamId,
                        )
                        onboardingChannel?.let {
                            autoRefreshTokenClient.updateMessage(
                                SlackUpdateMessage(
                                    onboardingChannel,
                                    onboardingInProgress.messageToUpdateTs,
                                    "You've successfully connected to Jira Cloud :white_check_mark:", // TODO: i18n
                                    onboardingInProgress.teamId,
                                ),
                            )
                        }
                    } ?: logger.error { "No onboarding in progress for team $teamId" }
                }
                UseCaseResult.RESULT_SUCCESS
            },
        )
}
