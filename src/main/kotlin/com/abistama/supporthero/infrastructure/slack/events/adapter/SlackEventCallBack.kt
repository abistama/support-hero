package com.abistama.supporthero.infrastructure.slack.events.adapter

import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.events.SlackEvent
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class SlackEventCallBack(
    val event: SlackEvent,
    @JsonProperty("team_id") val teamId: SlackTeamId,
) : SlackEventWrapper {
    override val type = EventWrapperType.EVENT_CALLBACK
}
