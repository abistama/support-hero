package com.abistama.supporthero.application.slackJira

import com.abistama.supporthero.application.slack.SlackClient
import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.SlackUpdateMessage
import com.abistama.supporthero.infrastructure.jira.repository.JiraCsatRepository
import com.abistama.supporthero.infrastructure.slack.events.adapter.SlackBlockActionsEvent
import mu.KLogging

class ProcessCsatAction(
    private val jiraCsatRepository: JiraCsatRepository,
    private val autoRefreshTokenClient: SlackClient,
) : KLogging() {
    fun handle(
        teamId: SlackTeamId,
        container: SlackBlockActionsEvent.MessageContainer,
        action: CSatAction,
    ) {
        logger.info { "Processing CSAT action..." }
        jiraCsatRepository.updateCsatValue(
            action.jiraCsatId,
            action.value,
        )
        autoRefreshTokenClient
            .updateMessage(
                SlackUpdateMessage(
                    container.channelId,
                    container.messageTs,
                    "Thank you for your feedback!",
                    teamId,
                ),
            ).fold(
                { error ->
                    logger.error { "Could not update message due to ${error.message}" }
                },
                {
                    logger.info { "Message updated" }
                },
            )
    }
}
