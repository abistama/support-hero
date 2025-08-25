package com.abistama.supporthero.application.slackJira

import arrow.core.Either
import com.abistama.supporthero.application.slack.SlackAPIError
import com.abistama.supporthero.application.slack.SlackClient
import com.abistama.supporthero.domain.slack.SlackChannelId
import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.SlackUpdateMessage
import com.abistama.supporthero.domain.slack.events.SlackTs
import com.abistama.supporthero.infrastructure.jira.repository.JiraCsatRepository
import com.abistama.supporthero.infrastructure.slack.events.adapter.SlackBlockActionsEvent
import com.slack.api.methods.response.chat.ChatUpdateResponse
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.*

class ProcessCsatActionTest :
    FunSpec({
        val jiraCsatRepository = mockk<JiraCsatRepository>()
        val autoRefreshTokenClient = mockk<SlackClient>()
        val processCsatAction = ProcessCsatAction(jiraCsatRepository, autoRefreshTokenClient)

        beforeTest {
            clearAllMocks()
        }

        context("handle") {
            test("should update CSAT value and send feedback message") {
                val teamId = SlackTeamId("T1234567890")
                val container =
                    SlackBlockActionsEvent.MessageContainer(SlackTs("1360782804.083113"), SlackChannelId("C1234567890"), true)
                val jiraCsatId = UUID.randomUUID()
                val action = CSatAction(jiraCsatId, 5)

                every { jiraCsatRepository.updateCsatValue(any(), any()) } returns Unit
                every { autoRefreshTokenClient.updateMessage(any()) } returns
                    Either.Right(
                        ChatUpdateResponse().apply {
                            this.isOk = true
                        },
                    )

                processCsatAction.handle(teamId, container, action)

                verify { jiraCsatRepository.updateCsatValue(jiraCsatId, 5) }
                verify {
                    autoRefreshTokenClient.updateMessage(
                        SlackUpdateMessage(
                            container.channelId,
                            container.messageTs,
                            "Thank you for your feedback!",
                            teamId,
                        ),
                    )
                }
            }

            test("should handle error when unable to update message") {
                val teamId = SlackTeamId("T1234567890")
                val container =
                    SlackBlockActionsEvent.MessageContainer(SlackTs("1360782804.083113"), SlackChannelId("C1234567890"), true)
                val jiraCsatId = UUID.randomUUID()
                val action = CSatAction(jiraCsatId, 1)

                every { jiraCsatRepository.updateCsatValue(any(), any()) } returns Unit
                every { autoRefreshTokenClient.updateMessage(any()) } returns Either.Left(SlackAPIError("Unable to update message"))

                processCsatAction.handle(teamId, container, action)

                verify { jiraCsatRepository.updateCsatValue(jiraCsatId, 1) }
                verify {
                    autoRefreshTokenClient.updateMessage(
                        SlackUpdateMessage(
                            container.channelId,
                            container.messageTs,
                            "Thank you for your feedback!",
                            teamId,
                        ),
                    )
                }
            }
        }
    })
