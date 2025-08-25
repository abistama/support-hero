package com.abistama.supporthero.domain.slack.events

import com.fasterxml.jackson.annotation.JsonCreator

data class SlackTs(val ts: String) {
    init {
        require(ts.matches(Regex("[0-9]{10}\\.[0-9]{6}"))) { "Invalid SlackTs format" }
    }

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(ts: String): SlackTs {
            return SlackTs(ts)
        }
    }
}
