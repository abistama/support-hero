package com.abistama.supporthero.application.slackJira

import arrow.core.Either
import com.abistama.supporthero.application.slack.SlackClient
import com.abistama.supporthero.domain.jira.SlackToJiraCsat
import com.abistama.supporthero.domain.slack.SlackPostBlocksMessage
import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.SlackUserId
import com.abistama.supporthero.infrastructure.jira.repository.JiraCsatRepository
import com.slack.api.methods.response.chat.ChatPostMessageResponse
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Duration
import java.util.*

class SendCsatReminderUseCaseTest :
    FunSpec({

        test("Should send a CSAT reminder to the user that reported the ticket") {
            // Given
            val jiraCsatRepository = mockk<JiraCsatRepository>(relaxed = true)
            val slackClient = mockk<SlackClient>()
            val sendCsatReminder = SendCsatReminderUseCase(jiraCsatRepository, slackClient)
            val ticketReporter = SlackUserId("U1234567890")
            val slackTeamId = SlackTeamId("T1234567890")
            val jiraCsatId = UUID.randomUUID()
            val expectedSlackPostBlocksMessage =
                SlackPostBlocksMessage(
                    ticketReporter,
                    createCsatSurveyBlock(SlackToJiraCsat(jiraCsatId, ticketReporter, slackTeamId, "SUPP-123")),
                    slackTeamId,
                )

            every {
                jiraCsatRepository.getReminders()
            } returns
                listOf(
                    SlackToJiraCsat(
                        jiraCsatId,
                        ticketReporter,
                        slackTeamId,
                        "SUPP-123",
                    ),
                )

            every {
                slackClient.postBlocks(expectedSlackPostBlocksMessage)
            } returns Either.Right(ChatPostMessageResponse())

            // When
            sendCsatReminder.execute()

            // Then
            verify(exactly = 1) { slackClient.postBlocks(expectedSlackPostBlocksMessage) }
        }

        test("Should decrease the number of reminder attempts when successfully sending a CSAT reminder") {
            // Given
            val jiraCsatRepository = mockk<JiraCsatRepository>()
            val slackClient = mockk<SlackClient>()
            val sendCsatReminder = SendCsatReminderUseCase(jiraCsatRepository, slackClient)
            val ticketReporter = SlackUserId("U1234567890")
            val slackTeamId = SlackTeamId("T1234567890")
            val jiraCsatId = UUID.randomUUID()

            every {
                jiraCsatRepository.getReminders()
            } returns
                listOf(
                    SlackToJiraCsat(
                        jiraCsatId,
                        ticketReporter,
                        slackTeamId,
                        "SUPP-123",
                    ),
                )

            every { jiraCsatRepository.decreaseReminderAttempts(jiraCsatId, Duration.ofHours(24)) } returns Unit

            every {
                slackClient.postBlocks(any())
            } returns Either.Right(ChatPostMessageResponse())

            // When
            sendCsatReminder.execute()

            // Then
            verify(exactly = 1) { jiraCsatRepository.decreaseReminderAttempts(jiraCsatId, Duration.ofHours(24)) }
        }
    })
