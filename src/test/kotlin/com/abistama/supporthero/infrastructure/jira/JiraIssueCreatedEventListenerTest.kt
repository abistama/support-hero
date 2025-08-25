package com.abistama.supporthero.infrastructure.jira

import arrow.core.Either
import com.abistama.supporthero.application.jira.JiraAPIError
import com.abistama.supporthero.domain.jira.JiraTenant
import com.abistama.supporthero.domain.slack.SlackUserId
import com.abistama.supporthero.infrastructure.jira.events.JiraIssueCreatedEvent
import com.abistama.supporthero.infrastructure.jira.events.JiraIssueCreatedEventListener
import com.abistama.supporthero.infrastructure.jira.repository.JiraTicketTrackingRepository
import com.abistama.supporthero.infrastructure.repository.JiraCloudId
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.*

class JiraIssueCreatedEventListenerTest :
    FunSpec({

        val fixtures = Fixtures()

        test("should NOT add the Jira ticket to the tickets tracking repository if it can't get the issue details") {
            // Given
            val jiraCloudId = JiraCloudId(UUID.randomUUID())
            val issue = IssueResponse("1234", "TOMR-1234", "self")
            val event = JiraIssueCreatedEvent("source", jiraCloudId, issue, false, SlackUserId("U1234567890"))
            val expectedJiraIssueResponse = fixtures.getJiraIssueResponseFixture()

            val jiraAutoRefreshTokenClient =
                mockk<JiraAutoRefreshTokenClient> {
                    every {
                        getIssue(
                            jiraCloudId,
                            issue.key,
                        )
                    } returns Either.Left(JiraAPIError("error"))
                }

            val jiraTicketTrackingRepository =
                mockk<JiraTicketTrackingRepository> {
                    every { add(event, expectedJiraIssueResponse) } returns Unit
                }

            val jiraIssueCreatedEventListener =
                JiraIssueCreatedEventListener(jiraAutoRefreshTokenClient, jiraTicketTrackingRepository)

            // When
            jiraIssueCreatedEventListener.onApplicationEvent(event)

            // Then
            verify(exactly = 0) { jiraTicketTrackingRepository.add(any(), any()) }
        }

        test("should add the Jira ticket to the tickets tracking repository") {
            // Given
            val jiraCloudId = JiraCloudId(UUID.randomUUID())
            val jiraTenant = JiraTenant(jiraCloudId, "accessToken")
            val issue = IssueResponse("1234", "TOMR-1234", "self")
            val event = JiraIssueCreatedEvent("source", jiraCloudId, issue, true, SlackUserId("U1234567890"))
            val expectedJiraIssueResponse = fixtures.getJiraIssueResponseFixture()

            val jiraAutoRefreshTokenClient =
                mockk<JiraAutoRefreshTokenClient> {
                    every {
                        getIssue(
                            jiraTenant.cloudId,
                            issue.key,
                        )
                    } returns Either.Right(expectedJiraIssueResponse)
                }

            val jiraTicketTrackingRepository =
                mockk<JiraTicketTrackingRepository> {
                    every { add(event, expectedJiraIssueResponse) } returns Unit
                }

            val jiraIssueCreatedEventListener =
                JiraIssueCreatedEventListener(jiraAutoRefreshTokenClient, jiraTicketTrackingRepository)

            // When
            jiraIssueCreatedEventListener.onApplicationEvent(event)

            // Then
            verify(exactly = 1) { jiraTicketTrackingRepository.add(event, expectedJiraIssueResponse) }
        }
    })
