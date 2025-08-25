package com.abistama.supporthero.application.slackJira

import com.abistama.supporthero.application.slack.events.SlackDomain
import com.abistama.supporthero.application.slack.events.SlackTeamProcessedEvent
import com.abistama.supporthero.domain.slack.JiraBoardConfigurationIdTrigger
import com.abistama.supporthero.domain.slack.SlackChannelId
import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.SlackToJiraTenantTrigger
import com.abistama.supporthero.domain.slack.events.SlackTs
import com.abistama.supporthero.infrastructure.slack.events.adapter.SlackBlockActionsEvent
import com.abistama.supporthero.infrastructure.slack.events.adapter.Team
import io.kotest.core.spec.style.FunSpec
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.redis.core.RedisTemplate
import java.util.*

class ProcessSlackBlockActionsTest :
    FunSpec({

        val processCsatAction = mockk<ProcessCsatAction>()
        val processAskForSupportAction = mockk<ProcessAskForSupportAction>()
        val processConfigureReactionForJira = mockk<ProcessConfigureReactionForJira>()
        val processDeleteReactionForJira = mockk<ProcessDeleteReactionForJira>()
        val applicationEventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
        val redisTemplate = mockk<RedisTemplate<String, Any>>()
        val processSlackBlockActions =
            ProcessSlackBlockActions(
                processCsatAction,
                processAskForSupportAction,
                processConfigureReactionForJira,
                processDeleteReactionForJira,
                applicationEventPublisher,
                redisTemplate,
            )

        beforeTest {
            clearAllMocks()
        }

        context("handle") {
            context("CSat action") {
                val slackMessageTs = SlackTs("1360782804.083113")
                val actionTs = SlackTs("1360782804.030455")
                val event =
                    SlackBlockActionsEvent(
                        SlackBlockActionsEvent.MessageContainer(
                            slackMessageTs,
                            SlackChannelId("D1234567890"),
                            false,
                        ),
                        "triggerId",
                        Team(SlackTeamId("T1234567890"), "domain"),
                        listOf(
                            SlackBlockActionsEvent.Action(
                                actionId = "csat|123e4567-e89b-12d3-a456-426614174000|5",
                                blockId = "blockId",
                                text = SlackBlockActionsEvent.Text("plain_text", "5", true),
                                value = "5",
                                style = "primary",
                                type = "button",
                                actionTs = actionTs,
                            ),
                        ),
                    )

                test("should process CSat action") {
                    every { processCsatAction.handle(any(), any(), any()) } just Runs
                    processSlackBlockActions.handle(event)

                    verify {
                        processCsatAction.handle(
                            SlackTeamId("T1234567890"),
                            event.container as SlackBlockActionsEvent.MessageContainer,
                            CSatAction(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), 5),
                        )
                    }
                }
            }

            context("Configure reaction for Jira action") {
                val slackMessageTs = SlackTs("1360782804.083113")
                val actionTs = SlackTs("1360782804.030455")
                val event =
                    SlackBlockActionsEvent(
                        SlackBlockActionsEvent.MessageContainer(
                            slackMessageTs,
                            SlackChannelId("D1234567890"),
                            false,
                        ),
                        "triggerId",
                        Team(SlackTeamId("T1234567890"), "domain"),
                        listOf(
                            SlackBlockActionsEvent.Action(
                                actionId = "configure_jira_reaction",
                                blockId = "blockId",
                                text = SlackBlockActionsEvent.Text("plain_text", "Configure ", true),
                                value = "123e4567-e89b-12d3-a456-426614174000",
                                style = "primary",
                                type = "button",
                                actionTs = actionTs,
                            ),
                        ),
                    )

                test("should process Configure reaction for Jira action") {
                    every { processConfigureReactionForJira.handle(any()) } just Runs
                    processSlackBlockActions.handle(event)

                    verify {
                        processConfigureReactionForJira.handle(
                            SlackToJiraTenantTrigger(
                                event.triggerId,
                                event.team,
                                UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                            ),
                        )
                    }
                }
            }

            context("Delete reaction for Jira action") {
                val slackMessageTs = SlackTs("1360782804.083113")
                val actionTs = SlackTs("1360782804.030455")
                val event =
                    SlackBlockActionsEvent(
                        SlackBlockActionsEvent.MessageContainer(
                            slackMessageTs,
                            SlackChannelId("D1234567890"),
                            false,
                        ),
                        "triggerId",
                        Team(SlackTeamId("T1234567890"), "domain"),
                        listOf(
                            SlackBlockActionsEvent.Action(
                                actionId = "delete_jira_reaction",
                                blockId = "blockId",
                                text = SlackBlockActionsEvent.Text("plain_text", "Configure ", true),
                                value = "123e4567-e89b-12d3-a456-426614174000",
                                style = "primary",
                                type = "button",
                                actionTs = actionTs,
                            ),
                        ),
                    )

                test("should process Delete reaction for Jira action") {
                    every { processDeleteReactionForJira.handle(any()) } just Runs
                    processSlackBlockActions.handle(event)

                    verify {
                        processDeleteReactionForJira.handle(
                            JiraBoardConfigurationIdTrigger(
                                event.triggerId,
                                event.team,
                                UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                                event.container as SlackBlockActionsEvent.MessageContainer,
                            ),
                        )
                    }
                }
            }

            context("Ask for Support action") {
                val slackMessageTs = SlackTs("1360782804.083113")
                val actionTs = SlackTs("1360782804.030455")
                val event =
                    SlackBlockActionsEvent(
                        SlackBlockActionsEvent.MessageContainer(
                            slackMessageTs,
                            SlackChannelId("D1234567890"),
                            false,
                        ),
                        "triggerId",
                        Team(SlackTeamId("T1234567890"), "domain"),
                        listOf(
                            SlackBlockActionsEvent.Action(
                                actionId = "ask_for_support",
                                blockId = "blockId",
                                text = SlackBlockActionsEvent.Text("plain_text", "Ask for Support ", true),
                                value = "123e4567-e89b-12d3-a456-426614174000",
                                style = "primary",
                                type = "button",
                                actionTs = actionTs,
                            ),
                        ),
                    )

                test("should process it") {
                    every { processAskForSupportAction.handle(any(), any()) } just Runs
                    processSlackBlockActions.handle(event)

                    verify {
                        processAskForSupportAction.handle(
                            event,
                            AskForSupportAction(UUID.fromString("123e4567-e89b-12d3-a456-426614174000")),
                        )
                    }
                }
            }

            context("Connect to Jira Cloud action") {
                val slackMessageTs = SlackTs("1360782804.083113")
                val actionTs = SlackTs("1360782804.030455")
                val event =
                    SlackBlockActionsEvent(
                        SlackBlockActionsEvent.MessageContainer(
                            slackMessageTs,
                            SlackChannelId("D1234567890"),
                            false,
                        ),
                        "triggerId",
                        Team(SlackTeamId("T1234567890"), "domain"),
                        listOf(
                            SlackBlockActionsEvent.Action(
                                actionId = "connect_jira_cloud",
                                blockId = "blockId",
                                text = SlackBlockActionsEvent.Text("plain_text", "Connect your Jira Cloud", true),
                                value = "123e4567-e89b-12d3-a456-426614174000",
                                style = "primary",
                                type = "button",
                                actionTs = actionTs,
                            ),
                        ),
                    )

                test("should save the onboarding DM channel in Redis") {
                    every { redisTemplate.opsForValue().set(any(), any()) } just Runs
                    processSlackBlockActions.handle(event)
                    verify {
                        redisTemplate.opsForValue().set(
                            "onboarding-dm:${event.team.id.id}",
                            (event.container as SlackBlockActionsEvent.MessageContainer).channelId,
                        )
                    }
                }
            }

            test("Send SlackTeamProcessed event") {
                val slackMessageTs = SlackTs("1360782804.083113")
                val actionTs = SlackTs("1360782804.030455")
                val event =
                    SlackBlockActionsEvent(
                        SlackBlockActionsEvent.MessageContainer(
                            slackMessageTs,
                            SlackChannelId("D1234567890"),
                            false,
                        ),
                        "triggerId",
                        Team(SlackTeamId("T1234567890"), "domain"),
                        listOf(
                            SlackBlockActionsEvent.Action(
                                actionId = "lalala",
                                blockId = "blockId",
                                text = SlackBlockActionsEvent.Text("plain_text", "Configure ", true),
                                value = "123e4567-e89b-12d3-a456-426614174000",
                                style = "primary",
                                type = "button",
                                actionTs = actionTs,
                            ),
                        ),
                    )

                every { applicationEventPublisher.publishEvent(any()) } just Runs

                // When
                processSlackBlockActions.handle(event)

                // Then
                verify {
                    applicationEventPublisher.publishEvent(
                        match<SlackTeamProcessedEvent> {
                            it.source == processSlackBlockActions &&
                                it.domain == SlackDomain("domain") &&
                                it.teamId == SlackTeamId("T1234567890")
                        },
                    )
                }
            }

            context("with invalid action id format") {
                val slackMessageTs = SlackTs("1360782804.083113")
                val actionTs = SlackTs("1360782804.030455")
                val event =
                    SlackBlockActionsEvent(
                        SlackBlockActionsEvent.MessageContainer(
                            slackMessageTs,
                            SlackChannelId("D1234567890"),
                            false,
                        ),
                        "triggerId",
                        Team(SlackTeamId("T1234567890"), "domain"),
                        actions =
                            listOf(
                                SlackBlockActionsEvent.Action(
                                    actionId = "invalid",
                                    blockId = "blockId",
                                    text = SlackBlockActionsEvent.Text("plain_text", "5", true),
                                    value = "5",
                                    style = "primary",
                                    type = "button",
                                    actionTs = actionTs,
                                ),
                            ),
                    )

                test("should NOT process any action") {
                    processSlackBlockActions.handle(event)

                    verify(exactly = 0) {
                        processCsatAction.handle(any(), any(), any())
                    }

                    verify(exactly = 0) {
                        processConfigureReactionForJira.handle(any())
                    }
                }
            }
        }
    })
