package com.abistama.supporthero.infrastructure.slack.oauth

import arrow.core.Either
import com.abistama.supporthero.application.slack.SlackAPIError
import com.abistama.supporthero.application.slack.SlackClient
import com.abistama.supporthero.infrastructure.slack.oauth.adapter.AddSlackTenant
import com.ninjasquad.springmockk.MockkBean
import com.slack.api.methods.response.oauth.OAuthV2AccessResponse
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(SlackOAuthController::class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SlackOAuthControllerTest(
    @MockkBean private val slackOAuthProperties: SlackOAuthProperties,
    @MockkBean private val slackAutoRefreshClient: SlackClient,
    @MockkBean private val addSlackTenant: AddSlackTenant,
) : FunSpec() {
    class ProjectConfig : AbstractProjectConfig() {
        override fun extensions() = listOf(SpringExtension)
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    init {

        val oauthAccessResponse = mockk<OAuthV2AccessResponse>()
        every { oauthAccessResponse.accessToken } returns "accessToken"
        every { oauthAccessResponse.tokenType } returns "bot"
        every { oauthAccessResponse.expiresIn } returns 86400
        every { oauthAccessResponse.refreshToken } returns "refreshToken"
        every {
            oauthAccessResponse.scope
        } returns "channels:history,channels:read,users:read,users:read.email,chat:write,im:write,reactions:read,users.profile:read"

        every { slackOAuthProperties.clientId }.returns("clientId")
        every { slackOAuthProperties.clientSecret }.returns("clientSecret")
        every { slackOAuthProperties.redirectUri }.returns("http://localhost:8080/slack/oauth")

        every { addSlackTenant.handle(any()) } just Runs

        test("Successful OAuth") {

            every { slackAutoRefreshClient.getOAuthToken(any()) } returns Either.Right(oauthAccessResponse)

            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .get("/slack/oauth")
                        .queryParam("code", "code"),
                ).andExpect(status().isFound)
                .andExpect(redirectedUrl("https://abistama.com/slack-ok/"))
        }

        test("Handles Slack API error") {

            every { slackOAuthProperties.clientId }.returns("clientId")
            every { slackOAuthProperties.clientSecret }.returns("clientSecret")
            every { slackOAuthProperties.redirectUri }.returns("http://localhost:8080/slack/oauth")
            every { slackAutoRefreshClient.getOAuthToken(any()) } returns Either.Left(SlackAPIError("error"))

            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .get("/slack/oauth")
                        .queryParam("code", "code"),
                ).andExpect(status().isFound)
                .andExpect(redirectedUrl("/error"))
        }
    }
}
