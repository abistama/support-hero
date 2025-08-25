package com.abistama.supporthero.infrastructure.slack.events.adapter

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class SlackShortcutEvent(
    @JsonProperty("trigger_id")
    val triggerId: String,
    @JsonProperty("callback_id")
    val callbackId: CallbackId,
    val team: Team,
    val user: User,
) : SlackEventWrapper {
    override val type = EventWrapperType.SHORTCUT
}

enum class CallbackId(
    val id: String,
) {
    CONFIGURE_JIRA_REACTION("configure_jira_reaction"),
    GET_CONFIGURED_JIRA_REACTIONS("get_configured_jira_reactions"),
    UNKNOWN("unknown"),
    ;

    override fun toString(): String = id

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: String): CallbackId = CallbackId.entries.firstOrNull { it.id == value } ?: UNKNOWN
    }
}
