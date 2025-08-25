package com.abistama.supporthero.infrastructure.jira.events

import com.abistama.supporthero.domain.slack.SlackUserId
import com.abistama.supporthero.infrastructure.jira.IssueResponse
import com.abistama.supporthero.infrastructure.repository.JiraCloudId
import org.springframework.context.ApplicationEvent

class JiraIssueCreatedEvent(
    source: Any,
    val jiraCloudId: JiraCloudId,
    val issue: IssueResponse,
    val sendCsat: Boolean,
    val reportedBy: SlackUserId,
) : ApplicationEvent(source)
