package com.abistama.supporthero.application.slack.events

import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.infrastructure.repository.SlackTenantRepository
import io.kotest.core.spec.style.FunSpec
import io.mockk.mockk
import io.mockk.verify

class SlackTeamProcessedEventListenerTest :
    FunSpec({
        val slackTenantRepository = mockk<SlackTenantRepository>(relaxed = true)
        val slackTeamProcessedEventListener = SlackTeamProcessedEventListener(slackTenantRepository)

        context("onApplicationEvent") {
            test("should update domain when event is received") {
                val event = SlackTeamProcessedEvent(this, SlackTeamId("T1234567890"), SlackDomain("new-domain"))

                slackTeamProcessedEventListener.onApplicationEvent(event)

                verify { slackTenantRepository.updateDomain(event) }
            }
        }
    })
