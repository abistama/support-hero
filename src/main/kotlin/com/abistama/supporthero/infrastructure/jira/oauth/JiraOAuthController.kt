package com.abistama.supporthero.infrastructure.jira.oauth

import com.abistama.supporthero.application.slackJira.LinkSlackToJira
import com.abistama.supporthero.application.slackJira.UseCaseResult
import com.abistama.supporthero.application.slackJira.UseCaseResult.*
import jakarta.servlet.http.HttpServletResponse
import mu.KLogging
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import java.util.*

@Controller
class JiraOAuthController(
    private val linkSlackToJira: LinkSlackToJira,
) {
    companion object : KLogging()

    @GetMapping("/jira/oauth")
    fun jiraOAuth(
        @RequestParam("code") code: String,
        @RequestParam("state") state: String,
        response: HttpServletResponse,
    ) {
        logger.info { "Received Jira OAuth code: $code" }
        logger.info { "Received Jira OAuth state: $state" }

        when (linkSlackToJira.link(code, state)) {
            UseCaseResult.RESULT_SUCCESS -> {
                response.sendRedirect("https://abistama.com/jira-ok/")
            }

            UseCaseResult.RESULT_ERROR -> {
                response.sendRedirect("/jira-error")
            }
        }
    }
}
