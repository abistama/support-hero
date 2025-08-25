package com.abistama.supporthero.infrastructure.slack.events.adapter

import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.SlackUserId
import com.abistama.supporthero.infrastructure.slack.events.SlackViewSubmissionEvent

data class AskForSupportViewSubmission internal constructor(
    val creator: SlackUserId,
    val teamId: SlackTeamId,
    val message: String,
) {
    companion object {
        fun SlackViewSubmissionEvent.toAskForSupport(): AskForSupportViewSubmission {
            val message =
                this.view.state
                    .get<SlackViewSubmissionEvent.PlainTextInput>("support_request")
                    .value
                    .orEmpty()

            return AskForSupportViewSubmission(this.user.id, this.team.id, message)
        }
    }
}
