package com.abistama.supporthero.infrastructure.slack.events

import com.abistama.supporthero.domain.slack.SlackSignatureValidation
import com.abistama.supporthero.infrastructure.slack.events.adapter.ApplicationJsonAdapter
import com.abistama.supporthero.infrastructure.slack.events.adapter.FormUrlEncodedAdapter
import com.abistama.supporthero.infrastructure.slack.events.adapter.toHttpResponse
import com.abistama.supporthero.infrastructure.slack.oauth.SlackOAuthProperties
import jakarta.servlet.http.HttpServletRequest
import mu.KLogging
import org.apache.commons.io.IOUtils
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import kotlin.text.Charsets.UTF_8

@RestController
class SlackEventController(
    private val slackOAuthProperties: SlackOAuthProperties,
    private val formUrlEncodedAdapter: FormUrlEncodedAdapter,
    private val applicationJsonAdapter: ApplicationJsonAdapter,
) : KLogging() {
    @PostMapping("/slack/events")
    fun receiveEvent(
        @RequestHeader("X-Slack-Request-Timestamp") slackRequestTimestamp: String,
        @RequestHeader("X-Slack-Signature") slackSignature: String,
        request: HttpServletRequest,
    ): ResponseEntity<String> {
        val contentType = request.contentType
        val requestBody = IOUtils.toString(request.inputStream, UTF_8)
        logger.info { "Received request: $requestBody" }
        val slackSignatureValidation =
            SlackSignatureValidation(
                requestBody,
                slackRequestTimestamp,
                slackSignature,
            )
        return if (slackSignatureValidation.isValid(slackOAuthProperties.signingSecret)) {
            when (contentType) {
                "application/json" -> applicationJsonAdapter.from(requestBody).toHttpResponse()
                "application/x-www-form-urlencoded" ->
                    formUrlEncodedAdapter
                        .from(requestBody)
                        ?.let { applicationJsonAdapter.from(it) }
                        ?.toHttpResponse() ?: invalidPayload()

                else -> ResponseEntity.status(UNSUPPORTED_MEDIA_TYPE).build()
            }
        } else {
            logger.info { "Unauthorized request" }
            UNAUTHORIZED
        }
    }

    private fun invalidPayload() = ResponseEntity.badRequest().body("Invalid payload")

    companion object {
        val UNAUTHORIZED = ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<String>()
    }
}
