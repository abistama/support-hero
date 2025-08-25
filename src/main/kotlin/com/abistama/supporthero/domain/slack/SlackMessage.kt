package com.abistama.supporthero.domain.slack

import com.abistama.supporthero.domain.slack.events.SlackTs

data class SlackMessage(
    val ts: SlackTs,
    val text: String,
    val username: String,
    val parentTs: SlackTs? = null,
)
