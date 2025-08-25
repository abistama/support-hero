package com.abistama.supporthero.application.slackJira.events

import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.SlackUserId
import org.springframework.context.ApplicationEvent

class JiraOnboardingStartedEvent(
    source: Any,
    val startedBy: SlackUserId,
    val teamId: SlackTeamId,
) : ApplicationEvent(source)
