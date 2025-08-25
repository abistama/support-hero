package com.abistama.supporthero.infrastructure.slack.oauth

import com.abistama.supporthero.application.slack.SlackAPIError
import com.abistama.supporthero.application.slack.SlackClient
import com.abistama.supporthero.domain.slack.ClientId
import com.abistama.supporthero.domain.slack.ClientSecret
import com.abistama.supporthero.domain.slack.OAuthCode
import com.abistama.supporthero.domain.slack.RequestToken
import com.abistama.supporthero.infrastructure.slack.oauth.adapter.AddSlackTenant
import jakarta.servlet.http.HttpServletResponse
import mu.KLogging
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import java.net.URI

@Controller
class SlackOAuthController(
    private val slackOAuthProperties: SlackOAuthProperties,
    private val slackAutoRefreshTokenClient: SlackClient,
    private val addSlackTenant: AddSlackTenant,
) {
    companion object : KLogging()

    @GetMapping("/slack/oauth")
    fun slackOAuth(
        @RequestParam("code") code: String,
        servletResponse: HttpServletResponse,
    ) {
        logger.info { "Slack OAuth code: $code" }
        val requestToken =
            RequestToken(
                ClientId(slackOAuthProperties.clientId),
                ClientSecret(slackOAuthProperties.clientSecret),
                URI(slackOAuthProperties.redirectUri),
                OAuthCode(code),
            )
        slackAutoRefreshTokenClient
            .getOAuthToken(
                requestToken,
            ).fold(redirectToError(servletResponse)) {
                addSlackTenant.handle(it)
                servletResponse.sendRedirect("https://abistama.com/slack-ok/")
            }
    }

    private fun redirectToError(servletResponse: HttpServletResponse): (SlackAPIError) -> Unit =
        { error ->
            logger.error { "Error in the OAuth2 authentication: $error" }
            servletResponse.sendRedirect("/error")
        }
}
