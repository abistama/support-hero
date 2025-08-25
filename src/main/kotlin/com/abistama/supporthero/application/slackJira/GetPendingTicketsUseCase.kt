package com.abistama.supporthero.application.slackJira

import com.abistama.supporthero.application.slackJira.events.JiraIssueStatusUpdatedEvent
import com.abistama.supporthero.infrastructure.jira.JiraAutoRefreshTokenClient
import com.abistama.supporthero.infrastructure.jira.repository.JiraTicketTrackingRepository
import mu.KLogging
import org.springframework.context.ApplicationEventPublisher

class GetPendingTicketsUseCase(
    private val jiraTicketTrackingRepository: JiraTicketTrackingRepository,
    private val jiraAutoRefreshTokenClient: JiraAutoRefreshTokenClient,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    companion object : KLogging()

    fun execute() {
        jiraTicketTrackingRepository
            .getPending()
            .forEach { pendingTicket ->
                jiraAutoRefreshTokenClient
                    .getIssue(
                        pendingTicket.jiraCloudId,
                        pendingTicket.jiraIssueResponse.key,
                    ).fold(
                        {
                            logger.error {
                                "Could not get issue ${pendingTicket.jiraIssueResponse.key} " +
                                    "(${pendingTicket.jiraCloudId.value}) from the Jira API "
                            }
                        },
                        {
                            logger.info {
                                "Got issue ${pendingTicket.jiraIssueResponse.key} " +
                                    "(${pendingTicket.jiraCloudId.value}) from the Jira API"
                            }
                            if (it.fields.status.name != pendingTicket.jiraIssueResponse.fields.status.name) {
                                logger.info {
                                    "Updating status of issue ${pendingTicket.jiraIssueResponse.key} " +
                                        "(${pendingTicket.jiraCloudId.value}) from " +
                                        "${pendingTicket.jiraIssueResponse.fields.status.name} to " +
                                        it.fields.status.name
                                }
                                jiraTicketTrackingRepository.updateStatus(
                                    pendingTicket.id,
                                    it.fields.status.name,
                                )
                                applicationEventPublisher.publishEvent(
                                    JiraIssueStatusUpdatedEvent(
                                        this,
                                        pendingTicket.jiraCloudId,
                                        pendingTicket.slackTeamId,
                                        it,
                                        pendingTicket.reportedBy,
                                    ),
                                )
                            } else {
                                logger.info {
                                    "Status of issue ${pendingTicket.jiraIssueResponse.key} " +
                                        "(${pendingTicket.jiraCloudId.value}) is already up to date"
                                }
                            }
                        },
                    )
            }
    }
}
