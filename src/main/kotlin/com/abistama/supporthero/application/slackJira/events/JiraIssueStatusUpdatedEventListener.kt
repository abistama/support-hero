package com.abistama.supporthero.application.slackJira.events

import com.abistama.supporthero.infrastructure.jira.repository.JiraCsatRepository
import mu.KLogging
import org.springframework.context.ApplicationListener
import java.time.Duration

class JiraIssueStatusUpdatedEventListener(
    private val jiraCsatRepository: JiraCsatRepository,
) : ApplicationListener<JiraIssueStatusUpdatedEvent> {
    companion object {
        private val logger = KLogging().logger()
        private const val DEFAULT_NUMBER_OF_REMINDERS = 3
        private val DEFAULT_REMINDER_INTERVAL = Duration.ofMinutes(1)
    }

    override fun onApplicationEvent(event: JiraIssueStatusUpdatedEvent) {
        logger.info { "Received Jira issue status updated event: ${event.issue.key} in tenant ${event.jiraCloudId.value}" }

        if (event.issue.fields.status.name != "Done") {
            logger.info { "Issue ${event.issue.key} is not resolved, skipping CSAT request" }
            return
        }

        jiraCsatRepository.createCsat(
            event.jiraCloudId,
            event.reportedBy,
            event.issue.key,
            event.issue.fields.project.key,
            DEFAULT_NUMBER_OF_REMINDERS,
            DEFAULT_REMINDER_INTERVAL,
        )
    }
}
