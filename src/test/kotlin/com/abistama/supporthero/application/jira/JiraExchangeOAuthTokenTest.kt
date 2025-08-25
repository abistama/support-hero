package com.abistama.supporthero.application.jira

import arrow.core.Either
import com.abistama.supporthero.domain.jira.JiraCredentials
import com.abistama.supporthero.infrastructure.jira.JiraAPIClient
import com.abistama.supporthero.infrastructure.jira.JiraBadRequestException
import com.abistama.supporthero.infrastructure.jira.oauth.JiraAccessibleResourcesResponse
import com.abistama.supporthero.infrastructure.jira.oauth.JiraOAuthAPIClient
import com.abistama.supporthero.infrastructure.jira.oauth.JiraOAuthInitialTokenRequest
import com.abistama.supporthero.infrastructure.jira.oauth.JiraOAuthProperties
import com.abistama.supporthero.infrastructure.jira.oauth.JiraOAuthTokenResponse
import com.abistama.supporthero.infrastructure.repository.JiraCloudId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class JiraExchangeOAuthTokenTest :
    FunSpec({

        test("Should get a long-lived, refreshable access token, if the user has granted access.") {
            // Given
            val clock =
                mockk<Clock> {
                    every { instant() } returns
                        LocalDateTime.of(2021, 1, 1, 0, 0).toInstant(ZoneOffset.UTC)
                    every { zone } returns ZoneOffset.UTC
                }
            val jiraCloudId = JiraCloudId(UUID.randomUUID())
            val expectedJiraCredentials =
                JiraCredentials(
                    jiraCloudId,
                    "refreshed-access-token",
                    LocalDateTime.now(clock).plusSeconds(3600),
                    "scopes",
                    "refreshed-refresh-token",
                )

            val jiraOAuthProperties =
                JiraOAuthProperties(
                    "client-id",
                    "client-secret",
                    "http://localhost/redirect",
                    "http://localhost/authorize",
                )

            val jiraAPIClient =
                mockk<JiraAPIClient> {
                    every { getAccessibleResources("refreshed-access-token") } returns
                        listOf(
                            JiraAccessibleResourcesResponse(
                                jiraCloudId.value.toString(),
                                "name",
                                "http://jira.com",
                                emptyList(),
                                "",
                            ),
                        )
                }

            val jiraOAuthAPIClient =
                mockk<JiraOAuthAPIClient> {
                    every { getInitialAccessToken(any()) } returns
                        JiraOAuthTokenResponse(
                            "access-token",
                            3600,
                            "scopes",
                            "refresh-token",
                        )
                    every { getRefreshableAccessToken(any()) } returns
                        JiraOAuthTokenResponse(
                            "refreshed-access-token",
                            3600,
                            "scopes",
                            "refreshed-refresh-token",
                        )
                }

            val jiraExChangeOAuthToken =
                JiraExchangeOAuthToken(jiraOAuthProperties, jiraOAuthAPIClient, jiraAPIClient, clock)

            // When
            val result = jiraExChangeOAuthToken.getCredentials("code")

            // Then
            result shouldBe Either.Right(expectedJiraCredentials)
        }

        test("Should return an error if it can't get the initial token") {
            // Given
            val jiraOAuthProperties =
                JiraOAuthProperties(
                    "client-id",
                    "client-secret",
                    "http://localhost/redirect",
                    "http://localhost/authorize",
                )

            val clock = mockk<Clock>()
            val jiraAPIClient = mockk<JiraAPIClient>()
            val jiraOAuthAPIClient =
                mockk<JiraOAuthAPIClient> {
                    every {
                        getInitialAccessToken(
                            JiraOAuthInitialTokenRequest(
                                "my-secret-code",
                                "client-id",
                                "client-secret",
                                "http://localhost/redirect",
                            ),
                        )
                    } throws JiraBadRequestException("not authorized")
                }

            val jiraExChangeOAuthToken =
                JiraExchangeOAuthToken(jiraOAuthProperties, jiraOAuthAPIClient, jiraAPIClient, clock)

            // When
            val result = jiraExChangeOAuthToken.getCredentials("my-secret-code")

            // Then
            result shouldBe Either.Left(JiraOAuthAPIError("not authorized"))
        }

        test("Should return an error if it can't get the refreshable token.") {
            // Given
            val clock =
                mockk<Clock> {
                    every { instant() } returns
                        LocalDateTime.of(2021, 1, 1, 0, 0).toInstant(ZoneOffset.UTC)
                    every { zone } returns ZoneOffset.UTC
                }
            val jiraOAuthProperties =
                JiraOAuthProperties(
                    "client-id",
                    "client-secret",
                    "http://localhost/redirect",
                    "http://localhost/authorize",
                )

            val jiraAPIClient = mockk<JiraAPIClient>()

            val jiraOAuthAPIClient =
                mockk<JiraOAuthAPIClient> {
                    every { getInitialAccessToken(any()) } returns
                        JiraOAuthTokenResponse(
                            "access-token",
                            3600,
                            "scopes",
                            "refresh-token",
                        )
                    every { getRefreshableAccessToken(any()) } throws JiraBadRequestException("not authorized")
                }

            val jiraExChangeOAuthToken =
                JiraExchangeOAuthToken(jiraOAuthProperties, jiraOAuthAPIClient, jiraAPIClient, clock)

            // When
            val result = jiraExChangeOAuthToken.getCredentials("code")

            // Then
            result shouldBe Either.Left(JiraOAuthAPIError("not authorized"))
        }
    })
