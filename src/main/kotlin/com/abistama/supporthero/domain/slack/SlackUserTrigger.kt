package com.abistama.supporthero.domain.slack

import com.abistama.supporthero.infrastructure.slack.events.adapter.Team
import com.abistama.supporthero.infrastructure.slack.events.adapter.User

data class SlackUserTrigger(
    val triggerId: String,
    val team: Team,
    val user: User,
)
