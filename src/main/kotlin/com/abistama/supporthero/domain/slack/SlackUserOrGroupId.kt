package com.abistama.supporthero.domain.slack

import com.fasterxml.jackson.annotation.JsonCreator

data class SlackUserOrGroupId(
    val id: String,
) {
    init {
        require(id.matches(Regex("[UWS][0-9A-Z]{10}"))) { "Invalid Slack User or Slack Group format" }
    }

    fun toUserId(): SlackUserId = SlackUserId(id)

    fun toUserGroupId(): SlackUserGroupId = SlackUserGroupId(id)

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(id: String): SlackUserOrGroupId = SlackUserOrGroupId(id)
    }
}
