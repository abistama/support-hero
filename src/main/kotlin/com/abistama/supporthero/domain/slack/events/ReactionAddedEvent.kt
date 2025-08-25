package com.abistama.supporthero.domain.slack.events

import com.abistama.supporthero.domain.slack.SlackUserId
import com.fasterxml.jackson.annotation.JsonProperty

data class ReactionAddedEvent(
    val user: SlackUserId,
    val reaction: String,
    @JsonProperty("item_user")
    val itemUser: SlackUserId,
    val item: Item,
    @JsonProperty("event_ts")
    val eventTs: SlackTs,
) : SlackEvent {
    override val type = EventType.REACTION_ADDED
}
