package com.abistama.supporthero.infrastructure.slack.events

import com.abistama.supporthero.application.slackJira.UseCaseResult
import com.abistama.supporthero.application.slackJira.UseCaseResult.Companion.RESULT_ERROR
import com.abistama.supporthero.application.slackJira.UseCaseResultType.SUCCESS
import com.abistama.supporthero.infrastructure.slack.events.adapter.ApplicationJsonAdapter
import com.abistama.supporthero.infrastructure.slack.events.adapter.FormUrlEncodedAdapter
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.extensions.spring.SpringExtension
import io.mockk.every
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.net.URLEncoder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.text.Charsets.UTF_8

val jsonRequestBody =
    """
    {
        "token": "Jhj5dZrVaK7ZwHHjRyZWjbDl",
        "challenge": "3eZbrw1aBm2rZgRNFdxV2595E9CY3gmdALWMmHkvFXO7tYXAYM8P",
        "type": "url_verification"
    }
    """.trimIndent()

val formUrlEncodedBody = "challenge=3eZbrw1aBm2rZgRNFdxV2595E9CY3gmdALWMmHkvFXO7tYXAYM8P&type=url_verification"
val formUrlEncodedBodyWithJsonPayload = "payload=${URLEncoder.encode(jsonRequestBody, UTF_8)}"

data class ValidateRequestTest(
    val signingSecret: String,
    val requestBody: String,
    val mediaType: MediaType,
    val expectedStatus: ResultMatcher,
    val expectedResponse: String? = null,
    val skewTimestamp: Long = 0,
)

@WebMvcTest(SlackEventController::class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SlackEventControllerTest(
    @MockkBean private val formUrlEncodedAdapter: FormUrlEncodedAdapter,
    @MockkBean private val applicationJsonAdapter: ApplicationJsonAdapter,
) : FunSpec() {
    class ProjectConfig : AbstractProjectConfig() {
        override fun extensions() = listOf(SpringExtension)
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    init {
        context("Should validate the Slack request signature") {
            withData(
                ValidateRequestTest(
                    "signing-secret",
                    jsonRequestBody,
                    MediaType.APPLICATION_JSON,
                    status().isOk,
                    "3eZbrw1aBm2rZgRNFdxV2595E9CY3gmdALWMmHkvFXO7tYXAYM8P",
                ),
                ValidateRequestTest(
                    "nope",
                    jsonRequestBody,
                    MediaType.APPLICATION_JSON,
                    status().isUnauthorized,
                ),
                ValidateRequestTest(
                    "signing-secret ",
                    jsonRequestBody,
                    MediaType.APPLICATION_JSON,
                    status().isUnauthorized,
                    skewTimestamp = 100,
                ),
                ValidateRequestTest(
                    "signing-secret",
                    formUrlEncodedBody,
                    MediaType.APPLICATION_FORM_URLENCODED,
                    status().isOk,
                ),
                ValidateRequestTest(
                    "signing-secret",
                    formUrlEncodedBodyWithJsonPayload,
                    MediaType.APPLICATION_FORM_URLENCODED,
                    status().isOk,
                ),
            ) {

                every { applicationJsonAdapter.from(any()) } returns
                    UseCaseResult(
                        SUCCESS,
                        it.expectedResponse ?: "",
                    )

                every { formUrlEncodedAdapter.from(any()) } returns it.requestBody

                val requestBody =
                    """
                    {
                        "token": "Jhj5dZrVaK7ZwHHjRyZWjbDl",
                        "challenge": "3eZbrw1aBm2rZgRNFdxV2595E9CY3gmdALWMmHkvFXO7tYXAYM8P",
                        "type": "url_verification"
                    }
                    """.trimIndent()

                val timestamp = 1717149740
                val baseString = "v0:$timestamp:$requestBody"
                val keySpec = SecretKeySpec(it.signingSecret.toByteArray(UTF_8), "HmacSHA256")
                val mac = Mac.getInstance("HmacSHA256")
                mac.init(keySpec)
                val macBytes = mac.doFinal(baseString.toByteArray())
                val hashValue = StringBuilder(2 * macBytes.size)
                for (macByte in macBytes) {
                    hashValue.append(String.format("%02x", macByte.toInt() and 0xff))
                }
                val slackSignature = "v0=$hashValue"

                mockMvc
                    .perform(
                        post("/slack/events")
                            .contentType(it.mediaType)
                            .header("X-Slack-Request-Timestamp", timestamp + it.skewTimestamp)
                            .header("X-Slack-Signature", slackSignature)
                            .content(requestBody),
                    ).andExpect(it.expectedStatus)
                    .andExpect(content().string(it.expectedResponse ?: ""))
            }
        }

        test("should handle an error in the request, for application/json content type") {
            every { applicationJsonAdapter.from(any()) } returns RESULT_ERROR
            val requestBody = "invalid request"

            mockMvc
                .perform(
                    post("/slack/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody),
                ).andExpect(status().isBadRequest)
        }

        test("should handle an error in the request, for application/x-www-form-url-encoded content type") {
            every { applicationJsonAdapter.from(any()) } returns RESULT_ERROR
            val requestBody = "invalid request"

            mockMvc
                .perform(
                    post("/slack/events")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content(requestBody),
                ).andExpect(status().isBadRequest)
        }

        /*
        context("Should handle the Reaction Added event") {
            val requestBody =
                """
                {
                    "token": "Jhj5dZrVaK7ZwHHjRyZWjbDl",
                    "team_id": "T024BE7LD00",
                    "api_app_id": "A0F7YS25R",
                    "event": {
                        "type": "reaction_added",
                        "user": "U024BE7LH00",
                        "item": {
                            "type": "message",
                            "channel": "C0G9QF9GW00",
                            "ts": "1360782400.498405"
                        },
                        "reaction": "thumbsup",
                        "item_user": "U0G9QF9C600",
                        "event_ts": "1360782804.083113"
                    },
                    "type": "event_callback",
                    "event_id": "Ev0G9QF9M6",
                    "event_time": 1360782804,
                    "authed_users": [
                        "U024BE7LH00"
                    ]
                }
                """.trimIndent()

            val expectedEvent =
                ReactionAddedEvent(
                    SlackUserId("U024BE7LH00"),
                    "thumbsup",
                    SlackUserId("U0G9QF9C600"),
                    Item("message", SlackChannelId("C0G9QF9GW00"), SlackTs("1360782400.498405")),
                    SlackTs("1360782804.083113"),
                )

            val expectedTeam = SlackTeamId("T024BE7LD00")

            val timestamp = 1717149740
            val baseString = "v0:$timestamp:$requestBody"
            val keySpec = SecretKeySpec("signing-secret".toByteArray(UTF_8), "HmacSHA256")
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(keySpec)
            val macBytes = mac.doFinal(baseString.toByteArray())
            val hashValue = StringBuilder(2 * macBytes.size)
            for (macByte in macBytes) {
                hashValue.append(String.format("%02x", macByte.toInt() and 0xff))
            }
            val slackSignature = "v0=$hashValue"

            every { createJiraFromReaction.handle(any(), any()) } just Runs

            mockMvc
                .perform(
                    post("/slack/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Slack-Request-Timestamp", 1717149740)
                        .header("X-Slack-Signature", slackSignature)
                        .content(requestBody),
                ).andExpect(status().isOk)

            verify(exactly = 1) { createJiraFromReaction.handle(expectedTeam, expectedEvent) }
        }
         */
    }
}
