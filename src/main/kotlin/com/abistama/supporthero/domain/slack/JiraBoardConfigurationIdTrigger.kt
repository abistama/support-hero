package com.abistama.supporthero.domain.slack

import com.abistama.supporthero.infrastructure.slack.events.adapter.SlackBlockActionsEvent
import com.abistama.supporthero.infrastructure.slack.events.adapter.Team
import java.util.UUID

data class JiraBoardConfigurationIdTrigger(
    val triggerId: String,
    val team: Team,
    val configurationId: UUID,
    val container: SlackBlockActionsEvent.MessageContainer,
)
