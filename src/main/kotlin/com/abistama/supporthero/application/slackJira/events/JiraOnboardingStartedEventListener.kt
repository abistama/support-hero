package com.abistama.supporthero.application.slackJira.events

import com.abistama.supporthero.application.slackJira.SlackToJiraOnboardingMessageService
import com.abistama.supporthero.infrastructure.repository.SlackToJiraRepository
import mu.KLogging
import org.springframework.context.ApplicationListener
import java.util.*

class JiraOnboardingStartedEventListener(
    private val slackToJiraRepository: SlackToJiraRepository,
    private val slackToJiraOnboardingMessageService: SlackToJiraOnboardingMessageService,
) : ApplicationListener<JiraOnboardingStartedEvent> {
    companion object {
        private val logger = KLogging().logger()
    }

    override fun onApplicationEvent(event: JiraOnboardingStartedEvent) {
        logger.info { "Received Jira onboarding started event: ${event.startedBy} in tenant ${event.teamId}" }
        slackToJiraRepository.get(event.teamId)?.let {
            slackToJiraOnboardingMessageService.alreadyConnected(it, event.startedBy, event.teamId)
            return
        }
        slackToJiraOnboardingMessageService.firstOnboarding(event)
    }
}
