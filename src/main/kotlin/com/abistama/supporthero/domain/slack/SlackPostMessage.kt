package com.abistama.supporthero.domain.slack

import com.abistama.supporthero.domain.slack.events.SlackTs
import com.slack.api.model.block.LayoutBlock

sealed interface SlackAuthorizable {
    val slackTeamId: SlackTeamId
    val slackCredentials: SlackCredentials?
}

data class SlackPostMessage(
    val to: SlackPostTo,
    val text: String,
    override val slackTeamId: SlackTeamId,
    override val slackCredentials: SlackCredentials? = null,
) : SlackAuthorizable

data class SlackUpdateMessage(
    val channel: SlackChannelId,
    val ts: SlackTs,
    val text: String,
    override val slackTeamId: SlackTeamId,
    override val slackCredentials: SlackCredentials? = null,
) : SlackAuthorizable

data class SlackPostTo(
    val channel: SlackChannelId,
    val thread: SlackTs? = null,
    val userId: SlackUserId? = null,
)

data class SlackPostBlocksMessage(
    val userId: SlackUserId,
    val blocks: List<LayoutBlock>,
    override val slackTeamId: SlackTeamId,
    val channel: SlackChannelId? = null,
    val threadTs: SlackTs? = null,
    override val slackCredentials: SlackCredentials? = null,
) : SlackAuthorizable

data class SlackGetGroups(
    override val slackTeamId: SlackTeamId,
    override val slackCredentials: SlackCredentials? = null,
) : SlackAuthorizable

data class SlackGetConversations(
    val thread: SlackTs,
    val channelId: SlackChannelId,
    override val slackTeamId: SlackTeamId,
    override val slackCredentials: SlackCredentials? = null,
) : SlackAuthorizable

data class SlackLookupByEmail(
    val email: String,
    override val slackTeamId: SlackTeamId,
    override val slackCredentials: SlackCredentials? = null,
) : SlackAuthorizable

data class SlackView(
    val view: String,
    val triggerId: String,
    override val slackTeamId: SlackTeamId,
    override val slackCredentials: SlackCredentials? = null,
) : SlackAuthorizable

data class SlackGetUserProfile(
    val userId: SlackUserId,
    override val slackTeamId: SlackTeamId,
    override val slackCredentials: SlackCredentials? = null,
) : SlackAuthorizable

fun SlackAuthorizable.updateSlackCredentials(newCredentials: SlackCredentials?): SlackAuthorizable =
    when (this) {
        is SlackPostMessage -> this.copy(slackCredentials = newCredentials)
        is SlackUpdateMessage -> this.copy(slackCredentials = newCredentials)
        is SlackPostBlocksMessage -> this.copy(slackCredentials = newCredentials)
        is SlackGetGroups -> this.copy(slackCredentials = newCredentials)
        is SlackGetConversations -> this.copy(slackCredentials = newCredentials)
        is SlackLookupByEmail -> this.copy(slackCredentials = newCredentials)
        is SlackView -> this.copy(slackCredentials = newCredentials)
        is SlackGetUserProfile -> this.copy(slackCredentials = newCredentials)
    }
