package com.abistama.supporthero.infrastructure.jira

import arrow.core.Either
import com.abistama.supporthero.application.jira.JiraAPIError
import com.abistama.supporthero.domain.jira.JiraCredentials
import com.abistama.supporthero.domain.jira.from
import com.abistama.supporthero.infrastructure.jira.events.JiraIssueResponse
import com.abistama.supporthero.infrastructure.jira.oauth.JiraOAuthAPIClient
import com.abistama.supporthero.infrastructure.jira.oauth.JiraOAuthProperties
import com.abistama.supporthero.infrastructure.jira.oauth.JiraOAuthRefreshableTokenRequest
import com.abistama.supporthero.infrastructure.repository.JiraCloudId
import com.abistama.supporthero.infrastructure.repository.JiraTenantRepository
import mu.KLogging
import java.time.Clock
import java.time.LocalDateTime

class JiraAutoRefreshTokenClient(
    private val jiraTenantRepository: JiraTenantRepository,
    private val jiraOAuthProperties: JiraOAuthProperties,
    private val jiraOAuthAPIClient: JiraOAuthAPIClient,
    private val jiraAPIClient: JiraAPIClient,
    private val clock: Clock,
) {
    companion object : KLogging()

    fun createIssue(
        jiraCloudId: JiraCloudId,
        issue: Issue,
    ): Either<JiraAPIError, IssueResponse> =
        withToken(jiraCloudId) {
            jiraAPIClient.createIssue(it.accessToken, it.jiraCloudId.value.toString(), issue)
        }

    fun getIssue(
        jiraCloudId: JiraCloudId,
        key: String,
    ): Either<JiraAPIError, JiraIssueResponse> =
        withToken(jiraCloudId) {
            jiraAPIClient.getIssue(it.accessToken, it.jiraCloudId.value.toString(), key)
        }

    fun getProjects(jiraCloudId: JiraCloudId): Either<JiraAPIError, List<Project>> =
        withToken(jiraCloudId) {
            jiraAPIClient.getProjects(it.accessToken, it.jiraCloudId.value.toString())
        }

    private fun <T> withToken(
        jiraCloudId: JiraCloudId,
        block: (JiraCredentials) -> T,
    ): Either<JiraAPIError, T> {
        logger.info { "Checking token for JiraCloudId ${jiraCloudId.value}" }
        return jiraTenantRepository.get(jiraCloudId)?.let { jiraCredentials ->
            if (jiraCredentials.expires.isBefore(LocalDateTime.now(clock))) {
                logger.info { "Refreshing token for JiraCloudId ${jiraCloudId.value}" }
                val response =
                    jiraOAuthAPIClient.getRefreshableAccessToken(
                        JiraOAuthRefreshableTokenRequest(
                            jiraOAuthProperties.clientId,
                            jiraOAuthProperties.clientSecret,
                            jiraCredentials.refreshToken,
                        ),
                    )
                val updatedCredentials = from(jiraCloudId, response, clock)
                jiraTenantRepository.add(updatedCredentials)
                logger.info { "Updated credentials..." }
                kotlin.runCatching { block(updatedCredentials) }.fold(
                    { Either.Right(it) },
                    {
                        logger.error(it) { "Error while refreshing token for JiraCloudId ${jiraCloudId.value}" }
                        Either.Left(
                            JiraAPIError(
                                it.message ?: "Error calling $block",
                            ),
                        )
                    },
                )
            } else {
                logger.info { "Token for JiraCloudId ${jiraCloudId.value} is still valid" }
                kotlin.runCatching { block(jiraCredentials) }.fold(
                    { Either.Right(it) },
                    {
                        logger.error(it) { "Error while refreshing token for JiraCloudId ${jiraCloudId.value}" }
                        Either.Left(
                            JiraAPIError(
                                it.message ?: "Error while refreshing token for JiraCloudId ${jiraCloudId.value}",
                            ),
                        )
                    },
                )
            }
        } ?: Either.Left(JiraAPIError("No credentials found for JiraCloudId ${jiraCloudId.value}"))
    }
}
