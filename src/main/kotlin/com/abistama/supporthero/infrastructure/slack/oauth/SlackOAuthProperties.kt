package com.abistama.supporthero.infrastructure.slack.oauth

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "slack.oauth")
data class SlackOAuthProperties(
    val clientId: String,
    val clientSecret: String,
    val botToken: String,
    val redirectUri: String,
    val signingSecret: String,
)
