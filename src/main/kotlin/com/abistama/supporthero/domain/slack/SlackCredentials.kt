package com.abistama.supporthero.domain.slack

import com.slack.api.methods.response.oauth.OAuthV2AccessResponse
import java.time.Clock
import java.time.LocalDateTime

data class SlackCredentials(
    val accessToken: String,
    val tokenType: String,
    val expires: LocalDateTime,
    val scopes: String,
    val refreshToken: String,
)

fun OAuthV2AccessResponse.toSlackCredentials(clock: Clock) =
    SlackCredentials(
        this.accessToken,
        this.tokenType,
        LocalDateTime.now(clock).plusSeconds(this.expiresIn.toLong()),
        this.scope,
        this.refreshToken,
    )
