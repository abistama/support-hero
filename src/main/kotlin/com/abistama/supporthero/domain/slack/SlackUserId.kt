package com.abistama.supporthero.domain.slack

import com.fasterxml.jackson.annotation.JsonCreator

data class SlackUserId(
    val id: String,
) {
    init {
        require(id.matches(Regex("[UW][0-9A-Z]{10}"))) { "Invalid SlackUserId format" }
    }

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(id: String): SlackUserId = SlackUserId(id)
    }
}
