package com.abistama.supporthero.application.slackJira

import arrow.core.Either
import com.abistama.supporthero.application.slack.SlackClient
import com.abistama.supporthero.domain.slack.SlackChannelId
import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.events.SlackTs
import com.abistama.supporthero.infrastructure.slack.events.adapter.SlackBlockActionsEvent
import com.abistama.supporthero.infrastructure.slack.events.adapter.Team
import com.slack.api.methods.response.views.ViewsOpenResponse
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.*

class ProcessAskForSupportActionTest :
    FunSpec({
        val slackAutoRefreshTokenClient = mockk<SlackClient>()
        val processAskForSupportAction = ProcessAskForSupportAction(slackAutoRefreshTokenClient)

        beforeTest {
            clearAllMocks()
        }

        context("handle") {
            test("should open view when handle is called") {
                // Given
                val event =
                    SlackBlockActionsEvent(
                        container =
                            SlackBlockActionsEvent.MessageContainer(
                                messageTs = SlackTs("1234567890.123456"),
                                channelId = SlackChannelId("C1234567890"),
                                isEphemeral = true,
                            ),
                        team = Team(SlackTeamId("T1234567890"), "domain"),
                        triggerId = "triggerId",
                        actions = listOf(),
                    )
                val action = AskForSupportAction(UUID.randomUUID())

                every { slackAutoRefreshTokenClient.openView(any()) } returns
                    Either.Right(
                        ViewsOpenResponse().apply {
                            this.isOk = true
                        },
                    )

                // When
                processAskForSupportAction.handle(event, action)

                // Then
                verify { slackAutoRefreshTokenClient.openView(any()) }
            }
        }
    })
