package com.abistama.supporthero.application.slack

import arrow.core.Either
import com.abistama.supporthero.domain.slack.RefreshToken
import com.abistama.supporthero.domain.slack.RequestToken
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
import com.slack.api.methods.response.chat.ChatPostEphemeralResponse
import com.slack.api.methods.response.chat.ChatPostMessageResponse
import com.slack.api.methods.response.chat.ChatUpdateResponse
import com.slack.api.methods.response.oauth.OAuthV2AccessResponse
import com.slack.api.methods.response.users.UsersLookupByEmailResponse
import com.slack.api.methods.response.users.profile.UsersProfileGetResponse
import com.slack.api.methods.response.views.ViewsOpenResponse

interface SlackClient {
    fun postMessage(message: SlackPostMessage): Either<SlackAPIError, ChatPostMessageResponse>

    fun updateMessage(updateMessage: SlackUpdateMessage): Either<SlackAPIError, ChatUpdateResponse>

    fun postBlocks(blocks: SlackPostBlocksMessage): Either<SlackAPIError, ChatPostMessageResponse>

    fun getConversations(conversations: SlackGetConversations): Either<SlackAPIError, SlackReplies>

    fun getUserGroups(getGroups: SlackGetGroups): Either<SlackAPIError, Map<SlackUserId, Collection<SlackUserGroupId>>>

    fun getUserByEmail(lookupByEmail: SlackLookupByEmail): Either<SlackAPIError, UsersLookupByEmailResponse>

    fun getUserProfile(profile: SlackGetUserProfile): Either<SlackAPIError, UsersProfileGetResponse>

    fun getOAuthToken(requestToken: RequestToken): Either<SlackAPIError, OAuthV2AccessResponse>

    fun refreshToken(refreshToken: RefreshToken): Either<SlackAPIError, OAuthV2AccessResponse>

    fun postEphemeralMessage(slackPostMessage: SlackPostMessage): Either<SlackAPIError, ChatPostEphemeralResponse>

    fun postEphemeralMessage(blocks: SlackPostBlocksMessage): Either<SlackAPIError, ChatPostEphemeralResponse>

    fun openView(view: SlackView): Either<SlackAPIError, ViewsOpenResponse>
}
