package com.abistama.supporthero.application.slack

import arrow.core.Either
import arrow.core.flatMap
import com.abistama.supporthero.domain.slack.RefreshToken
import com.abistama.supporthero.domain.slack.SlackAuthorizable
import com.abistama.supporthero.domain.slack.SlackGetConversations
import com.abistama.supporthero.domain.slack.SlackGetGroups
import com.abistama.supporthero.domain.slack.SlackGetUserProfile
import com.abistama.supporthero.domain.slack.SlackLookupByEmail
import com.abistama.supporthero.domain.slack.SlackPostBlocksMessage
import com.abistama.supporthero.domain.slack.SlackPostMessage
import com.abistama.supporthero.domain.slack.SlackReplies
import com.abistama.supporthero.domain.slack.SlackUpdateMessage
import com.abistama.supporthero.domain.slack.SlackUserGroupId
import com.abistama.supporthero.domain.slack.SlackUserId
import com.abistama.supporthero.domain.slack.SlackView
import com.abistama.supporthero.domain.slack.toSlackCredentials
import com.abistama.supporthero.domain.slack.updateSlackCredentials
import com.abistama.supporthero.infrastructure.repository.SlackTenantRepository
import com.abistama.supporthero.infrastructure.slack.oauth.SlackOAuthProperties
import com.slack.api.methods.response.chat.ChatPostEphemeralResponse
import com.slack.api.methods.response.chat.ChatPostMessageResponse
import com.slack.api.methods.response.users.UsersLookupByEmailResponse
import com.slack.api.methods.response.users.profile.UsersProfileGetResponse
import com.slack.api.methods.response.views.ViewsOpenResponse
import mu.KLogging
import java.time.Clock
import java.time.LocalDateTime

class SlackAutoRefreshTokenClient(
    private val slackTenantRepository: SlackTenantRepository,
    private val slackOAuthProperties: SlackOAuthProperties,
    private val slackClient: SlackClient,
    private val clock: Clock,
) : SlackClient by slackClient {
    companion object : KLogging()

    override fun postMessage(message: SlackPostMessage) =
        withToken<SlackPostMessage, ChatPostMessageResponse>(message) { slackClient.postMessage(it) }

    override fun updateMessage(updateMessage: SlackUpdateMessage) = withToken(updateMessage) { slackClient.updateMessage(it) }

    override fun postBlocks(blocks: SlackPostBlocksMessage) =
        withToken<SlackPostBlocksMessage, ChatPostMessageResponse>(blocks) { slackClient.postBlocks(it) }

    override fun postEphemeralMessage(slackPostMessage: SlackPostMessage): Either<SlackAPIError, ChatPostEphemeralResponse> =
        withToken<SlackPostMessage, ChatPostEphemeralResponse>(slackPostMessage) {
            slackClient.postEphemeralMessage(
                it,
            )
        }

    override fun postEphemeralMessage(blocks: SlackPostBlocksMessage) =
        withToken<SlackPostBlocksMessage, ChatPostEphemeralResponse>(blocks) {
            slackClient.postEphemeralMessage(
                it,
            )
        }

    override fun getUserGroups(getGroups: SlackGetGroups): Either<SlackAPIError, Map<SlackUserId, Collection<SlackUserGroupId>>> =
        withToken<SlackGetGroups, Map<SlackUserId, Collection<SlackUserGroupId>>>(getGroups) {
            slackClient.getUserGroups(
                it,
            )
        }

    override fun getConversations(conversations: SlackGetConversations): Either<SlackAPIError, SlackReplies> =
        withToken<SlackGetConversations, SlackReplies>(conversations) {
            slackClient.getConversations(it)
        }

    override fun getUserByEmail(lookupByEmail: SlackLookupByEmail): Either<SlackAPIError, UsersLookupByEmailResponse> =
        withToken<SlackLookupByEmail, UsersLookupByEmailResponse>(lookupByEmail) {
            slackClient.getUserByEmail(it)
        }

    override fun getUserProfile(profile: SlackGetUserProfile): Either<SlackAPIError, UsersProfileGetResponse> =
        withToken<SlackGetUserProfile, UsersProfileGetResponse>(profile) {
            return slackClient.getUserProfile(it)
        }

    override fun openView(view: SlackView): Either<SlackAPIError, ViewsOpenResponse> {
        return withToken<SlackView, ViewsOpenResponse>(view) {
            return slackClient.openView(it)
        }
    }

    private inline fun <reified T : SlackAuthorizable, U> withToken(
        sa: T,
        block: (sa: T) -> Either<SlackAPIError, U>,
    ): Either<SlackAPIError, U> {
        logger.info { "Checking token for team ${sa.slackTeamId}" }
        return slackTenantRepository
            .get(sa.slackTeamId)
            ?.let { credentials ->
                if (credentials.expires.isBefore(LocalDateTime.now(clock))) {
                    logger.info { "Refreshing token for team ${sa.slackTeamId}" }
                    slackClient
                        .refreshToken(
                            RefreshToken(
                                slackOAuthProperties.clientId,
                                slackOAuthProperties.clientSecret,
                                credentials.refreshToken,
                            ),
                        ).flatMap { response ->
                            val updatedCredentials = response.toSlackCredentials(clock)
                            slackTenantRepository.updateToken(
                                sa.slackTeamId,
                                updatedCredentials,
                            )
                            block(sa.updateSlackCredentials(updatedCredentials) as T)
                        }
                } else {
                    logger.info { "Token for team ${sa.slackTeamId} is still valid" }
                    block(sa.updateSlackCredentials(credentials) as T)
                }
            } ?: Either.Left(SlackAPIError("No token found for team ${sa.slackTeamId}"))
    }
}
