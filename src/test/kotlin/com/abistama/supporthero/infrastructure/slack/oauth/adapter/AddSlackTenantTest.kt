package com.abistama.supporthero.infrastructure.slack.oauth.adapter

import arrow.core.Either
import com.abistama.supporthero.application.slack.SlackClient
import com.abistama.supporthero.application.slackJira.events.JiraOnboardingStartedEvent
import com.abistama.supporthero.domain.slack.SlackAddTenant
import com.abistama.supporthero.domain.slack.SlackCredentials
import com.abistama.supporthero.domain.slack.SlackEnterpriseId
import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.SlackUserId
import com.abistama.supporthero.infrastructure.repository.SlackTenantRepository
import com.slack.api.methods.response.oauth.OAuthV2AccessResponse
import io.kotest.core.spec.style.FunSpec
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.springframework.context.ApplicationEventPublisher
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime

class AddSlackTenantTest :
    FunSpec({

        test("add Slack tenant") {

            val clock = mockk<Clock>()
            val fakeNow = Instant.parse("2021-07-02T00:00:00Z")
            every { clock.instant() } returns fakeNow
            every { clock.zone } returns Clock.systemUTC().zone

            val expectedCredentials =
                SlackCredentials(
                    "accessToken",
                    "bot",
                    LocalDateTime.now(clock).plusSeconds(86400),
                    "scope",
                    "refreshToken",
                )

            /*
            val authorizationUri =
                "https://authorize.atlassian.com/oauth/authorize?client_id=#{clientId}&redirect_uri=#{redirectUri}&state=#{state}&response_type=code"
            val jiraOAuthProperties =
                JiraOAuthProperties(
                    "clientId",
                    "clientSecret",
                    "http://localhost:8080/jira/oauth",
                    authorizationUri,
                )

             */

            val slackTenantRepository = mockk<SlackTenantRepository>()
            every { slackTenantRepository.add(any()) } just Runs

            /*
            val slackToJiraId = UUID.randomUUID()
            val slackToJiraRepository =
                mockk<SlackToJiraRepository> {
                    every { get(SlackTeamId("T024BE7LD00")) } returns slackToJiraId
                }

             */

            val slackAutoRefreshTokenClient = mockk<SlackClient>()
            every { slackAutoRefreshTokenClient.postBlocks(any()) } returns
                Either.Right(
                    mockk(),
                )

            val team =
                mockk<OAuthV2AccessResponse.Team>().apply {
                    every { id } returns "T024BE7LD00"
                }

            val authedUser = mockk<OAuthV2AccessResponse.AuthedUser>()
            every { authedUser.id } returns "U0G9QF9C600"

            val response = mockk<OAuthV2AccessResponse>()
            every { response.team } returns team
            every { response.enterprise } returns null
            every { response.accessToken } returns "accessToken"
            every { response.tokenType } returns "bot"
            every { response.scope } returns "scope"
            every { response.expiresIn } returns 86400
            every { response.refreshToken } returns "refreshToken"
            every { response.authedUser } returns authedUser

            val applicationEventPublisher =
                mockk<ApplicationEventPublisher> {
                    every { publishEvent(any()) } just Runs
                }

            val addSlackTenant =
                AddSlackTenant(
                    slackTenantRepository,
                    clock,
                    applicationEventPublisher,
                )

            // When
            addSlackTenant.handle(response)

            // Then
            verify(exactly = 1) {
                slackTenantRepository.add(
                    SlackAddTenant(
                        SlackTeamId("T024BE7LD00"),
                        SlackEnterpriseId(null),
                        expectedCredentials,
                    ),
                )
            }
            verify(exactly = 1) {
                applicationEventPublisher.publishEvent(
                    match<JiraOnboardingStartedEvent> {
                        it.source == addSlackTenant &&
                            it.startedBy == SlackUserId("U0G9QF9C600") &&
                            it.teamId == SlackTeamId("T024BE7LD00")
                    },
                )
            }
        }
    })
