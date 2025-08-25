package com.abistama.supporthero.infrastructure.slack.events.adapter

import com.abistama.supporthero.domain.slack.SlackChannelId
import com.abistama.supporthero.domain.slack.events.SlackTs
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonIgnoreProperties(ignoreUnknown = true)
data class SlackBlockActionsEvent(
    val container: Container,
    @JsonProperty("trigger_id")
    val triggerId: String,
    val team: Team,
    val actions: List<Action>,
    @JsonProperty("response_url")
    val responseUrl: String? = null,
) : SlackEventWrapper {
    override val type = EventWrapperType.BLOCK_ACTIONS

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type",
    )
    @JsonSubTypes(
        JsonSubTypes.Type(value = MessageContainer::class, name = "message"),
        JsonSubTypes.Type(value = ViewContainer::class, name = "view"),
    )
    interface Container {
        val type: String
    }

    data class MessageContainer(
        @JsonProperty("message_ts")
        val messageTs: SlackTs,
        @JsonProperty("channel_id")
        val channelId: SlackChannelId,
        @JsonProperty("is_ephemeral")
        val isEphemeral: Boolean,
    ) : Container {
        override val type = "message"
    }

    data class ViewContainer(
        @JsonProperty("view_id")
        val viewId: String,
    ) : Container {
        override val type = "view"
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Action(
        val type: String,
        @JsonProperty("action_id")
        val actionId: String,
        @JsonProperty("block_id")
        val blockId: String,
        @JsonProperty("action_ts")
        val actionTs: SlackTs,
        val text: Text? = null,
        val value: String? = null,
        val style: String? = null,
    )

    data class Text(
        val type: String,
        val text: String,
        val emoji: Boolean,
    )
}
