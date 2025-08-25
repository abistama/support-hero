package com.abistama.supporthero.infrastructure.slack.events.adapter

import com.abistama.supporthero.application.slack.EmailService
import com.abistama.supporthero.application.slack.SlackClient
import com.abistama.supporthero.application.slackJira.UseCaseResult
import com.abistama.supporthero.application.slackJira.UseCaseResult.Companion.RESULT_ERROR
import com.abistama.supporthero.application.slackJira.UseCaseResult.Companion.RESULT_SUCCESS
import com.abistama.supporthero.domain.slack.JiraBoardConfiguration
import com.abistama.supporthero.domain.slack.SlackGetUserProfile
import com.abistama.supporthero.infrastructure.repository.SlackToJiraConfigurationRepository
import com.abistama.supporthero.infrastructure.slack.events.SlackViewSubmissionEvent
import com.abistama.supporthero.infrastructure.slack.events.adapter.AskForSupportViewSubmission.Companion.toAskForSupport
import com.abistama.supporthero.infrastructure.slack.events.adapter.ConfigureJiraReactionViewSubmission.Companion.toConfigureJiraReactionViewSubmission
import mu.KLogging
import org.jooq.exception.IntegrityConstraintViolationException
import java.util.*

class ViewSubmissionHandler(
    private val slackToJiraConfigurationRepository: SlackToJiraConfigurationRepository,
    private val autoRefreshTokenClient: SlackClient,
    private val emailService: EmailService,
) {
    companion object {
        private val logger = KLogging().logger
        private const val CONFIGURE_JIRA_REACTION = "configure_jira_reaction"
        private const val ASK_FOR_SUPPORT = "ask_for_support"
    }

    fun handle(event: SlackViewSubmissionEvent): UseCaseResult =
        when (event.view.externalId) {
            CONFIGURE_JIRA_REACTION -> {
                kotlin
                    .runCatching {
                        val slackToJiraTenantId = UUID.fromString(event.view.privateMetadata)
                        val owner = event.user.id
                        val submission = event.toConfigureJiraReactionViewSubmission()
                        logger.info {
                            "Received view submission from configure_jira_reaction $submission for tenant $slackToJiraTenantId"
                        }
                        val config = jiraBoardConfiguration(submission)
                        slackToJiraConfigurationRepository.add(
                            slackToJiraTenantId,
                            owner,
                            config,
                        )
                    }.fold(
                        { RESULT_SUCCESS },
                        {
                            when (it) {
                                is IntegrityConstraintViolationException -> {
                                    logger.error { "We already have the tuple (user or group id, reaction, channel id)" }
                                    RESULT_SUCCESS
                                }

                                else -> {
                                    logger.error(it) { "Error handling view submission" }
                                    RESULT_ERROR
                                }
                            }
                        },
                    )
            }
            ASK_FOR_SUPPORT -> {
                val submission = event.toAskForSupport()
                logger.info { "Received view submission from ask_for_support $submission" }
                autoRefreshTokenClient.getUserProfile(SlackGetUserProfile(submission.creator, submission.teamId)).fold(
                    {
                        logger.error { "Error getting user profile for ${submission.creator}" }
                        RESULT_ERROR
                    },
                    {
                        emailService.sendEmail("Support Request", submission.message, it.profile.email)
                        RESULT_SUCCESS
                    },
                )
            }
            else -> {
                logger.error { "Unsupported view submission externalId: ${event.view.externalId}" }
                RESULT_ERROR
            }
        }

    private fun jiraBoardConfiguration(submission: ConfigureJiraReactionViewSubmission) =
        when (submission.userOrGroupId.id.first()) {
            'U', 'W' ->
                JiraBoardConfiguration.UserConfiguration(
                    submission.project,
                    submission.reaction,
                    submission.feedbackMessage,
                    submission.issueType,
                    submission.userOrGroupId.toUserId(),
                    submission.sendCsat,
                    submission.useAiSummarizer,
                )

            'S' ->
                JiraBoardConfiguration.UserGroupConfiguration(
                    submission.project,
                    submission.reaction,
                    submission.feedbackMessage,
                    submission.issueType,
                    submission.userOrGroupId.toUserGroupId(),
                    submission.sendCsat,
                    submission.useAiSummarizer,
                )

            else -> throw IllegalArgumentException("Invalid Slack User or Slack Group format")
        }
}
