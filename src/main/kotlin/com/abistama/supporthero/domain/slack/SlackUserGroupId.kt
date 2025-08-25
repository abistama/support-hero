package com.abistama.supporthero.domain.slack

import com.fasterxml.jackson.annotation.JsonCreator

data class SlackUserGroupId(val id: String) {
    init {
        require(id.matches(Regex("S[0-9A-Z]{10}"))) { "Invalid SlackUserGroupId format" }
    }

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(id: String): SlackUserGroupId {
            return SlackUserGroupId(id)
        }
    }
}
