package com.abistama.supporthero.application.slackJira

import arrow.core.Either
import com.abistama.supporthero.application.jira.JiraAPIError
import com.abistama.supporthero.application.slack.SlackAPIError
import com.abistama.supporthero.application.slack.SlackClient
import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.SlackToJiraTenantTrigger
import com.abistama.supporthero.infrastructure.jira.JiraAutoRefreshTokenClient
import com.abistama.supporthero.infrastructure.jira.Project
import com.abistama.supporthero.infrastructure.repository.JiraCloudId
import com.abistama.supporthero.infrastructure.repository.JiraTenantRepository
import com.abistama.supporthero.infrastructure.slack.events.adapter.SlackBlockActionsEvent
import com.abistama.supporthero.infrastructure.slack.events.adapter.Team
import com.slack.api.methods.response.views.ViewsOpenResponse
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.*

class ProcessConfigureReactionForJiraTest :
    FunSpec({
        val jiraTenantRepository = mockk<JiraTenantRepository>()
        val autoRefreshTokenClient = mockk<SlackClient>()
        val jiraAutoRefreshTokenClient = mockk<JiraAutoRefreshTokenClient>()
        val processConfigureReactionForJira =
            ProcessConfigureReactionForJira(jiraTenantRepository, autoRefreshTokenClient, jiraAutoRefreshTokenClient)

        beforeTest {
            clearAllMocks()
        }

        context("handle") {
            test("should open view when JiraCloudId is found") {
                val event =
                    SlackBlockActionsEvent(
                        SlackBlockActionsEvent.ViewContainer(
                            "viewId",
                        ),
                        team = Team(SlackTeamId("T1234567890"), "domain"),
                        triggerId = "triggerId",
                        actions = listOf(),
                    )
                val action = ConfigureReactionForJiraAction(UUID.randomUUID())

                every { jiraTenantRepository.getJiraCloudId(any()) } returns JiraCloudId(UUID.randomUUID())
                every { jiraAutoRefreshTokenClient.getProjects(any()) } returns
                    Either.Right(
                        listOf(
                            Project(
                                "key",
                                "name",
                            ),
                        ),
                    )
                every { autoRefreshTokenClient.openView(any()) } returns
                    Either.Right(
                        ViewsOpenResponse().apply {
                            this.isOk = true
                        },
                    )

                processConfigureReactionForJira.handle(SlackToJiraTenantTrigger(event.triggerId, event.team, action.slackToJiraTenant))

                verify { autoRefreshTokenClient.openView(any()) }
            }

            test("should NOT open view when JiraCloudId is not found") {
                // Given
                val event =
                    SlackBlockActionsEvent(
                        SlackBlockActionsEvent.ViewContainer(
                            "viewId",
                        ),
                        team = Team(SlackTeamId("T1234567890"), "domain"),
                        triggerId = "triggerId",
                        actions = listOf(),
                    )
                val action = ConfigureReactionForJiraAction(UUID.randomUUID())

                every { jiraTenantRepository.getJiraCloudId(any()) } returns null

                // When
                processConfigureReactionForJira.handle(SlackToJiraTenantTrigger(event.triggerId, event.team, action.slackToJiraTenant))

                // Then
                verify(exactly = 0) { autoRefreshTokenClient.openView(any()) }
            }

            test("should NOT open view when unable to get Jira projects") {
                // Givne
                val event =
                    SlackBlockActionsEvent(
                        SlackBlockActionsEvent.ViewContainer(
                            "viewId",
                        ),
                        team = Team(SlackTeamId("T1234567890"), "domain"),
                        triggerId = "triggerId",
                        actions = listOf(),
                    )
                val action = ConfigureReactionForJiraAction(UUID.randomUUID())

                every { jiraTenantRepository.getJiraCloudId(any()) } returns JiraCloudId(UUID.randomUUID())
                every { jiraAutoRefreshTokenClient.getProjects(any()) } returns Either.Left(JiraAPIError("error"))

                // When
                processConfigureReactionForJira.handle(SlackToJiraTenantTrigger(event.triggerId, event.team, action.slackToJiraTenant))

                // Then
                verify(exactly = 0) { autoRefreshTokenClient.openView(any()) }
            }

            test("should NOT open view when unable to open view") {
                val event =
                    SlackBlockActionsEvent(
                        SlackBlockActionsEvent.ViewContainer(
                            "viewId",
                        ),
                        team = Team(SlackTeamId("T1234567890"), "domain"),
                        triggerId = "triggerId",
                        actions = listOf(),
                    )
                val action = ConfigureReactionForJiraAction(UUID.randomUUID())

                every { jiraTenantRepository.getJiraCloudId(any()) } returns JiraCloudId(UUID.randomUUID())
                every { jiraAutoRefreshTokenClient.getProjects(any()) } returns
                    Either.Right(
                        listOf(
                            Project(
                                "key",
                                "name",
                            ),
                        ),
                    )
                every { autoRefreshTokenClient.openView(any()) } returns Either.Left(SlackAPIError("error"))

                // When
                processConfigureReactionForJira.handle(SlackToJiraTenantTrigger(event.triggerId, event.team, action.slackToJiraTenant))

                // Then
                verify { autoRefreshTokenClient.openView(any()) }
            }
        }
    })
