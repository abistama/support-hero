package com.abistama.supporthero.application.slackJira

import com.abistama.supporthero.application.slack.SlackClient
import com.abistama.supporthero.domain.slack.JiraBoardConfiguration
import com.abistama.supporthero.domain.slack.SlackChannelId
import com.abistama.supporthero.domain.slack.SlackPostBlocksMessage
import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.SlackUserId
import com.abistama.supporthero.domain.slack.SlackUserTrigger
import com.abistama.supporthero.domain.slackToJira.DisplayJiraBoards
import com.abistama.supporthero.domain.slackToJira.JiraBoardsStatistics
import com.abistama.supporthero.infrastructure.jira.IssueType
import com.abistama.supporthero.infrastructure.jira.Project
import com.abistama.supporthero.infrastructure.repository.SlackToJiraConfigurationRepository
import com.abistama.supporthero.infrastructure.repository.SlackToJiraRepository
import com.abistama.supporthero.infrastructure.slack.events.adapter.Team
import com.abistama.supporthero.infrastructure.slack.events.adapter.User
import com.slack.api.model.block.element.BlockElements.button
import com.slack.api.model.kotlin_extension.block.withBlocks
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.*

class GetCurrentReactionsForJiraTest :
    FunSpec({

        val slackToJiraRepository = mockk<SlackToJiraRepository>()
        val slackToJiraConfigurationRepository = mockk<SlackToJiraConfigurationRepository>()
        val slackClient = mockk<SlackClient>(relaxed = true)
        val slackToJiraOnboardingMessageService = mockk<SlackToJiraOnboardingMessageService>(relaxed = true)
        val getCurrentReactionsForJira =
            GetCurrentReactionsForJira(
                slackToJiraRepository,
                slackToJiraConfigurationRepository,
                slackClient,
                slackToJiraOnboardingMessageService,
            )

        context("handle method") {
            test("should send configurations when user has configurations") {
                val userTrigger =
                    SlackUserTrigger(
                        "trigger",
                        Team(SlackTeamId("T9876543210"), "domain"),
                        User(SlackUserId("U1234567890")),
                    )
                val firstConfigId = UUID.randomUUID()
                val secondConfigId = UUID.randomUUID()
                val jiraBoardsStatistics =
                    DisplayJiraBoards(
                        UUID.randomUUID(),
                        listOf(
                            JiraBoardsStatistics(
                                firstConfigId,
                                JiraBoardConfiguration.UserConfiguration(
                                    Project("PROJ", "Project"),
                                    "ticket",
                                    "This is my feedback message",
                                    IssueType("Task"),
                                    SlackUserId("U1234567890"),
                                    true,
                                    false,
                                ),
                                5.0f,
                            ),
                            JiraBoardsStatistics(
                                secondConfigId,
                                JiraBoardConfiguration.UserConfiguration(
                                    Project("PROJ", "Project"),
                                    "fire",
                                    "This is my incident message",
                                    IssueType("Task"),
                                    SlackUserId("U1234567890"),
                                    false,
                                    false,
                                    SlackChannelId("C1234567890"),
                                ),
                                5.0f,
                            ),
                        ),
                    )
                val expectedBlocks =
                    withBlocks {
                        header {
                            text(":gear: Project configurations", emoji = true)
                        }
                        section {
                            markdownText("Here you can see the configurations owned by you or create a new one.")
                        }
                        divider()
                        section {
                            markdownText(
                                """
                                :ticket: in all channels creates a ticket in PROJ Jira project
                                """.trimIndent(),
                            )
                            accessory {
                                button {
                                    text("Edit", emoji = true)
                                    value(firstConfigId.toString())
                                    actionId("edit_jira_reaction||")
                                }
                            }
                        }
                        section {
                            markdownText(
                                """
                                :large_green_circle: Average CSAT: 5.0
                                """.trimIndent(),
                            )
                            accessory {
                                button {
                                    text("Delete", emoji = true)
                                    style("danger")
                                    confirm {
                                        title("Are you sure?")
                                        markdownText("This action will delete the configuration.")
                                        confirm("Yes, delete it")
                                        deny("No, keep it")
                                    }
                                    value(firstConfigId.toString())
                                    actionId("delete_jira_reaction||")
                                }
                            }
                        }
                        section {
                            markdownText(
                                """
                                :fire: in <#C1234567890> creates a ticket in PROJ Jira project
                                """.trimIndent(),
                            )
                            accessory {
                                button {
                                    text("Edit", emoji = true)
                                    value(secondConfigId.toString())
                                    actionId("edit_jira_reaction||")
                                }
                            }
                        }
                        section {
                            markdownText(
                                """
                                No CSAT configured.
                                """.trimIndent(),
                            )
                            accessory {
                                button {
                                    text("Delete", emoji = true)
                                    style("danger")
                                    confirm {
                                        title("Are you sure?")
                                        markdownText("This action will delete the configuration.")
                                        confirm("Yes, delete it")
                                        deny("No, keep it")
                                    }
                                    value(secondConfigId.toString())
                                    actionId("delete_jira_reaction||")
                                }
                            }
                        }
                    }

                every { slackToJiraConfigurationRepository.getByOwner(userTrigger.user.id) } returns jiraBoardsStatistics

                getCurrentReactionsForJira.handle(userTrigger)

                verify(exactly = 1) {
                    slackClient.postBlocks(
                        SlackPostBlocksMessage(
                            userTrigger.user.id,
                            expectedBlocks,
                            userTrigger.team.id,
                        ),
                    )
                }
            }

            test("should initiate onboarding when no tenant is found for the team") {
                val userTrigger =
                    SlackUserTrigger(
                        "trigger",
                        Team(SlackTeamId("T9876543210"), "domain"),
                        User(SlackUserId("U1234567890")),
                    )

                every { slackToJiraConfigurationRepository.getByOwner(userTrigger.user.id) } returns null
                every { slackToJiraRepository.get(userTrigger.team.id) } returns null

                getCurrentReactionsForJira.handle(userTrigger)

                verify { slackToJiraOnboardingMessageService.firstOnboarding(any()) }
            }

            test("should send no configurations message when user has no configurations but tenant exists") {
                val userTrigger =
                    SlackUserTrigger(
                        "trigger",
                        Team(SlackTeamId("T9876543210"), "domain"),
                        User(SlackUserId("U1234567890")),
                    )
                val slackToJiraId = UUID.randomUUID()
                val expectedBlocks =
                    withBlocks {
                        header {
                            text(":gear: Project configurations", emoji = true)
                        }
                        section {
                            markdownText("You don't have any configurations yet.")
                        }
                        actions {
                            button {
                                text("Create one :heavy_plus_sign:", emoji = true)
                                value("$slackToJiraId")
                                actionId("configure_jira_reaction||")
                            }
                        }
                    }

                every { slackToJiraConfigurationRepository.getByOwner(userTrigger.user.id) } returns null
                every { slackToJiraRepository.get(userTrigger.team.id) } returns slackToJiraId

                getCurrentReactionsForJira.handle(userTrigger)

                verify {
                    slackClient.postBlocks(
                        SlackPostBlocksMessage(
                            userTrigger.user.id,
                            expectedBlocks,
                            userTrigger.team.id,
                        ),
                    )
                }
            }
        }
    })
