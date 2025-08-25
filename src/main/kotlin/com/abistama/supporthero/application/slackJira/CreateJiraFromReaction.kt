package com.abistama.supporthero.application.slackJira

import arrow.core.fold
import com.abistama.supporthero.application.slack.SlackClient
import com.abistama.supporthero.application.slack.UserGroupService
import com.abistama.supporthero.application.summarizer.Summarizer
import com.abistama.supporthero.domain.slack.JiraBoardConfiguration
import com.abistama.supporthero.domain.slack.SlackChannelId
import com.abistama.supporthero.domain.slack.SlackGetConversations
import com.abistama.supporthero.domain.slack.SlackMessage
import com.abistama.supporthero.domain.slack.SlackPostMessage
import com.abistama.supporthero.domain.slack.SlackPostTo
import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.events.ReactionAddedEvent
import com.abistama.supporthero.domain.slack.events.SlackTs
import com.abistama.supporthero.infrastructure.jira.Issue
import com.abistama.supporthero.infrastructure.jira.IssueFields
import com.abistama.supporthero.infrastructure.jira.IssueResponse
import com.abistama.supporthero.infrastructure.jira.JiraAutoRefreshTokenClient
import com.abistama.supporthero.infrastructure.jira.events.JiraIssueCreatedEvent
import com.abistama.supporthero.infrastructure.repository.SlackToJiraConfigurationRepository
import mu.KLogging
import org.springframework.context.ApplicationEventPublisher

class CreateJiraFromReaction(
    private val slackToJiraConfigurationRepository: SlackToJiraConfigurationRepository,
    private val userGroupService: UserGroupService,
    private val slackClient: SlackClient,
    private val jiraAutoRefreshTokenClient: JiraAutoRefreshTokenClient,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val viewedEvents: MutableSet<SlackTs>,
    private val ticketCreatedMessage: MutableSet<SlackTs>,
    private val aiSummarizer: Summarizer,
) {
    companion object : KLogging()

    fun handle(
        slackTeamId: SlackTeamId,
        reactionAddedEvent: ReactionAddedEvent,
    ) {
        logger.debug { "Received reaction added event: ${reactionAddedEvent.reaction}" }

        if (!viewedEvents.add(reactionAddedEvent.eventTs)) {
            logger.debug { "We've already processed this event. Discarding..." }
            return
        }

        val slackMessage =
            getMessage(slackTeamId, reactionAddedEvent.item.channel, reactionAddedEvent.item.ts).firstOrNull() ?: return

        logger.debug { "Parent ts? ${slackMessage.parentTs} | Message ts: ${slackMessage.ts}" }

        if (ticketCreatedMessage.contains(slackMessage.parentTs ?: slackMessage.ts)) {
            logger.debug { "The current message or thread already has a ticket created..." }
            return
        }

        slackToJiraConfigurationRepository
            .getSlackToJiraConfig(
                reactionAddedEvent.user,
                userGroupService.getUserGroups(reactionAddedEvent.user),
                reactionAddedEvent.reaction,
                reactionAddedEvent.item.channel,
            )?.let { config ->
                val jiraCloudId = config.first
                val jiraBoardConfiguration = config.second

                val description = slackMessage.text
                val summary = generateSummary(slackMessage.text, jiraBoardConfiguration)

                jiraAutoRefreshTokenClient
                    .createIssue(
                        jiraCloudId,
                        Issue(
                            IssueFields(
                                jiraBoardConfiguration.project,
                                summary,
                                description,
                                jiraBoardConfiguration.issueType,
                            ),
                        ),
                    ).fold(
                        { error ->
                            logger.error { "Error creating Jira issue: $error" }
                            return
                        },
                        { issue ->
                            logger.debug { "Jira issue created: ${issue.key}" }
                            applicationEventPublisher.publishEvent(
                                JiraIssueCreatedEvent(
                                    this,
                                    jiraCloudId,
                                    issue,
                                    jiraBoardConfiguration.sendCsat,
                                    reactionAddedEvent.itemUser,
                                ),
                            )
                            slackClient.postMessage(
                                SlackPostMessage(
                                    SlackPostTo(reactionAddedEvent.item.channel, reactionAddedEvent.item.ts),
                                    substituteVariables(
                                        jiraBoardConfiguration.feedbackMessage,
                                        issue,
                                        "<@${reactionAddedEvent.itemUser.id}>",
                                    ),
                                    slackTeamId,
                                ),
                            )
                            val ts = slackMessage.parentTs ?: slackMessage.ts
                            ticketCreatedMessage.add(ts)
                        },
                    )
            } ?: run {
            logger.debug { "No configuration found for reaction: ${reactionAddedEvent.reaction}" }
        }
    }

    private fun generateSummary(
        messageText: String,
        configuration: JiraBoardConfiguration,
    ): String =
        if (configuration.useAiSummarizer) {
            generateAiSummary(messageText)
        } else {
            generatePlainTextSummary(messageText)
        }

    private fun generateAiSummary(messageText: String): String =
        try {
            logger.debug { "Using AI summarizer to generate title for message" }
            val aiGeneratedTitle = aiSummarizer.summarizeTitle(messageText)
            aiGeneratedTitle.take(255) // Ensure it doesn't exceed Jira's limit
        } catch (e: Exception) {
            logger.error(e) { "Error generating AI summary, falling back to text truncation" }
            generatePlainTextSummary(messageText)
        }

    private fun generatePlainTextSummary(messageText: String): String = messageText.take(255).replace("\n", " ")

    private fun substituteVariables(
        message: String,
        issue: IssueResponse,
        reporter: String,
    ): String =
        mapOf("\$issue" to issue.key, "\$reporter" to reporter).fold(message) { acc, (key, value) ->
            acc.replace(key, value)
        }

    private fun getMessage(
        slackTeamId: SlackTeamId,
        slackChannelId: SlackChannelId,
        ts: SlackTs,
    ): List<SlackMessage> =
        slackClient.getConversations(SlackGetConversations(ts, slackChannelId, slackTeamId)).fold(
            { error ->
                logger.error { "Error getting message content: $error" }
                emptyList()
            },
            { response -> response.messages },
        )
}
