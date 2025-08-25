package com.abistama.supporthero.application.slackJira

import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.SlackUserId
import com.abistama.supporthero.infrastructure.jira.events.JiraIssueResponse
import com.abistama.supporthero.infrastructure.repository.JiraCloudId
import java.util.*

@JvmInline
value class SlackToJiraPendingTickedId(
    val value: UUID,
)

data class SlackToJiraPendingTicket(
    val jiraCloudId: JiraCloudId,
    val slackTeamId: SlackTeamId,
    val id: SlackToJiraPendingTickedId,
    val jiraIssueResponse: JiraIssueResponse,
    val reportedBy: SlackUserId,
)
