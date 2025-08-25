package com.abistama.supporthero.application.jira

import arrow.core.Either
import com.abistama.supporthero.domain.jira.JiraCredentials
import com.abistama.supporthero.domain.jira.from
import com.abistama.supporthero.infrastructure.jira.JiraAPIClient
import com.abistama.supporthero.infrastructure.jira.oauth.JiraOAuthAPIClient
import com.abistama.supporthero.infrastructure.jira.oauth.JiraOAuthInitialTokenRequest
import com.abistama.supporthero.infrastructure.jira.oauth.JiraOAuthProperties
import com.abistama.supporthero.infrastructure.jira.oauth.JiraOAuthRefreshableTokenRequest
import com.abistama.supporthero.infrastructure.repository.JiraCloudId
import java.time.Clock
import java.util.*

class JiraExchangeOAuthToken(
    private val jiraOAuthProperties: JiraOAuthProperties,
    private val jiraOAuthAPIClient: JiraOAuthAPIClient,
    private val jiraAPIClient: JiraAPIClient,
    private val clock: Clock,
) {
    fun getCredentials(code: String): Either<JiraOAuthAPIError, JiraCredentials> =
        kotlin
            .runCatching {
                val tokenResponse =
                    jiraOAuthAPIClient.getInitialAccessToken(
                        JiraOAuthInitialTokenRequest(
                            code,
                            jiraOAuthProperties.clientId,
                            jiraOAuthProperties.clientSecret,
                            jiraOAuthProperties.redirectUri,
                        ),
                    )
                val refreshableToken =
                    jiraOAuthAPIClient.getRefreshableAccessToken(
                        JiraOAuthRefreshableTokenRequest(
                            jiraOAuthProperties.clientId,
                            jiraOAuthProperties.clientSecret,
                            tokenResponse.refreshToken,
                        ),
                    )

                val accessibleResources = jiraAPIClient.getAccessibleResources(refreshableToken.accessToken)
                val jiraCloudId = JiraCloudId(UUID.fromString(accessibleResources.first().id))
                Either.Right(
                    from(
                        jiraCloudId,
                        refreshableToken,
                        clock,
                    ),
                )
            }.fold(
                { it },
                { Either.Left(fromException(it)) },
            )
}
