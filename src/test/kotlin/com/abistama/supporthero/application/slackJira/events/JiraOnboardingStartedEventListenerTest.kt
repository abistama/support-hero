package com.abistama.supporthero.application.slackJira.events

import com.abistama.supporthero.application.slackJira.SlackToJiraOnboardingMessageService
import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.SlackUserId
import com.abistama.supporthero.infrastructure.repository.SlackToJiraRepository
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.*

class JiraOnboardingStartedEventListenerTest :
    FunSpec({
        val slackToJiraRepository = mockk<SlackToJiraRepository>(relaxed = true)
        val slackToJiraOnboardingMessageService = mockk<SlackToJiraOnboardingMessageService>(relaxed = true)
        val jiraOnboardingStartedEventListener =
            JiraOnboardingStartedEventListener(slackToJiraRepository, slackToJiraOnboardingMessageService)

        context("onApplicationEvent") {
            test("should call firstOnboarding when no previous onboarding exists") {
                val slackTeamId = SlackTeamId("T024BE7LD00")
                val event = JiraOnboardingStartedEvent("source", SlackUserId("U0G9QF9C600"), slackTeamId)

                every { slackToJiraRepository.get(slackTeamId) } returns null

                // When
                jiraOnboardingStartedEventListener.onApplicationEvent(event)

                // Then
                verify { slackToJiraOnboardingMessageService.firstOnboarding(event) }
            }

            test("should call alreadyConnected when previous onboarding exists") {
                val slackTeamId = SlackTeamId("T024BE7LD00")
                val event = JiraOnboardingStartedEvent("source", SlackUserId("U0G9QF9C600"), slackTeamId)

                val existingOnboarding = UUID.randomUUID()
                every { slackToJiraRepository.get(slackTeamId) } returns existingOnboarding

                jiraOnboardingStartedEventListener.onApplicationEvent(event)

                verify {
                    slackToJiraOnboardingMessageService.alreadyConnected(
                        existingOnboarding,
                        SlackUserId("U0G9QF9C600"),
                        slackTeamId,
                    )
                }
            }
        }
    })
