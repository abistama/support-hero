package com.abistama.supporthero.application.slackJira.events

import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.SlackUserId
import com.abistama.supporthero.infrastructure.jira.events.JiraIssueResponse
import com.abistama.supporthero.infrastructure.repository.JiraCloudId
import org.springframework.context.ApplicationEvent

class JiraIssueStatusUpdatedEvent(
    source: Any,
    val jiraCloudId: JiraCloudId,
    val slackTeamId: SlackTeamId,
    val issue: JiraIssueResponse,
    val reportedBy: SlackUserId,
) : ApplicationEvent(source)
