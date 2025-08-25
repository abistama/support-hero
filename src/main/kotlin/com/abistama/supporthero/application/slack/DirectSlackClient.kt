package com.abistama.supporthero.application.slack

import arrow.core.Either
import com.abistama.supporthero.domain.slack.RefreshToken
import com.abistama.supporthero.domain.slack.RequestToken
import com.abistama.supporthero.domain.slack.SlackGetConversations
import com.abistama.supporthero.domain.slack.SlackGetGroups
import com.abistama.supporthero.domain.slack.SlackGetUserProfile
import com.abistama.supporthero.domain.slack.SlackLookupByEmail
import com.abistama.supporthero.domain.slack.SlackMessage
import com.abistama.supporthero.domain.slack.SlackPostBlocksMessage
import com.abistama.supporthero.domain.slack.SlackPostMessage
import com.abistama.supporthero.domain.slack.SlackReplies
import com.abistama.supporthero.domain.slack.SlackUpdateMessage
import com.abistama.supporthero.domain.slack.SlackUserGroupId
import com.abistama.supporthero.domain.slack.SlackUserId
import com.abistama.supporthero.domain.slack.SlackView
import com.abistama.supporthero.domain.slack.events.SlackTs
import com.slack.api.Slack
import com.slack.api.methods.response.chat.ChatPostEphemeralResponse
import com.slack.api.methods.response.chat.ChatPostMessageResponse
import com.slack.api.methods.response.chat.ChatUpdateResponse
import com.slack.api.methods.response.oauth.OAuthV2AccessResponse
import com.slack.api.methods.response.users.UsersLookupByEmailResponse
import com.slack.api.methods.response.users.profile.UsersProfileGetResponse
import com.slack.api.methods.response.views.ViewsOpenResponse
import mu.KLogging

