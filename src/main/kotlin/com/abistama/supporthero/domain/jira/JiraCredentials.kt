package com.abistama.supporthero.domain.jira

import com.abistama.supporthero.infrastructure.jira.oauth.JiraOAuthTokenResponse
import com.abistama.supporthero.infrastructure.repository.JiraCloudId
import java.time.Clock
import java.time.LocalDateTime

data class JiraCredentials(
    val jiraCloudId: JiraCloudId,
    val accessToken: String,
    val expires: LocalDateTime,
    val scopes: String,
    val refreshToken: String,
)

fun from(
    jiraCloudId: JiraCloudId,
    response: JiraOAuthTokenResponse,
    clock: Clock,
) = JiraCredentials(
    jiraCloudId,
    response.accessToken,
    LocalDateTime.now(clock).plusSeconds(response.expiresIn.toLong()),
    response.scopes,
    response.refreshToken,
)
