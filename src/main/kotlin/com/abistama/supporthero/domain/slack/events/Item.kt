package com.abistama.supporthero.domain.slack.events

import com.abistama.supporthero.domain.slack.SlackChannelId

data class Item(
    val type: String,
    val channel: SlackChannelId,
    val ts: SlackTs,
)
