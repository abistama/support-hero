package com.abistama.supporthero.application.slack.events

import com.abistama.supporthero.domain.slack.SlackTeamId
import org.springframework.context.ApplicationEvent

class SlackTeamProcessedEvent(
    source: Any,
    val teamId: SlackTeamId,
    val domain: SlackDomain,
) : ApplicationEvent(source)

@JvmInline
value class SlackDomain(
    val value: String,
)
