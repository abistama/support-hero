package com.abistama.supporthero.application.slackJira

import com.abistama.supporthero.application.slack.SlackClient
import com.abistama.supporthero.domain.slack.JiraBoardConfigurationIdTrigger
import com.abistama.supporthero.domain.slack.SlackUpdateMessage
import com.abistama.supporthero.infrastructure.repository.SlackToJiraConfigurationRepository
import mu.KLogging

class ProcessDeleteReactionForJira(
    private val slackToJiraConfigurationRepository: SlackToJiraConfigurationRepository,
    private val slackAutoRefreshTokenClient: SlackClient,
) : KLogging() {
    fun handle(trigger: JiraBoardConfigurationIdTrigger) {
        logger.info { "Processing delete reaction for Jira action..." }
        slackToJiraConfigurationRepository.delete(trigger.configurationId)
        slackAutoRefreshTokenClient.updateMessage(
            SlackUpdateMessage(
                trigger.container.channelId,
                trigger.container.messageTs,
                "Configuration deleted successfully",
                trigger.team.id,
            ),
        )
    }
}
