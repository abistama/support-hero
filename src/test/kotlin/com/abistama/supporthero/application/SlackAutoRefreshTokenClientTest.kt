package com.abistama.supporthero.application

import arrow.core.Either
import com.abistama.supporthero.application.slack.SlackAutoRefreshTokenClient
import com.abistama.supporthero.application.slack.SlackClient
import com.abistama.supporthero.domain.slack.SlackChannelId
import com.abistama.supporthero.domain.slack.SlackCredentials
import com.abistama.supporthero.domain.slack.SlackPostMessage
import com.abistama.supporthero.domain.slack.SlackPostTo
import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.infrastructure.repository.SlackTenantRepository
import com.abistama.supporthero.infrastructure.slack.oauth.SlackOAuthProperties
import com.slack.api.methods.response.oauth.OAuthV2AccessResponse
import io.kotest.core.spec.style.FunSpec
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime

data class PostMessageTest(
    val now: Instant,
    val expectedCredentials: SlackCredentials,
    val expectedRefreshedCredentials: SlackCredentials? = null,
)

class SlackAutoRefreshTokenClientTest :
    FunSpec({

        val fakeNow = Instant.parse("2021-07-02T00:00:00Z")

        test("postMessage with expired credentials") {
            val slackTenantRepository = mockk<SlackTenantRepository>()
            val slackOAuthProperties = mockk<SlackOAuthProperties>(relaxed = true)
            val slackClient = mockk<SlackClient>(relaxed = true)
            val clock = mockk<Clock>()
            val refreshedToken = mockk<OAuthV2AccessResponse>()
            val slackAutoRefreshTokenClient =
                SlackAutoRefreshTokenClient(slackTenantRepository, slackOAuthProperties, slackClient, clock)

            val expiredCredentialsDateTime = LocalDateTime.parse("2021-07-01T00:00:00")
            val refreshedCredentialsDateTime = LocalDateTime.parse("2021-07-10T00:00:00")
            val expiredCredentials = SlackCredentials("ABC", "Bearer", expiredCredentialsDateTime, "scope", "DEF")
            val refreshedCredentials = SlackCredentials("BCA", "Bearer", refreshedCredentialsDateTime, "scope", "FED")

            every { clock.instant() } returns fakeNow
            every { clock.zone } returns Clock.systemUTC().zone
            every { slackTenantRepository.get(any()) } returns expiredCredentials
            every { slackTenantRepository.updateToken(any(), any()) } just Runs

            every { refreshedToken.accessToken } returns "BCA"
            every { refreshedToken.tokenType } returns "Bearer"
            every { refreshedToken.expiresIn } returns 691200
            every { refreshedToken.scope } returns "scope"
            every { refreshedToken.refreshToken } returns "FED"
            every { slackClient.refreshToken(any()) } returns Either.Right(refreshedToken)

            val slackChannelId = SlackChannelId("C0G9QF9GW00")
            val slackTeamId = SlackTeamId("T024BE7LD00")

            val message = "ABC"
            val postMessage = SlackPostMessage(SlackPostTo(slackChannelId), message, slackTeamId)
            slackAutoRefreshTokenClient.postMessage(postMessage)

            verify {
                slackClient.postMessage(
                    SlackPostMessage(
                        SlackPostTo(slackChannelId),
                        message,
                        slackTeamId,
                        slackCredentials = refreshedCredentials,
                    ),
                )
            }
        }

        test("postMessage with valid credentials") {
            // Given
            val slackTenantRepository = mockk<SlackTenantRepository>()
            val slackOAuthProperties = mockk<SlackOAuthProperties>(relaxed = true)
            val slackClient = mockk<SlackClient>(relaxed = true)
            val clock = mockk<Clock>()
            val slackAutoRefreshTokenClient =
                SlackAutoRefreshTokenClient(slackTenantRepository, slackOAuthProperties, slackClient, clock)

            val nonExpiredCredentialsDateTime = LocalDateTime.parse("2021-07-03T00:00:00")
            val nonExpiredCredentials = SlackCredentials("ABC", "Bearer", nonExpiredCredentialsDateTime, "scope", "DEF")

            every { clock.instant() } returns fakeNow
            every { clock.zone } returns Clock.systemUTC().zone
            every { slackTenantRepository.get(any()) } returns nonExpiredCredentials

            val slackChannelId = SlackChannelId("C0G9QF9GW00")
            val slackTeamId = SlackTeamId("T024BE7LD00")

            val message = "ABC"
            val postMessage = SlackPostMessage(SlackPostTo(slackChannelId), message, slackTeamId)

            // When
            slackAutoRefreshTokenClient.postMessage(postMessage)

            // Then
            verify {
                slackClient.postMessage(
                    SlackPostMessage(
                        SlackPostTo(slackChannelId),
                        message,
                        slackTeamId,
                        slackCredentials = nonExpiredCredentials,
                    ),
                )
            }
        }
    })
