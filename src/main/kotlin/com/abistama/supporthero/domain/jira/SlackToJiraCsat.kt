package com.abistama.supporthero.domain.jira

import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.SlackUserId
import java.util.UUID

data class SlackToJiraCsat(
    val id: UUID,
    val sendTo: SlackUserId,
    val slackTeamId: SlackTeamId,
    val ticketKey: String,
)
