package com.abistama.supporthero.application.slackJira

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.abistama.supporthero.infrastructure.slack.events.adapter.SlackBlockActionsEvent
import java.util.UUID

@JvmInline
value class ActionId(
    val id: String,
)

sealed interface Action {
    val actionId: ActionId
}

data class CSatAction(
    val jiraCsatId: UUID,
    val value: Int,
) : Action {
    override val actionId = ActionId(ACTION_ID)

    companion object {
        private const val ACTION_DELIMITER = "|"
        const val ACTION_ID = "csat"

        fun fromString(str: String): CSatAction {
            val split = str.split(ACTION_DELIMITER)
            if (split.size != 3) {
                throw IllegalArgumentException("Invalid action: $str")
            }
            if (split[0] != ACTION_ID) {
                throw IllegalArgumentException("Invalid actionId: $str")
            }
            return CSatAction(
                UUID.fromString(split[1]),
                split[2].toIntOrNull() ?: 0,
            )
        }
    }
}

data class ConfigureReactionForJiraAction(
    val slackToJiraTenant: UUID,
) : Action {
    override val actionId = ActionId(CONFIGURE_REACTION)

    companion object {
        const val CONFIGURE_REACTION = "configure_jira_reaction"
    }
}

data class DeleteReactionForJiraAction(
    val configurationId: UUID,
) : Action {
    override val actionId = ActionId(DELETE_REACTION)

    companion object {
        const val DELETE_REACTION = "delete_jira_reaction"
    }
}

data class AskForSupportAction(
    val slackToJiraTenant: UUID,
) : Action {
    override val actionId = ActionId(ASK_FOR_SUPPORT)

    companion object {
        const val ASK_FOR_SUPPORT = "ask_for_support"
    }
}

data class LinkJiraCloudAction(
    val value: String,
) : Action {
    override val actionId = ActionId(CONNECT_JIRA_CLOUD)

    companion object {
        const val CONNECT_JIRA_CLOUD = "connect_jira_cloud"
    }
}

fun SlackBlockActionsEvent.Action.toAction(): Option<Action> =
    when {
        this.actionId.startsWith(CSatAction.ACTION_ID) -> Some(CSatAction.fromString(this.actionId))
        this.actionId.startsWith(ConfigureReactionForJiraAction.CONFIGURE_REACTION) ->
            Some(
                ConfigureReactionForJiraAction(
                    UUID.fromString(this.value),
                ),
            )

        this.actionId.startsWith(AskForSupportAction.ASK_FOR_SUPPORT) ->
            Some(
                AskForSupportAction(
                    UUID.fromString(this.value),
                ),
            )

        this.actionId.startsWith(LinkJiraCloudAction.CONNECT_JIRA_CLOUD) -> Some(LinkJiraCloudAction(this.value.orEmpty()))
        this.actionId.startsWith(DeleteReactionForJiraAction.DELETE_REACTION) ->
            Some(
                DeleteReactionForJiraAction(
                    UUID.fromString(this.value),
                ),
            )
        else -> None
    }
