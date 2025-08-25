package com.abistama.supporthero.application.slack.events

import com.abistama.supporthero.infrastructure.repository.SlackTenantRepository
import mu.KLogging
import org.springframework.context.ApplicationListener

class SlackTeamProcessedEventListener(
    private val slackTenantRepository: SlackTenantRepository,
) : ApplicationListener<SlackTeamProcessedEvent> {
    companion object : KLogging()

    override fun onApplicationEvent(event: SlackTeamProcessedEvent) {
        logger.info { "Updating the domain (${event.domain}) for team ${event.teamId}" }
        slackTenantRepository.updateDomain(event)
    }
}
