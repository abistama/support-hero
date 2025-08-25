package com.abistama.supporthero.domain.slackToJira

import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.SlackUserId
import com.abistama.supporthero.domain.slack.events.SlackTs

data class JiraOnboardingInProgress(
    val startedBy: SlackUserId,
    val teamId: SlackTeamId,
    val messageToUpdateTs: SlackTs,
)
