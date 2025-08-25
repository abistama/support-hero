package com.abistama.supporthero.infrastructure.slack.events.adapter

import arrow.core.Either
import com.abistama.supporthero.application.slack.EmailService
import com.abistama.supporthero.application.slack.SlackClient
import com.abistama.supporthero.application.slackJira.UseCaseResultType
import com.abistama.supporthero.domain.slack.JiraBoardConfiguration
import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.SlackUserGroupId
import com.abistama.supporthero.domain.slack.SlackUserId
import com.abistama.supporthero.infrastructure.jira.IssueType
import com.abistama.supporthero.infrastructure.jira.Project
import com.abistama.supporthero.infrastructure.repository.SlackToJiraConfigurationRepository
import com.abistama.supporthero.infrastructure.slack.events.SlackViewSubmissionEvent
import com.slack.api.methods.response.users.profile.UsersProfileGetResponse
import com.slack.api.model.User
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.util.*

class ViewSubmissionHandlerTest :
    FunSpec({

        test("return SUCCESS on configure_jira_reaction view submission for USER based configuration") {
            // Given
            val slackToJiraConfigurationRepository = mockk<SlackToJiraConfigurationRepository>()
            val slackClient = mockk<SlackClient>()
            val emailService = mockk<EmailService>()
            val handler = ViewSubmissionHandler(slackToJiraConfigurationRepository, slackClient, emailService)
            val slackToJiraId = UUID.randomUUID().toString()
            val event = slackViewSubmissionEvent(slackToJiraId, "U0987654321")
            val boardConfig =
                JiraBoardConfiguration.UserConfiguration(
                    Project("project-1"),
                    "reaction-1",
                    "Feedback message",
                    IssueType("Bug"),
                    SlackUserId("U0987654321"),
                    true,
                    false,
                )

            every {
                slackToJiraConfigurationRepository.add(
                    UUID.fromString(slackToJiraId),
                    SlackUserId("U0987654321"),
                    boardConfig,
                )
            } just Runs

            // When
            val result = handler.handle(event)

            // Then
            result.type shouldBe UseCaseResultType.SUCCESS
        }

        test("return SUCCESS on configure_jira_reaction view submission for USER based configuration with AI summarizer enabled") {
            // Given
            val slackToJiraConfigurationRepository = mockk<SlackToJiraConfigurationRepository>()
            val slackClient = mockk<SlackClient>()
            val emailService = mockk<EmailService>()
            val handler = ViewSubmissionHandler(slackToJiraConfigurationRepository, slackClient, emailService)
            val slackToJiraId = UUID.randomUUID().toString()
            val event = slackViewSubmissionEvent(slackToJiraId, "U0987654321", useAiSummarizer = true)
            val boardConfig =
                JiraBoardConfiguration.UserConfiguration(
                    Project("project-1"),
                    "reaction-1",
                    "Feedback message",
                    IssueType("Bug"),
                    SlackUserId("U0987654321"),
                    true,
                    true,
                )

            every {
                slackToJiraConfigurationRepository.add(
                    UUID.fromString(slackToJiraId),
                    SlackUserId("U0987654321"),
                    boardConfig,
                )
            } just Runs

            // When
            val result = handler.handle(event)

            // Then
            result.type shouldBe UseCaseResultType.SUCCESS
        }

        test("return SUCCESS on configure_jira_reaction view submission for USER GROUP based configuration") {
            // Given
            val slackToJiraConfigurationRepository = mockk<SlackToJiraConfigurationRepository>()
            val slackClient = mockk<SlackClient>()
            val emailService = mockk<EmailService>()
            val handler = ViewSubmissionHandler(slackToJiraConfigurationRepository, slackClient, emailService)
            val slackToJiraId = UUID.randomUUID().toString()
            val event = slackViewSubmissionEvent(slackToJiraId, "S0987654321", issueType = "Task")
            val boardConfig =
                JiraBoardConfiguration.UserGroupConfiguration(
                    Project("project-1"),
                    "reaction-1",
                    "Feedback message",
                    IssueType("Task"),
                    SlackUserGroupId("S0987654321"),
                    true,
                    false,
                )

            every {
                slackToJiraConfigurationRepository.add(
                    UUID.fromString(slackToJiraId),
                    SlackUserId("U0987654321"),
                    boardConfig,
                )
            } just Runs

            // When
            val result = handler.handle(event)

            // Then
            result.type shouldBe UseCaseResultType.SUCCESS
        }

        test("send email on ask_for_support view submission") {
            // Given
            val slackToJiraConfigurationRepository = mockk<SlackToJiraConfigurationRepository>()
            val slackClient = mockk<SlackClient>()
            val emailService = mockk<EmailService>()
            val handler = ViewSubmissionHandler(slackToJiraConfigurationRepository, slackClient, emailService)
            val slackToJiraId = "ask_for_support"
            val event = slackAskForSupportViewSubmissionEvent(slackToJiraId)
            every { emailService.sendEmail(any(), any(), any()) } just Runs
            every { slackClient.getUserProfile(any()) } returns
                Either.Right(
                    UsersProfileGetResponse().apply {
                        this.isOk = true
                        this.profile =
                            User.Profile().apply {
                                this.email = "myemail@email.com"
                            }
                    },
                )

            // When
            val result = handler.handle(event)

            // Then
            result.type shouldBe UseCaseResultType.SUCCESS
            verify { emailService.sendEmail("Support Request", "Support request", "myemail@email.com") }
        }

        test("do not send email on ask_for_support view submission when user profile is not found") {
            // Given
            val slackToJiraConfigurationRepository = mockk<SlackToJiraConfigurationRepository>()
            val slackClient = mockk<SlackClient>()
            val emailService = mockk<EmailService>()
            val handler = ViewSubmissionHandler(slackToJiraConfigurationRepository, slackClient, emailService)
            val slackToJiraId = "ask_for_support"
            val event = slackAskForSupportViewSubmissionEvent(slackToJiraId)
            every { emailService.sendEmail(any(), any(), any()) } just Runs
            every { slackClient.getUserProfile(any()) } returns Either.Left(mockk())

            // When
            val result = handler.handle(event)

            // Then
            result.type shouldBe UseCaseResultType.ERROR
            verify(exactly = 0) { emailService.sendEmail(any(), any(), any()) }
        }

        test("return ERROR on unknown view submission") {
            // Given
            val slackToJiraConfigurationRepository = mockk<SlackToJiraConfigurationRepository>()
            val slackClient = mockk<SlackClient>()
            val emailService = mockk<EmailService>()
            val handler = ViewSubmissionHandler(slackToJiraConfigurationRepository, slackClient, emailService)
            val baseEvent = slackViewSubmissionEvent("does not matter", "U0987654321")
            val event = baseEvent.copy(view = baseEvent.view.copy(externalId = "unknown"))

            // When
            val result = handler.handle(event)

            // Then
            result.type shouldBe UseCaseResultType.ERROR
        }

        test("return ERROR on exception") {
            // Given
            val slackToJiraConfigurationRepository = mockk<SlackToJiraConfigurationRepository>()
            val slackClient = mockk<SlackClient>()
            val emailService = mockk<EmailService>()
            val handler = ViewSubmissionHandler(slackToJiraConfigurationRepository, slackClient, emailService)
            val slackToJiraId = "exception!"
            val event = slackViewSubmissionEvent(slackToJiraId, "U0987654321")

            // When
            val result = handler.handle(event)

            // Then
            result.type shouldBe UseCaseResultType.ERROR
        }
    })

