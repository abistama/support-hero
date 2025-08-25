package com.abistama.supporthero.infrastructure.jira.oauth

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jira.oauth")
data class JiraOAuthProperties(
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String,
    val authorizationUri: String,
)
