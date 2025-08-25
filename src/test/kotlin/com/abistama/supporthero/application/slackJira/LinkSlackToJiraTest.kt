package com.abistama.supporthero.application.slackJira

import arrow.core.Either
import com.abistama.supporthero.application.jira.JiraExchangeOAuthToken
import com.abistama.supporthero.application.jira.JiraOAuthAPIError
import com.abistama.supporthero.application.slack.SlackClient
import com.abistama.supporthero.domain.jira.JiraCredentials
import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.SlackUserId
import com.abistama.supporthero.domain.slack.events.SlackTs
import com.abistama.supporthero.domain.slackToJira.JiraOnboardingInProgress
import com.abistama.supporthero.infrastructure.repository.JiraCloudId
import com.abistama.supporthero.infrastructure.repository.JiraTenantRepository
import com.abistama.supporthero.infrastructure.repository.SlackToJiraRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.redis.core.RedisTemplate
import java.time.LocalDateTime
import java.util.*

class LinkSlackToJiraTest :
    FunSpec({

        test("should return ERROR when it fails to get valid Jira credentials") {
            // Given
            val jiraExchangeOAuthToken =
                mockk<JiraExchangeOAuthToken> {
                    every { getCredentials("my-secret-code") } returns Either.Left(JiraOAuthAPIError("unauthorized"))
                }
            val linkSlackToJira =
                LinkSlackToJira(
                    jiraExchangeOAuthToken,
                    mockk<JiraTenantRepository>(),
                    mockk<SlackToJiraRepository>(),
                    mockk<SlackToJiraOnboardingMessageService>(),
                    mockk<SlackClient>(),
                    mockk<RedisTemplate<String, Any>>(),
                )

            // When
            val result = linkSlackToJira.link("my-secret-code", "state")

            // Then
            result shouldBe UseCaseResult.RESULT_ERROR
        }

        test("should save the Jira tenant when it successfully gets valid Jira credentials") {
            // Given
            val expectedCredentials =
                JiraCredentials(
                    JiraCloudId(UUID.randomUUID()),
                    "access-token",
                    LocalDateTime.MIN,
                    "scopes",
                    "refresh-token",
                )
            val jiraExchangeOAuthToken =
                mockk<JiraExchangeOAuthToken> {
                    every { getCredentials("my-secret-code") } returns Either.Right(expectedCredentials)
                }
            val jiraTenantRepository =
                mockk<JiraTenantRepository> {
                    every { add(any()) } returns 1
                }
            val slackToJiraRepository = mockk<SlackToJiraRepository>(relaxed = true)
            val slackToJiraOnboardingMessageService = mockk<SlackToJiraOnboardingMessageService>(relaxed = true)
            val slackAutoRefreshTokenClient = mockk<SlackClient>()
            val redisTemplate =
                mockk<RedisTemplate<String, Any>> {
                    every { opsForValue().getAndDelete("onboarding:T1234567890") } returns
                        JiraOnboardingInProgress(
                            SlackUserId("U0G9QF9C600"),
                            SlackTeamId("T1234567890"),
                            SlackTs("1234567890.123456"),
                        )
                    every { opsForValue().getAndDelete("onboarding-dm:T1234567890") } returns "1234567890.123456"
                }
            val linkSlackToJira =
                LinkSlackToJira(
                    jiraExchangeOAuthToken,
                    jiraTenantRepository,
                    slackToJiraRepository,
                    slackToJiraOnboardingMessageService,
                    slackAutoRefreshTokenClient,
                    redisTemplate,
                )

            // When
            val result = linkSlackToJira.link("my-secret-code", "T1234567890")

            // Then
            verify(exactly = 1) { jiraTenantRepository.add(expectedCredentials) }
            verify {
                redisTemplate.opsForValue().getAndDelete("onboarding:T1234567890")
                redisTemplate.opsForValue().getAndDelete("onboarding-dm:T1234567890")
            }
            result shouldBe UseCaseResult.RESULT_SUCCESS
        }

        test("should link Slack to Jira tenant when it successfully gets valid Jira credentials") {
            // Given
            val expectedCredentials =
                JiraCredentials(
                    JiraCloudId(UUID.randomUUID()),
                    "access-token",
                    LocalDateTime.MIN,
                    "scopes",
                    "refresh-token",
                )
            val jiraExchangeOAuthToken =
                mockk<JiraExchangeOAuthToken> {
                    every { getCredentials("my-secret-code") } returns Either.Right(expectedCredentials)
                }
            val jiraTenantRepository =
                mockk<JiraTenantRepository>(relaxed = true)
            val slackToJiraId = UUID.randomUUID()
            val slackToJiraRepository =
                mockk<SlackToJiraRepository> {
                    every { add(any(), any()) } returns slackToJiraId
                }
            val slackToJiraOnboardingMessageService = mockk<SlackToJiraOnboardingMessageService>(relaxed = true)
            val slackAutoRefreshTokenClient = mockk<SlackClient>()
            val redisTemplate =
                mockk<RedisTemplate<String, Any>> {
                    every { opsForValue().getAndDelete(any()) } returns
                        JiraOnboardingInProgress(
                            SlackUserId("U0G9QF9C600"),
                            SlackTeamId("T1234567890"),
                            SlackTs("1234567890.123456"),
                        )
                }
            val linkSlackToJira =
                LinkSlackToJira(
                    jiraExchangeOAuthToken,
                    jiraTenantRepository,
                    slackToJiraRepository,
                    slackToJiraOnboardingMessageService,
                    slackAutoRefreshTokenClient,
                    redisTemplate,
                )

            // When
            val result = linkSlackToJira.link("my-secret-code", "T1234567890")

            // Then
            verify(exactly = 1) {
                slackToJiraRepository.add(
                    SlackTeamId("T1234567890"),
                    expectedCredentials.jiraCloudId,
                )
            }
            verify(exactly = 1) {
                slackToJiraOnboardingMessageService.alreadyConnected(
                    slackToJiraId,
                    SlackUserId(
                        "U0G9QF9C600",
                    ),
                    SlackTeamId("T1234567890"),
                )
            }
            result shouldBe UseCaseResult.RESULT_SUCCESS
        }
    })
