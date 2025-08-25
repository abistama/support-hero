package com.abistama.supporthero.domain.slack

import com.abistama.supporthero.infrastructure.slack.events.adapter.Team
import java.util.UUID

data class SlackToJiraTenantTrigger(
    val triggerId: String,
    val team: Team,
    val slackToJiraTenant: UUID,
)