class DirectSlackClient(
    private val slack: Slack,
) : SlackClient {
    companion object : KLogging()

    override fun postMessage(message: SlackPostMessage): Either<SlackAPIError, ChatPostMessageResponse> {
        return try {
            val response =
                slack.methods().chatPostMessage {
                    it
                        .channel(message.to.channel.id)
                        .threadTs(message.to.thread?.ts)
                        .token(message.slackCredentials?.accessToken)
                        .text(message.text)
                }
            return if (!response.isOk) {
                logger.error { "Error posting message: ${response.error}" }
                Either.Left(SlackAPIError(response.error))
            } else {
                Either.Right(response)
            }
        } catch (e: Exception) {
            Either.Left(e.toSlackAPIError())
        }
    }

    override fun updateMessage(updateMessage: SlackUpdateMessage): Either<SlackAPIError, ChatUpdateResponse> {
        logger.info { "Updating message in channel ${updateMessage.channel.id} ${updateMessage.ts}" }
        return try {
            val response =
                slack.methods().chatUpdate {
                    it
                        .token(updateMessage.slackCredentials?.accessToken)
                        .channel(updateMessage.channel.id)
                        .ts(updateMessage.ts.ts)
                        .text(updateMessage.text)
                }
            return if (!response.isOk) {
                logger.error { "Error updating message: ${response.error}" }
                Either.Left(SlackAPIError(response.error))
            } else {
                Either.Right(response)
            }
        } catch (e: Exception) {
            Either.Left(e.toSlackAPIError())
        }
    }

    override fun postBlocks(blocks: SlackPostBlocksMessage): Either<SlackAPIError, ChatPostMessageResponse> {
        logger.info { "Posting blocks to channel ${blocks.userId.id} ${blocks.slackCredentials?.accessToken}" }
        return try {
            val response =
                slack.methods().chatPostMessage {
                    it
                        .channel(blocks.userId.id)
                        .token(blocks.slackCredentials?.accessToken)
                        .blocks(blocks.blocks)
                }
            return if (!response.isOk) {
                logger.error { "Error posting message: ${response.error}" }
                Either.Left(SlackAPIError(response.error))
            } else {
                Either.Right(response)
            }
        } catch (e: Exception) {
            Either.Left(e.toSlackAPIError())
        }
    }

    override fun postEphemeralMessage(slackPostMessage: SlackPostMessage): Either<SlackAPIError, ChatPostEphemeralResponse> {
        return try {
            val response =
                slack.methods().chatPostEphemeral {
                    it
                        .channel(slackPostMessage.to.channel.id)
                        .threadTs(slackPostMessage.to.thread?.ts)
                        .text(slackPostMessage.text)
                        .user(slackPostMessage.to.userId?.id)
                        .token(slackPostMessage.slackCredentials?.accessToken)
                }
            return if (!response.isOk) {
                logger.error { "Error posting ephemeral message: ${response.error}" }
                Either.Left(SlackAPIError(response.error))
            } else {
                Either.Right(response)
            }
        } catch (e: Exception) {
            Either.Left(e.toSlackAPIError())
        }
    }

    override fun postEphemeralMessage(blocks: SlackPostBlocksMessage): Either<SlackAPIError, ChatPostEphemeralResponse> {
        return try {
            val response =
                slack.methods().chatPostEphemeral {
                    it
                        .channel(blocks.channel?.id)
                        .threadTs(blocks.threadTs?.ts)
                        .user(blocks.userId.id)
                        .blocks(blocks.blocks)
                        .token(blocks.slackCredentials?.accessToken)
                }
            return if (!response.isOk) {
                logger.error { "Error posting ephemeral message: ${response.error}" }
                Either.Left(SlackAPIError(response.error))
            } else {
                Either.Right(response)
            }
        } catch (e: Exception) {
            Either.Left(e.toSlackAPIError())
        }
    }

    override fun getConversations(conversations: SlackGetConversations): Either<SlackAPIError, SlackReplies> {
        kotlin
            .runCatching {
                slack.methods().conversationsReplies {
                    it
                        .token(conversations.slackCredentials?.accessToken)
                        .channel(conversations.channelId.id)
                        .ts(conversations.thread.ts)
                }
            }.fold(
                { response ->
                    return if (response.isOk) {
                        Either.Right(
                            SlackReplies(
                                response.messages.map {
                                    SlackMessage(
                                        SlackTs(it.ts),
                                        it.text,
                                        it.user,
                                        it.threadTs?.let { ts -> SlackTs(ts) },
                                    )
                                },
                                conversations.channelId,
                                conversations.thread,
                            ),
                        )
                    } else {
                        logger.error { "Error in Response getting conversations: ${response.error}" }
                        Either.Left(SlackAPIError(response.error))
                    }
                },
                {
                    logger.error(it) { "Exception getting conversations: ${it.message}" }
                    return Either.Left(it.toSlackAPIError())
                },
            )
    }

    override fun getUserGroups(getGroups: SlackGetGroups): Either<SlackAPIError, Map<SlackUserId, Collection<SlackUserGroupId>>> {
        kotlin
            .runCatching {
                slack.methods().usergroupsList {
                    it
                        .token(getGroups.slackCredentials?.accessToken)
                        .includeUsers(true)
                }
            }.fold(
                { response ->
                    return if (response.isOk) {
                        val userGroups =
                            response.usergroups
                                .flatMap { userGroup ->
                                    userGroup.users.map { user ->
                                        Pair(
                                            SlackUserId(user),
                                            SlackUserGroupId(userGroup.id),
                                        )
                                    }
                                }.groupBy({ it.first }, { it.second })
                        Either.Right(userGroups)
                    } else {
                        logger.error { "Error getting user groups: ${response.error}" }
                        Either.Left(SlackAPIError(response.error))
                    }
                },
                {
                    logger.error(it) { "Error getting user groups" }
                    return Either.Left(it.toSlackAPIError())
                },
            )
    }

    override fun getUserByEmail(lookupByEmail: SlackLookupByEmail): Either<SlackAPIError, UsersLookupByEmailResponse> {
        kotlin
            .runCatching {
                slack.methods().usersLookupByEmail {
                    it.token(lookupByEmail.slackCredentials?.accessToken).email(lookupByEmail.email)
                }
            }.fold(
                { response ->
                    return if (response.isOk) {
                        Either.Right(response)
                    } else {
                        logger.error { "Error getting user by email: ${response.error}" }
                        Either.Left(SlackAPIError(response.error))
                    }
                },
                { exception ->
                    logger.error(exception) { "Error getting user by email" }
                    return Either.Left(exception.toSlackAPIError())
                },
            )
    }

    override fun getUserProfile(profile: SlackGetUserProfile): Either<SlackAPIError, UsersProfileGetResponse> {
        kotlin
            .runCatching {
                slack.methods().usersProfileGet {
                    it.token(profile.slackCredentials?.accessToken).user(profile.userId.id)
                }
            }.fold(
                { response ->
                    return if (response.isOk) {
                        Either.Right(response)
                    } else {
                        logger.error { "Error getting user profile: ${response.error}" }
                        Either.Left(SlackAPIError(response.error))
                    }
                },
                { exception ->
                    logger.error(exception) { "Error getting user profile" }
                    return Either.Left(exception.toSlackAPIError())
                },
            )
    }

    override fun openView(view: SlackView): Either<SlackAPIError, ViewsOpenResponse> {
        kotlin
            .runCatching {
                slack
                    .methods()
                    .viewsOpen {
                        it
                            .token(view.slackCredentials?.accessToken)
                            .triggerId(view.triggerId)
                            .viewAsString(view.view)
                    }
            }.fold(
                { response ->
                    return if (response.isOk) {
                        Either.Right(response)
                    } else {
                        response.responseMetadata.messages.forEach { logger.error { it } }
                        logger.error { "Error opening view: ${response.error}" }
                        Either.Left(SlackAPIError(response.error))
                    }
                },
                { exception ->
                    logger.error(exception) { "Error opening view" }
                    return Either.Left(exception.toSlackAPIError())
                },
            )
    }

    override fun getOAuthToken(requestToken: RequestToken): Either<SlackAPIError, OAuthV2AccessResponse> {
        kotlin
            .runCatching {
                slack.methods().oauthV2Access {
                    it
                        .clientId(requestToken.clientId.value)
                        .clientSecret(requestToken.clientSecret.value)
                        .code(requestToken.code.value)
                        .redirectUri(requestToken.redirectUri.toString())
                }
            }.fold(
                { return if (it.isOk) Either.Right(it) else Either.Left(SlackAPIError(it.error)) },
                { return Either.Left(it.toSlackAPIError()) },
            )
    }

    override fun refreshToken(refreshToken: RefreshToken): Either<SlackAPIError, OAuthV2AccessResponse> =
        try {
            val response =
                slack.methods().oauthV2Access {
                    it
                        .clientId(refreshToken.clientId)
                        .grantType("refresh_token")
                        .clientSecret(refreshToken.clientSecret)
                        .refreshToken(refreshToken.refreshToken)
                }
            if (!response.isOk) {
                Either.Left(SlackAPIError(response.error))
            } else {
                Either.Right(response)
            }
        } catch (e: Exception) {
            Either.Left(e.toSlackAPIError())
        }
}
