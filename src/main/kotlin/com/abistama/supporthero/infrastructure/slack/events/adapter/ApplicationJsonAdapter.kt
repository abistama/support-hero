package com.abistama.supporthero.infrastructure.slack.events.adapter

import com.abistama.supporthero.application.slack.SummarizeSlackThreadFromReaction
import com.abistama.supporthero.application.slackJira.CreateJiraFromReaction
import com.abistama.supporthero.application.slackJira.ProcessSlackBlockActions
import com.abistama.supporthero.application.slackJira.UseCaseResult
import com.abistama.supporthero.application.slackJira.UseCaseResult.Companion.RESULT_SUCCESS
import com.abistama.supporthero.application.slackJira.UseCaseResultType.SUCCESS
import com.abistama.supporthero.domain.slack.events.EventType.REACTION_ADDED
import com.abistama.supporthero.domain.slack.events.ReactionAddedEvent
import com.abistama.supporthero.infrastructure.slack.events.SlackViewSubmissionEvent
import com.abistama.supporthero.infrastructure.slack.events.adapter.EventWrapperType.BLOCK_ACTIONS
import com.abistama.supporthero.infrastructure.slack.events.adapter.EventWrapperType.EVENT_CALLBACK
import com.abistama.supporthero.infrastructure.slack.events.adapter.EventWrapperType.SHORTCUT
import com.abistama.supporthero.infrastructure.slack.events.adapter.EventWrapperType.URL_VERIFICATION
import com.abistama.supporthero.infrastructure.slack.events.adapter.EventWrapperType.VIEW_SUBMISSION
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KLogging
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity

class ApplicationJsonAdapter(
    private val summarizeSlackThreadFromReaction: SummarizeSlackThreadFromReaction,
    private val createJiraFromReaction: CreateJiraFromReaction,
    private val processSlackBlockActions: ProcessSlackBlockActions,
    private val viewSubmissionHandler: ViewSubmissionHandler,
    private val shortcutHandler: ShortcutHandler,
    private val objectMapper: ObjectMapper,
) : KLogging(),
    CoroutineScope by CoroutineScope(Dispatchers.Default) {
    fun from(requestBody: String): UseCaseResult {
        val eventWrapper = objectMapper.readValue(requestBody, SlackEventWrapper::class.java)
        return when (eventWrapper.type) {
            URL_VERIFICATION ->
                UseCaseResult(SUCCESS, (eventWrapper as SlackEventUrlVerification).challenge)

            EVENT_CALLBACK -> {
                val slackEvent = (eventWrapper as SlackEventCallBack)
                logger.info { "Received event: ${slackEvent.type}" }
                when (slackEvent.event.type) {
                    REACTION_ADDED -> {
                        launch {
                            createJiraFromReaction.handle(
                                slackEvent.teamId,
                                slackEvent.event as ReactionAddedEvent,
                            )
                        }
                        launch {
                            summarizeSlackThreadFromReaction.handle(
                                slackEvent.teamId,
                                slackEvent.event as ReactionAddedEvent,
                            )
                        }
                    }

                    else -> logger.error { "Unsupported event type: ${slackEvent.type}" }
                }
                RESULT_SUCCESS
            }

            BLOCK_ACTIONS -> {
                logger.info { "Received block actions" }
                val slackEvent = (eventWrapper as SlackBlockActionsEvent)
                when (slackEvent.container.type) {
                    "message" -> {
                        logger.info { "Received block actions from message" }
                        processSlackBlockActions.handle(slackEvent)
                    }

                    "view" -> {
                        logger.info { "Received block actions from view. Discarding?" }
                    }

                    else -> {
                        logger.error { "Unsupported container type: ${slackEvent.container.type}" }
                    }
                }
                RESULT_SUCCESS
            }

            VIEW_SUBMISSION -> {
                logger.info { "Received view submission" }
                viewSubmissionHandler.handle(eventWrapper as SlackViewSubmissionEvent)
            }

            SHORTCUT -> {
                logger.info { "Received shortcut" }
                shortcutHandler.handle(eventWrapper as SlackShortcutEvent)
                RESULT_SUCCESS
            }

            else -> {
                logger.error { "Unsupported event type: ${eventWrapper.type}" }
                RESULT_SUCCESS
            }
        }
    }
}

fun UseCaseResult.toHttpResponse(): ResponseEntity<String> =
    when (this.type) {
        SUCCESS -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(this.message)
        else -> ResponseEntity.badRequest().build()
    }
