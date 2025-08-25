package com.abistama.supporthero.domain.slack

import java.net.URI

@JvmInline
value class ClientId(val value: String)

@JvmInline
value class ClientSecret(val value: String)

@JvmInline
value class OAuthCode(val value: String)

data class RequestToken(
    val clientId: ClientId,
    val clientSecret: ClientSecret,
    val redirectUri: URI,
    val code: OAuthCode,
)
