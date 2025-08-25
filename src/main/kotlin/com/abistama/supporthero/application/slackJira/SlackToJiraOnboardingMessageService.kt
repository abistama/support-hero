package com.abistama.supporthero.application.slackJira

import com.abistama.supporthero.application.slack.SlackClient
import com.abistama.supporthero.application.slackJira.events.JiraOnboardingStartedEvent
import com.abistama.supporthero.domain.slack.SlackPostBlocksMessage
import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.SlackUserId
import com.abistama.supporthero.domain.slack.events.SlackTs
import com.abistama.supporthero.domain.slackToJira.JiraOnboardingInProgress
import com.abistama.supporthero.infrastructure.jira.oauth.JiraOAuthProperties
import com.abistama.supporthero.infrastructure.slack.oauth.adapter.URLReplacerWithEncoding
import com.slack.api.model.block.Blocks
import com.slack.api.model.block.composition.BlockCompositions
import com.slack.api.model.block.element.BlockElements
import com.slack.api.model.kotlin_extension.block.withBlocks
import mu.KLogging
import org.springframework.data.redis.core.RedisTemplate
import java.util.UUID

class SlackToJiraOnboardingMessageService(
    private val jiraOAuthProperties: JiraOAuthProperties,
    private val slackAutoRefreshTokenClient: SlackClient,
    private val redisTemplate: RedisTemplate<String, Any>,
) {
    companion object : KLogging()

    fun alreadyConnected(
        slackToJiraId: UUID,
        userId: SlackUserId,
        teamId: SlackTeamId,
    ) {
        val postBlocksMessage =
            SlackPostBlocksMessage(
                userId,
                withBlocks {
                    header {
                        text("Welcome to SupportHero!", emoji = true)
                    }
                    section {
                        markdownText("We're excited to have you on board. 🎉")
                    }
                    section {
                        markdownText("Your Jira Cloud instance is already connected! Here's what you can do next:")
                    }
                    divider()
                    section {
                        markdownText(
                            "*1. Configure your interactions from Slack to a Jira project*\nSet up an emoji (reaction) in Slack that will automatically create a Jira ticket in your desired Jira project.",
                        )
                        accessory {
                            button {
                                text("Configure Reaction", emoji = true)
                                value("$slackToJiraId")
                                actionId("configure_jira_reaction||")
                            }
                        }
                    }
                    section {
                        markdownText(
                            "*2. Ask for Support*\nIf you have any questions or need assistance, feel free to reach out to our support team. We're here to help you get the most out of our integration. 😊",
                        )
                        accessory {
                            button {
                                text("Ask for Support", emoji = true)
                                value("$slackToJiraId")
                                actionId("ask_for_support||")
                            }
                        }
                    }
                },
                teamId,
            )
        slackAutoRefreshTokenClient
            .postBlocks(postBlocksMessage)
            .fold(
                { error ->
                    logger.error { "Error posting message: $error" }
                },
                { response ->
                    logger.info { "Message posted: ${response.ts}" }
                },
            )
    }

    fun firstOnboarding(event: JiraOnboardingStartedEvent) {
        val postBlocksMessage =
            SlackPostBlocksMessage(
                event.startedBy,
                listOf(
                    Blocks.section { section ->
                        section.text(BlockCompositions.plainText("Welcome! Let's get started with the onboarding process."))
                    },
                    Blocks.actions { actions ->
                        actions.elements(
                            listOf(
                                BlockElements.button { button ->
                                    button
                                        .actionId("connect_jira_cloud")
                                        .text(BlockCompositions.plainText("Connect your JIRA Cloud"))
                                        .url(jiraUrlReplacer(event.teamId))
                                },
                            ),
                        )
                    },
                ),
                event.teamId,
            )
        slackAutoRefreshTokenClient
            .postBlocks(postBlocksMessage)
            .fold(
                { error ->
                    logger.error { "Error posting message: $error" }
                },
                { response ->
                    logger.debug { "Message posted: ${response.ts}" }
                    redisTemplate.opsForValue().set(
                        "onboarding:${event.teamId.id}",
                        JiraOnboardingInProgress(
                            event.startedBy,
                            event.teamId,
                            SlackTs(response.ts),
                        ),
                    )
                },
            )
    }

    private fun jiraUrlReplacer(teamId: SlackTeamId) =
        URLReplacerWithEncoding.replace(
            jiraOAuthProperties.authorizationUri,
            mapOf(
                "#{state}" to teamId.id,
                "#{clientId}" to jiraOAuthProperties.clientId,
                "#{redirectUri}" to jiraOAuthProperties.redirectUri,
            ),
        )
}
