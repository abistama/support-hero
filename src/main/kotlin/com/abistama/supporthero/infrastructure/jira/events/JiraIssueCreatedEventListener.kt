package com.abistama.supporthero.infrastructure.jira.events

import com.abistama.supporthero.infrastructure.jira.JiraAutoRefreshTokenClient
import com.abistama.supporthero.infrastructure.jira.repository.JiraTicketTrackingRepository
import mu.KLogging
import org.springframework.context.ApplicationListener

class JiraIssueCreatedEventListener(
    private val jiraAutoRefreshTokenClient: JiraAutoRefreshTokenClient,
    private val jiraTicketTrackingRepository: JiraTicketTrackingRepository,
) : ApplicationListener<JiraIssueCreatedEvent> {
    companion object : KLogging()

    override fun onApplicationEvent(event: JiraIssueCreatedEvent) {
        logger.info { "Received Jira issue created event: ${event.issue.key} in tenant ${event.jiraCloudId.value}" }

        jiraAutoRefreshTokenClient
            .getIssue(
                event.jiraCloudId,
                event.issue.key,
            ).fold(
                {
                    logger.error { "Error getting Jira issue ${event.issue.key} | ${it.message}" }
                },
                {
                    jiraTicketTrackingRepository.add(event, it)
                },
            )
    }
}