fun slackAskForSupportViewSubmissionEvent(slackToJiraId: String): SlackViewSubmissionEvent =
    SlackViewSubmissionEvent(
        team = Team(SlackTeamId("T1234567890"), "Team"),
        user = User(SlackUserId("U0987654321")),
        view =
            SlackViewSubmissionEvent.View(
                externalId = "ask_for_support",
                privateMetadata = slackToJiraId,
                state =
                    SlackViewSubmissionEvent.State(
                        values =
                            mapOf(
                                "support_request_input_block" to
                                    mapOf(
                                        "support_request_input" to
                                            SlackViewSubmissionEvent.PlainTextInput(
                                                value = "Support request",
                                            ),
                                    ),
                            ),
                    ),
            ),
    )

private fun slackViewSubmissionEvent(
    slackToJiraId: String,
    userOrGroupId: String,
    issueType: String = "Bug",
    useAiSummarizer: Boolean = false,
) = SlackViewSubmissionEvent(
    team = Team(SlackTeamId("T1234567890"), "Team"),
    user = User(SlackUserId("U0987654321")),
    view =
        SlackViewSubmissionEvent.View(
            externalId = "configure_jira_reaction",
            privateMetadata = slackToJiraId,
            state =
                SlackViewSubmissionEvent.State(
                    values =
                        mapOf(
                            "project_input_block" to
                                mapOf(
                                    "project_input" to
                                        SlackViewSubmissionEvent.StaticSelect(
                                            selectedOption = SlackViewSubmissionEvent.StaticSelect.Option("project-1"),
                                        ),
                                ),
                            "reaction_input_block" to
                                mapOf(
                                    "reaction_input" to
                                        SlackViewSubmissionEvent.StaticSelect(
                                            selectedOption = SlackViewSubmissionEvent.StaticSelect.Option("reaction-1"),
                                        ),
                                ),
                            "user_or_group_id_input_block" to
                                mapOf(
                                    "user_or_group_id_input" to
                                        SlackViewSubmissionEvent.UsersSelect(
                                            selectedUser = userOrGroupId,
                                        ),
                                ),
                            "jira_issue_type_input_block" to
                                mapOf(
                                    "jira_issue_type_input" to
                                        SlackViewSubmissionEvent.StaticSelect(
                                            selectedOption = SlackViewSubmissionEvent.StaticSelect.Option(issueType),
                                        ),
                                ),
                            "feedback_message_input_block" to
                                mapOf(
                                    "feedback_message_input" to
                                        SlackViewSubmissionEvent.PlainTextInput(
                                            value = "Feedback message",
                                        ),
                                ),
                            "send_csat_input_block" to
                                mapOf(
                                    "send_csat_input" to
                                        SlackViewSubmissionEvent.RadioButtons(
                                            selectedOption =
                                                SlackViewSubmissionEvent.RadioButtons.Option("Yes"),
                                        ),
                                ),
                            "use_ai_summarizer_input_block" to
                                mapOf(
                                    "use_ai_summarizer_input" to
                                        SlackViewSubmissionEvent.Checkboxes(
                                            selectedOptions =
                                                if (useAiSummarizer) {
                                                    listOf(
                                                        SlackViewSubmissionEvent.Checkboxes.Option("enabled"),
                                                    )
                                                } else {
                                                    emptyList()
                                                },
                                        ),
                                ),
                        ),
                ),
        ),
)
