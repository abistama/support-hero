package com.abistama.supporthero.domain.slack

import com.fasterxml.jackson.annotation.JsonCreator

data class SlackChannelId(
    val id: String,
) {
    init {
        require(id.matches(Regex("[CD][0-9A-Z]{10}"))) { "Invalid SlackChannelId format" }
    }

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(id: String): SlackChannelId = SlackChannelId(id)
    }
}
