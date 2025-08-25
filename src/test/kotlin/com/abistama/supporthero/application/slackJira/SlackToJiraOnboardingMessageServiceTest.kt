package com.abistama.supporthero.application.slackJira

import arrow.core.Either
import com.abistama.supporthero.application.slack.SlackClient
import com.abistama.supporthero.application.slackJira.events.JiraOnboardingStartedEvent
import com.abistama.supporthero.domain.slack.SlackPostBlocksMessage
import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.SlackUserId
import com.abistama.supporthero.domain.slack.events.SlackTs
import com.abistama.supporthero.domain.slackToJira.JiraOnboardingInProgress
import com.abistama.supporthero.infrastructure.jira.oauth.JiraOAuthProperties
import com.slack.api.methods.response.chat.ChatPostMessageResponse
import com.slack.api.methods.response.oauth.OAuthV2AccessResponse
import com.slack.api.model.block.Blocks
import com.slack.api.model.block.composition.BlockCompositions
import com.slack.api.model.block.element.BlockElements
import com.slack.api.model.kotlin_extension.block.withBlocks
import io.kotest.core.spec.style.FunSpec
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.redis.core.RedisTemplate
import java.util.UUID

class SlackToJiraOnboardingMessageServiceTest :
    FunSpec({

        test("show 'connect to Jira' message if not already connected") {

            val expectedAuthorizationUri =
                @Suppress("ktlint:standard:max-line-length")
                "https://authorize.atlassian.com/oauth/authorize?client_id=clientId&redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Fjira%2Foauth&state=T024BE7LD00&response_type=code"

            val authorizationUri =
                "https://authorize.atlassian.com/oauth/authorize?client_id=#{clientId}&redirect_uri=#{redirectUri}&state=#{state}&response_type=code"
            val jiraOAuthProperties =
                JiraOAuthProperties(
                    "clientId",
                    "clientSecret",
                    "http://localhost:8080/jira/oauth",
                    authorizationUri,
                )

            val slackAutoRefreshTokenClient = mockk<SlackClient>()
            val chatPostMessageResponse =
                mockk<ChatPostMessageResponse> {
                    every { ts } returns "1234567890.123456"
                }
            every { slackAutoRefreshTokenClient.postBlocks(any()) } returns
                Either.Right(
                    chatPostMessageResponse,
                )

            val redisTemplate =
                mockk<RedisTemplate<String, Any>> {
                    every { opsForValue().set(any(), any()) } just Runs
                }
            val onboardingMessageService =
                SlackToJiraOnboardingMessageService(
                    jiraOAuthProperties,
                    slackAutoRefreshTokenClient,
                    redisTemplate,
                )

            // When
            onboardingMessageService.firstOnboarding(
                JiraOnboardingStartedEvent("source", SlackUserId("U0G9QF9C600"), SlackTeamId("T024BE7LD00")),
            )

            val expectedBlock =
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
                                        .url(
                                            expectedAuthorizationUri,
                                        )
                                },
                            ),
                        )
                    },
                )

            val expectedSlackPostBlocksMessage =
                SlackPostBlocksMessage(
                    SlackUserId("U0G9QF9C600"),
                    expectedBlock,
                    SlackTeamId("T024BE7LD00"),
                )

            verify(exactly = 1) {
                slackAutoRefreshTokenClient.postBlocks(
                    expectedSlackPostBlocksMessage,
                )
                redisTemplate.opsForValue().set(
                    "onboarding:T024BE7LD00",
                    JiraOnboardingInProgress(
                        SlackUserId("U0G9QF9C600"),
                        SlackTeamId("T024BE7LD00"),
                        SlackTs("1234567890.123456"),
                    ),
                )
            }
        }

        test("show onboarding message if already connected to Jira") {

            val authorizationUri =
                "https://authorize.atlassian.com/oauth/authorize?client_id=#{clientId}&redirect_uri=#{redirectUri}&state=#{state}&response_type=code"
            val jiraOAuthProperties =
                JiraOAuthProperties(
                    "clientId",
                    "clientSecret",
                    "http://localhost:8080/jira/oauth",
                    authorizationUri,
                )
            val slackAutoRefreshTokenClient = mockk<SlackClient>()
            every { slackAutoRefreshTokenClient.postBlocks(any()) } returns
                Either.Right(
                    mockk(),
                )

            val team = mockk<OAuthV2AccessResponse.Team>()
            every { team.id } returns "T024BE7LD00"

            val authedUser = mockk<OAuthV2AccessResponse.AuthedUser>()
            every { authedUser.id } returns "U0G9QF9C600"

            val response = mockk<OAuthV2AccessResponse>()
            every { response.team } returns team
            every { response.enterprise } returns null
            every { response.accessToken } returns "accessToken"
            every { response.tokenType } returns "bot"
            every { response.scope } returns "scope"
            every { response.expiresIn } returns 86400
            every { response.refreshToken } returns "refreshToken"
            every { response.authedUser } returns authedUser

            val slackToJiraId = UUID.randomUUID()

            val redisTemplate =
                mockk<RedisTemplate<String, Any>> {
                    every { opsForValue().set(any(), any()) } just Runs
                }
            val onboardingMessageService =
                SlackToJiraOnboardingMessageService(
                    jiraOAuthProperties,
                    slackAutoRefreshTokenClient,
                    redisTemplate,
                )

            // When
            onboardingMessageService.alreadyConnected(
                slackToJiraId,
                SlackUserId("U0G9QF9C600"),
                SlackTeamId("T024BE7LD00"),
            )

            val expectedBlock =
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
                }

            val expectedSlackPostBlocksMessage =
                SlackPostBlocksMessage(
                    SlackUserId("U0G9QF9C600"),
                    expectedBlock,
                    SlackTeamId("T024BE7LD00"),
                )

            verify(exactly = 1) {
                slackAutoRefreshTokenClient.postBlocks(
                    expectedSlackPostBlocksMessage,
                )
            }
        }
    })
