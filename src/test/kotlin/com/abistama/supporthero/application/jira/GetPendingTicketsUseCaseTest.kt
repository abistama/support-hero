package com.abistama.supporthero.application.jira

import arrow.core.Either
import com.abistama.supporthero.application.slackJira.GetPendingTicketsUseCase
import com.abistama.supporthero.application.slackJira.SlackToJiraPendingTickedId
import com.abistama.supporthero.application.slackJira.SlackToJiraPendingTicket
import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.SlackUserId
import com.abistama.supporthero.infrastructure.jira.Fixtures
import com.abistama.supporthero.infrastructure.jira.JiraAutoRefreshTokenClient
import com.abistama.supporthero.infrastructure.jira.repository.JiraTicketTrackingRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.context.ApplicationEventPublisher
import java.util.*

data class GetPendingTicketsTestData(
    val tickets: List<SlackToJiraPendingTicket>,
)

class GetPendingTicketsUseCaseTest :
    FunSpec({

        val fixtures = Fixtures()
        val jiraCloudId1 = fixtures.getJiraCloudIdFixture()
        val jiraCloudId2 = fixtures.getJiraCloudIdFixture()
        val slackTeamId = SlackTeamId("T1234567890")

        context("should get pending tickets and update them if needed") {
            withData(
                GetPendingTicketsTestData(
                    listOf(
                        SlackToJiraPendingTicket(
                            jiraCloudId1,
                            slackTeamId,
                            SlackToJiraPendingTickedId(UUID.randomUUID()),
                            fixtures.getJiraIssueResponseFixture(key = "TEST-1"),
                            SlackUserId("U1234567890"),
                        ),
                    ),
                ),
                GetPendingTicketsTestData(
                    listOf(
                        SlackToJiraPendingTicket(
                            jiraCloudId1,
                            slackTeamId,
                            SlackToJiraPendingTickedId(UUID.randomUUID()),
                            fixtures.getJiraIssueResponseFixture(key = "TEST-1"),
                            SlackUserId("U1234567890"),
                        ),
                        SlackToJiraPendingTicket(
                            jiraCloudId2,
                            slackTeamId,
                            SlackToJiraPendingTickedId(UUID.randomUUID()),
                            fixtures.getJiraIssueResponseFixture(key = "TEST-2", status = "To Do"),
                            SlackUserId("U1234567890"),
                        ),
                        SlackToJiraPendingTicket(
                            jiraCloudId2,
                            slackTeamId,
                            SlackToJiraPendingTickedId(UUID.randomUUID()),
                            fixtures.getJiraIssueResponseFixture(key = "TEST-3"),
                            SlackUserId("U1234567890"),
                        ),
                    ),
                ),
                GetPendingTicketsTestData(emptyList()),
            ) { testData ->

                // Given
                val jiraTicketTrackingRepository =
                    mockk<JiraTicketTrackingRepository>(relaxed = true) {
                        every { getPending() } returns testData.tickets
                    }

                val jiraAutoRefreshTokenClient = mockk<JiraAutoRefreshTokenClient>()

                every {
                    jiraAutoRefreshTokenClient.getIssue(any(), "TEST-1")
                } returns
                    Either.Right(
                        fixtures.getJiraIssueResponseFixture(
                            key = "TEST-1",
                            status = "Done",
                        ),
                    )

                every {
                    jiraAutoRefreshTokenClient.getIssue(any(), "TEST-2")
                } returns
                    Either.Right(
                        fixtures.getJiraIssueResponseFixture(
                            key = "TEST-2",
                            status = "WIP",
                        ),
                    )

                every {
                    jiraAutoRefreshTokenClient.getIssue(any(), "TEST-3")
                } returns
                    Either.Right(
                        fixtures.getJiraIssueResponseFixture(
                            key = "TEST-3",
                        ),
                    )

                val applicationEventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)

                val getPendingTicketsUseCase =
                    GetPendingTicketsUseCase(
                        jiraTicketTrackingRepository,
                        jiraAutoRefreshTokenClient,
                        applicationEventPublisher,
                    )

                // When
                getPendingTicketsUseCase.execute()

                // Then
                verify(exactly = 1) { jiraTicketTrackingRepository.getPending() }

                testData.tickets.forEach {
                    verify(exactly = 1) {
                        jiraAutoRefreshTokenClient.getIssue(it.jiraCloudId, it.jiraIssueResponse.key)
                    }
                }

                testData.tickets.filter { it.jiraIssueResponse.fields.status.name != "To Do" }.forEach {
                    verify(exactly = 1) {
                        jiraTicketTrackingRepository.updateStatus(it.id, any())
                    }
                }
            }
        }
    })
