package com.abistama.supporthero.infrastructure.slackToJira.events

import com.abistama.supporthero.application.slackJira.events.JiraIssueStatusUpdatedEvent
import com.abistama.supporthero.application.slackJira.events.JiraIssueStatusUpdatedEventListener
import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.SlackUserId
import com.abistama.supporthero.infrastructure.jira.Fixtures
import com.abistama.supporthero.infrastructure.jira.repository.JiraCsatRepository
import com.abistama.supporthero.infrastructure.repository.JiraCloudId
import io.kotest.core.spec.style.FunSpec
import io.mockk.mockk
import io.mockk.verify
import java.time.Duration
import java.util.*

class JiraIssueStatusUpdatedEventListenerTest :
    FunSpec({

        val fixtures = Fixtures()

        test("should add a CSAT request to the tickets tracking repository if the issue is resolved") {
            // Given
            val jiraCloudId = JiraCloudId(UUID.randomUUID())
            val sendTo = SlackUserId("U1234567890")
            val jiraCsatRepository = mockk<JiraCsatRepository>(relaxed = true)
            val jiraIssueStatusUpdatedEventListener =
                JiraIssueStatusUpdatedEventListener(jiraCsatRepository)
            val slackTeamId = SlackTeamId("T1234567890")
            val jiraIssueResponse = fixtures.getJiraIssueResponseFixture(status = "Done")
            val event = JiraIssueStatusUpdatedEvent("source", jiraCloudId, slackTeamId, jiraIssueResponse, SlackUserId("U1234567890"))

            // When
            jiraIssueStatusUpdatedEventListener.onApplicationEvent(event)

            // Then
            verify(exactly = 1) {
                jiraCsatRepository.createCsat(
                    jiraCloudId,
                    sendTo,
                    "TO-19",
                    "TO",
                    3,
                    Duration.ofMinutes(1),
                )
            }
        }

        test("should NOT do anything if the issue is not resolved") {
            // Given
            val jiraCloudId = JiraCloudId(UUID.randomUUID())
            val jiraCsatRepository = mockk<JiraCsatRepository>(relaxed = true)
            val jiraIssueStatusUpdatedEventListener =
                JiraIssueStatusUpdatedEventListener(jiraCsatRepository)
            val slackTeamId = SlackTeamId("T1234567890")
            val jiraIssueResponse = fixtures.getJiraIssueResponseFixture()
            val event = JiraIssueStatusUpdatedEvent("source", jiraCloudId, slackTeamId, jiraIssueResponse, SlackUserId("U1234567890"))

            // When
            jiraIssueStatusUpdatedEventListener.onApplicationEvent(event)

            // Then
            verify(exactly = 0) {
                jiraCsatRepository.createCsat(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            }
        }
    })
