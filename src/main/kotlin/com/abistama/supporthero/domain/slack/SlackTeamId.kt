package com.abistama.supporthero.domain.slack

import com.fasterxml.jackson.annotation.JsonCreator

data class SlackTeamId(val id: String) {
    init {
        validateTeamId(id)
    }

    private fun validateTeamId(teamId: String) {
        if (!"^(T)[a-zA-Z\\d]{10}".toRegex().matches(teamId)) {
            throw SlackException("Slack Team Id isn't valid")
        }
    }

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(id: String): SlackTeamId {
            return SlackTeamId(id)
        }
    }
}
