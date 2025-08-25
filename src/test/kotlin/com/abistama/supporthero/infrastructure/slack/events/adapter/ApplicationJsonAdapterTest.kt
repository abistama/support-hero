package com.abistama.supporthero.infrastructure.slack.events.adapter

import com.abistama.supporthero.application.slack.SummarizeSlackThreadFromReaction
import com.abistama.supporthero.application.slackJira.CreateJiraFromReaction
import com.abistama.supporthero.application.slackJira.ProcessSlackBlockActions
import com.abistama.supporthero.application.slackJira.UseCaseResult
import com.abistama.supporthero.application.slackJira.UseCaseResultType
import com.abistama.supporthero.domain.slack.SlackChannelId
import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.SlackUserId
import com.abistama.supporthero.domain.slack.events.Item
import com.abistama.supporthero.domain.slack.events.ReactionAddedEvent
import com.abistama.supporthero.domain.slack.events.SlackTs
import com.abistama.supporthero.infrastructure.slack.events.adapter.CallbackId.CONFIGURE_JIRA_REACTION
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.nio.file.Paths

class ApplicationJsonAdapterTest :
    StringSpec({

        val createJiraFromReaction = mockk<CreateJiraFromReaction>()
        val summarizeSlackThreadFromReaction = mockk<SummarizeSlackThreadFromReaction>()
        val processSlackBlockActions = mockk<ProcessSlackBlockActions>()
        val viewSubmissionHandler = mockk<ViewSubmissionHandler>()
        val shortcutHandler = mockk<ShortcutHandler>()
        val objectMapper = jacksonObjectMapper()
        val appJsonAdapter =
            ApplicationJsonAdapter(
                summarizeSlackThreadFromReaction,
                createJiraFromReaction,
                processSlackBlockActions,
                viewSubmissionHandler,
                shortcutHandler,
                objectMapper,
            )

        beforeTest {
            clearAllMocks()
        }

        "should return challenge on URL_VERIFICATION" {
            val requestBody = """{"type":"url_verification","challenge":"test_challenge", "token":"test_token"}"""

            val result = appJsonAdapter.from(requestBody)

            result.type shouldBe UseCaseResultType.SUCCESS
            result.message shouldBe "test_challenge"
        }

        "should handle REACTION_ADDED event" {
            runTest {
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
                val event =
                    ReactionAddedEvent(
                        SlackUserId("U024BE7LH00"),
                        "thumbsup",
                        SlackUserId("U0G9QF9C600"),
                        Item("message", SlackChannelId("C0G9QF9GW00"), SlackTs("1360782400.498405")),
                        SlackTs("1360782804.083113"),
                    )

                every { createJiraFromReaction.handle(SlackTeamId("T024BE7LD00"), event) } just Runs
                every { summarizeSlackThreadFromReaction.handle(any(), any()) } just Runs

                val result = appJsonAdapter.from(requestBody)

                coVerify { createJiraFromReaction.handle(SlackTeamId("T024BE7LD00"), event) }
                coVerify { summarizeSlackThreadFromReaction.handle(SlackTeamId("T024BE7LD00"), event) }
                result.type shouldBe UseCaseResultType.SUCCESS
            }
        }

        "should handle BLOCK_ACTIONS from message" {
            val uri =
                this::class.java.classLoader
                    .getResource("block-actions-event.json")
                    ?.toURI()
            val requestBody = uri?.let { Paths.get(it) }?.let { Files.readAllLines(it) }?.joinToString("\n")!!

            every { processSlackBlockActions.handle(any()) } just Runs

            val result = appJsonAdapter.from(requestBody)

            verify { processSlackBlockActions.handle(any()) }
            result.type shouldBe UseCaseResultType.SUCCESS
        }

        "should handle BLOCK_ACTIONS from view" {
            val uri =
                this::class.java.classLoader
                    .getResource("block-actions-view-event.json")
                    ?.toURI()
            val requestBody = uri?.let { Paths.get(it) }?.let { Files.readAllLines(it) }?.joinToString("\n")!!

            val result = appJsonAdapter.from(requestBody)

            result.type shouldBe UseCaseResultType.SUCCESS
        }

        "should handle VIEW_SUBMISSION" {
            val uri =
                this::class.java.classLoader
                    .getResource("view-submission-event.json")
                    ?.toURI()
            val requestBody = uri?.let { Paths.get(it) }?.let { Files.readAllLines(it) }?.joinToString("\n")!!
            every { viewSubmissionHandler.handle(any()) } returns UseCaseResult.RESULT_SUCCESS

            val result = appJsonAdapter.from(requestBody)

            verify { viewSubmissionHandler.handle(any()) }
            result.type shouldBe UseCaseResultType.SUCCESS
        }

        "should handle SHORTCUT" {
            val requestBody =
                """
                {
                    "type": "shortcut",
                    "token": "pBNLGVq7ddxF2wB36SgdXJxJ",
                    "action_ts": "1720093833.136686",
                    "team": {
                        "id": "T050VAYDD5E",
                        "domain": "joao-test-group"
                    },
                    "user": {
                        "id": "U05084E8G2Y",
                        "username": "joaoqalves",
                        "team_id": "T050VAYDD5E"
                    },
                    "is_enterprise_install": false,
                    "enterprise": null,
                    "callback_id": "configure_jira_reaction",
                    "trigger_id": "7376146959378.5029372455184.52d6216876ebe98fb655a084ba0b276b"
                }
                """.trimIndent()
            val event =
                SlackShortcutEvent(
                    "7376146959378.5029372455184.52d6216876ebe98fb655a084ba0b276b",
                    CONFIGURE_JIRA_REACTION,
                    Team(SlackTeamId("T050VAYDD5E"), "joao-test-group"),
                    User(SlackUserId("U05084E8G2Y")),
                )

            every { shortcutHandler.handle(event) } returns UseCaseResult.RESULT_SUCCESS

            // When
            val result = appJsonAdapter.from(requestBody)

            verify { shortcutHandler.handle(event) }
            result.type shouldBe UseCaseResultType.SUCCESS
        }
    })
