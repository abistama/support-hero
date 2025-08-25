package com.abistama.supporthero.infrastructure.slack.events.adapter

import com.abistama.supporthero.application.slackJira.GetCurrentReactionsForJira
import com.abistama.supporthero.application.slackJira.ProcessConfigureReactionForJira
import com.abistama.supporthero.application.slackJira.UseCaseResult
import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.SlackToJiraTenantTrigger
import com.abistama.supporthero.domain.slack.SlackUserId
import com.abistama.supporthero.domain.slack.SlackUserTrigger
import com.abistama.supporthero.infrastructure.repository.SlackToJiraRepository
import com.abistama.supporthero.infrastructure.slack.events.adapter.CallbackId.CONFIGURE_JIRA_REACTION
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

class ShortcutHandlerTest :
    FunSpec({

        val slackToJiraTenant = UUID.randomUUID()
        val slackToJiraRepository = mockk<SlackToJiraRepository>()
        val processConfigureReactionForJira = mockk<ProcessConfigureReactionForJira>()
        val getCurrentReactionsForJira = mockk<GetCurrentReactionsForJira>()
        val shortcutHandler =
            ShortcutHandler(slackToJiraRepository, processConfigureReactionForJira, getCurrentReactionsForJira)

        beforeTest {
            clearAllMocks()
        }

        context("handle 'configure_jira_reaction' shortcut event") {
            test("should return SUCCESS if SlackToJiraTenant exists") {
                // Given
                every { slackToJiraRepository.get(SlackTeamId("T050VAYDD5E")) } returns slackToJiraTenant
                every { processConfigureReactionForJira.handle(any()) } just Runs

                // When
                val result =
                    shortcutHandler.handle(
                        SlackShortcutEvent(
                            "trigger_id",
                            CONFIGURE_JIRA_REACTION,
                            Team(SlackTeamId("T050VAYDD5E"), "team_domain"),
                            User(SlackUserId("U05084E8G2Y")),
                        ),
                    )

                // Then
                verify {
                    processConfigureReactionForJira.handle(
                        SlackToJiraTenantTrigger(
                            "trigger_id",
                            Team(SlackTeamId("T050VAYDD5E"), "team_domain"),
                            slackToJiraTenant,
                        ),
                    )
                }
                result shouldBe UseCaseResult.RESULT_SUCCESS
            }
            test("should return ERROR if SlackToJiraTenant does NOT exist") {
                // Given
                every { slackToJiraRepository.get(SlackTeamId("T1234567890")) } returns null

                // When
                val result =
                    shortcutHandler.handle(
                        SlackShortcutEvent(
                            "trigger_id",
                            CONFIGURE_JIRA_REACTION,
                            Team(SlackTeamId("T1234567890"), "team_domain"),
                            User(SlackUserId("U05084E8G2Y")),
                        ),
                    )

                // Then
                verify(exactly = 0) {
                    processConfigureReactionForJira.handle(any())
                }
                result shouldBe UseCaseResult.RESULT_ERROR
            }
        }

        context("handle 'get_configured_jira_reactions' shortcut event") {
            test("should return SUCCESS") {
                // Given
                every { slackToJiraRepository.get(SlackTeamId("T050VAYDD5E")) } returns slackToJiraTenant
                every { getCurrentReactionsForJira.handle(any()) } just Runs

                // When
                val result =
                    shortcutHandler.handle(
                        SlackShortcutEvent(
                            "trigger_id",
                            CallbackId.GET_CONFIGURED_JIRA_REACTIONS,
                            Team(SlackTeamId("T050VAYDD5E"), "team_domain"),
                            User(SlackUserId("U05084E8G2Y")),
                        ),
                    )

                // Then
                verify {
                    getCurrentReactionsForJira.handle(
                        SlackUserTrigger(
                            "trigger_id",
                            Team(SlackTeamId("T050VAYDD5E"), "team_domain"),
                            User(SlackUserId("U05084E8G2Y")),
                        ),
                    )
                }
                result shouldBe UseCaseResult.RESULT_SUCCESS
            }
        }
    })
