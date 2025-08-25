package com.abistama.supporthero.domain.slack

import com.abistama.supporthero.domain.slack.events.SlackTs

data class SlackReplies(
    val messages: List<SlackMessage>,
    val slackChannelId: SlackChannelId,
    val ts: SlackTs,
)
