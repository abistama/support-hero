package com.abistama.supporthero.infrastructure.jira.oauth

import com.abistama.supporthero.application.slackJira.LinkSlackToJira
import com.abistama.supporthero.application.slackJira.UseCaseResult.*
import com.abistama.supporthero.application.slackJira.UseCaseResult.Companion.RESULT_ERROR
import com.abistama.supporthero.application.slackJira.UseCaseResult.Companion.RESULT_SUCCESS
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.every
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(JiraOAuthController::class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class JiraOAuthControllerTest(
    @MockkBean private val linkSlackToJira: LinkSlackToJira,
) : FunSpec() {
    class ProjectConfig : AbstractProjectConfig() {
        override fun extensions() = listOf(SpringExtension)
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    init {
        test("should redirect to success page when linking Jira to Slack is successful") {
            // Given
            every { linkSlackToJira.link("my-secret-code", "state") } returns RESULT_SUCCESS

            // Then
            mockMvc
                .perform(
                    get("/jira/oauth?code=my-secret-code&state=state"),
                ).andExpect(status().is3xxRedirection)
                .andExpect(redirectedUrl("https://abistama.com/jira-ok/"))
        }

        test("should redirect to Jira error page when linking Jira to Slack is NOT successful") {
            // Given
            every { linkSlackToJira.link("my-secret-code", "state") } returns RESULT_ERROR

            // Then
            mockMvc
                .perform(
                    get("/jira/oauth?code=my-secret-code&state=state"),
                ).andExpect(status().is3xxRedirection)
                .andExpect(redirectedUrl("/jira-error"))
        }
    }
}
