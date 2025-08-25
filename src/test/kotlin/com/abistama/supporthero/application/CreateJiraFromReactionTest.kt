package com.abistama.supporthero.application

import arrow.core.Either
import com.abistama.supporthero.application.slack.SlackAPIError
import com.abistama.supporthero.application.slack.SlackClient
import com.abistama.supporthero.application.slack.UserGroupService
import com.abistama.supporthero.application.slackJira.CreateJiraFromReaction
import com.abistama.supporthero.application.summarizer.Summarizer
import com.abistama.supporthero.domain.slack.JiraBoardConfiguration
import com.abistama.supporthero.domain.slack.SlackChannelId
import com.abistama.supporthero.domain.slack.SlackMessage
import com.abistama.supporthero.domain.slack.SlackPostMessage
import com.abistama.supporthero.domain.slack.SlackPostTo
import com.abistama.supporthero.domain.slack.SlackReplies
import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.SlackUserId
import com.abistama.supporthero.domain.slack.events.Item
import com.abistama.supporthero.domain.slack.events.ReactionAddedEvent
import com.abistama.supporthero.domain.slack.events.SlackTs
import com.abistama.supporthero.infrastructure.jira.Issue
import com.abistama.supporthero.infrastructure.jira.IssueFields
import com.abistama.supporthero.infrastructure.jira.IssueResponse
import com.abistama.supporthero.infrastructure.jira.IssueType
import com.abistama.supporthero.infrastructure.jira.JiraAutoRefreshTokenClient
import com.abistama.supporthero.infrastructure.jira.Project
import com.abistama.supporthero.infrastructure.jira.events.JiraIssueCreatedEvent
import com.abistama.supporthero.infrastructure.repository.JiraCloudId
import com.abistama.supporthero.infrastructure.repository.SlackToJiraConfigurationRepository
import io.kotest.core.spec.style.FunSpec
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.springframework.context.ApplicationEventPublisher
import java.util.*

class CreateJiraFromReactionTest :
    FunSpec({

        test("Should not process a message if it can't get its details") {
            // Given
            val slackToJiraConfigurationRepository = mockk<SlackToJiraConfigurationRepository>()
            val userGroupService = mockk<UserGroupService>()
            val slackClient = mockk<SlackClient>()
            val jiraAutoRefreshTokenClient = mockk<JiraAutoRefreshTokenClient>()
            val applicationEventPublisher = mockk<ApplicationEventPublisher>()
            val aiSummarizer = mockk<Summarizer>()
            val createJiraFromReaction =
                CreateJiraFromReaction(
                    slackToJiraConfigurationRepository,
                    userGroupService,
                    slackClient,
                    jiraAutoRefreshTokenClient,
                    applicationEventPublisher,
                    mutableSetOf(),
                    mutableSetOf(),
                    aiSummarizer,
                )
            val slackTeamId = SlackTeamId("T1234567890")
            val slackUserId = SlackUserId("U1234567890")
            val slackChannelId = SlackChannelId("C1234567890")
            val event =
                ReactionAddedEvent(
                    slackUserId,
                    "ticket",
                    slackUserId,
                    Item("reaction_added", slackChannelId, SlackTs("1360782804.083113")),
                    SlackTs("1360782804.083113"),
                )

            every { slackClient.getConversations(any()) } returns Either.Left(SlackAPIError("Error"))

            // When
            createJiraFromReaction.handle(slackTeamId, event)

            // Then
            verify(exactly = 0) {
                slackToJiraConfigurationRepository.getSlackToJiraConfig(
                    any(),
                    any(),
                    any(),
                    any(),
                )
            }
        }

        test("Repeated messages should not be processed") {
            // Given
            val slackToJiraConfigurationRepository = mockk<SlackToJiraConfigurationRepository>()
            val userGroupService = mockk<UserGroupService>()
            val slackClient = mockk<SlackClient>(relaxed = true)
            val jiraAutoRefreshTokenClient = mockk<JiraAutoRefreshTokenClient>()
            val applicationEventPublisher = mockk<ApplicationEventPublisher>()
            val aiSummarizer = mockk<Summarizer>()
            val createJiraFromReaction =
                CreateJiraFromReaction(
                    slackToJiraConfigurationRepository,
                    userGroupService,
                    slackClient,
                    jiraAutoRefreshTokenClient,
                    applicationEventPublisher,
                    mutableSetOf(),
                    mutableSetOf(),
                    aiSummarizer,
                )
            val slackTeamId = SlackTeamId("T1234567890")
            val slackUserId = SlackUserId("U1234567890")
            val slackChannelId = SlackChannelId("C1234567890")
            val event =
                ReactionAddedEvent(
                    slackUserId,
                    "ticket",
                    slackUserId,
                    Item("reaction_added", slackChannelId, SlackTs("1360782804.083113")),
                    SlackTs("1360782804.083113"),
                )
            val events = listOf(event, event)
            val message = SlackMessage(SlackTs("1360782804.083113"), "Hello, world!", "username")
            val reply = SlackReplies(listOf(message), slackChannelId, SlackTs("1360782804.083113"))

            every { userGroupService.getUserGroups(any()) } returns emptyList()
            every { slackToJiraConfigurationRepository.getSlackToJiraConfig(any(), any(), any(), any()) } returns null
            every { slackClient.getConversations(any()) } returns Either.Right(reply)

            // When
            events.forEach { createJiraFromReaction.handle(slackTeamId, it) }

            // Then
            verify(exactly = 1) {
                slackToJiraConfigurationRepository.getSlackToJiraConfig(
                    slackUserId,
                    emptyList(),
                    "ticket",
                    slackChannelId,
                )
            }
        }

        test("Different messages should be processed") {
            // Given
            val slackToJiraConfigurationRepository = mockk<SlackToJiraConfigurationRepository>()
            val userGroupService = mockk<UserGroupService>()
            val slackClient = mockk<SlackClient>()
            val jiraAutoRefreshTokenClient = mockk<JiraAutoRefreshTokenClient>()
            val applicationEventPublisher = mockk<ApplicationEventPublisher>()
            val aiSummarizer = mockk<Summarizer>()
            val createJiraFromReaction =
                CreateJiraFromReaction(
                    slackToJiraConfigurationRepository,
                    userGroupService,
                    slackClient,
                    jiraAutoRefreshTokenClient,
                    applicationEventPublisher,
                    mutableSetOf(),
                    mutableSetOf(),
                    aiSummarizer,
                )
            val slackTeamId = SlackTeamId("T1234567890")
            val slackUserId = SlackUserId("U1234567890")
            val slackChannelId = SlackChannelId("C1234567890")
            val event =
                ReactionAddedEvent(
                    slackUserId,
                    "ticket",
                    slackUserId,
                    Item("reaction_added", slackChannelId, SlackTs("1360782804.083113")),
                    SlackTs("1360782804.083113"),
                )
            val events =
                listOf(event, event.copy(eventTs = SlackTs("1360782804.083114"), item = event.item.copy(ts = SlackTs("1360782804.083114"))))

            val message1 = SlackMessage(SlackTs("1360782804.083113"), "Hello, world!", "username")
            val message2 = SlackMessage(SlackTs("1360782999.083999"), "A new message!", "username")

            val reply1 = SlackReplies(listOf(message1), slackChannelId, SlackTs("1360782804.083113"))
            val reply2 = SlackReplies(listOf(message2), slackChannelId, SlackTs("1360782999.083999"))

            every { userGroupService.getUserGroups(any()) } returns emptyList()
            every { slackToJiraConfigurationRepository.getSlackToJiraConfig(any(), any(), any(), any()) } returns null
            every { slackClient.getConversations(any()) }.returnsMany(Either.Right(reply1), Either.Right(reply2))

            // When
            events.forEach { createJiraFromReaction.handle(slackTeamId, it) }

            // Then
            verify(exactly = events.size) {
                slackToJiraConfigurationRepository.getSlackToJiraConfig(
                    slackUserId,
                    emptyList(),
                    "ticket",
                    slackChannelId,
                )
            }
        }

        test("Two messages from the same thread should not be processed IF one of them has a ticket created") {
            // Given
            val slackToJiraConfigurationRepository = mockk<SlackToJiraConfigurationRepository>()
            val userGroupService = mockk<UserGroupService>()
            val slackClient = mockk<SlackClient>(relaxed = true)
            val jiraAutoRefreshTokenClient = mockk<JiraAutoRefreshTokenClient>()
            val applicationEventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
            val aiSummarizer = mockk<Summarizer>()
            val createJiraFromReaction =
                CreateJiraFromReaction(
                    slackToJiraConfigurationRepository,
                    userGroupService,
                    slackClient,
                    jiraAutoRefreshTokenClient,
                    applicationEventPublisher,
                    mutableSetOf(),
                    mutableSetOf(),
                    aiSummarizer,
                )
            val slackTeamId = SlackTeamId("T1234567890")
            val slackUserId = SlackUserId("U1234567890")
            val slackChannelId = SlackChannelId("C1234567890")
            val event1 =
                ReactionAddedEvent(
                    slackUserId,
                    "ticket",
                    slackUserId,
                    Item("reaction_added", slackChannelId, SlackTs("1360782804.083113")),
                    SlackTs("1360782804.083113"),
                )
            val event2 =
                ReactionAddedEvent(
                    slackUserId,
                    "ticket",
                    slackUserId,
                    Item("reaction_added", slackChannelId, SlackTs("1360782999.083999")),
                    SlackTs("1360782999.083999"),
                )
            val events = listOf(event1, event2)

            val message1 = SlackMessage(SlackTs("1360782804.083113"), "Hello, world!", "username")
            val message2 = SlackMessage(SlackTs("1360782999.083999"), "A thread!", "username", SlackTs("1360782804.083113"))

            val reply1 = SlackReplies(listOf(message1), slackChannelId, SlackTs("1360782804.083113"))
            val reply2 = SlackReplies(listOf(message2), slackChannelId, SlackTs("1360782999.083999"))

            every { userGroupService.getUserGroups(any()) } returns emptyList()
            every { slackToJiraConfigurationRepository.getSlackToJiraConfig(any(), any(), any(), any()) } returns
                Pair(
                    JiraCloudId(UUID.randomUUID()),
                    JiraBoardConfiguration.UserConfiguration(
                        Project("TOMR", "Tomahawk"),
                        "ticket",
                        "Yey! A new ticket!",
                        IssueType("Task"),
                        slackUserId,
                        false,
                        false,
                    ),
                )
            every { slackClient.getConversations(any()) }.returnsMany(Either.Right(reply1), Either.Right(reply2))
            every { jiraAutoRefreshTokenClient.createIssue(any(), any()) } returns
                Either.Right(
                    IssueResponse(
                        "1234",
                        "TOMR-1234",
                        "self",
                    ),
                )

            // When
            events.forEach { createJiraFromReaction.handle(slackTeamId, it) }

            // Then
            verify(exactly = 1) {
                jiraAutoRefreshTokenClient.createIssue(
                    any(),
                    any(),
                )
            }
        }

        test("Two messages from the same thread should be processed IF none of them has a ticket created") {
            // Given
            val slackToJiraConfigurationRepository = mockk<SlackToJiraConfigurationRepository>()
            val userGroupService = mockk<UserGroupService>()
            val slackClient = mockk<SlackClient>(relaxed = true)
            val jiraAutoRefreshTokenClient = mockk<JiraAutoRefreshTokenClient>()
            val applicationEventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
            val aiSummarizer = mockk<Summarizer>()
            val createJiraFromReaction =
                CreateJiraFromReaction(
                    slackToJiraConfigurationRepository,
                    userGroupService,
                    slackClient,
                    jiraAutoRefreshTokenClient,
                    applicationEventPublisher,
                    mutableSetOf(),
                    mutableSetOf(),
                    aiSummarizer,
                )
            val slackTeamId = SlackTeamId("T1234567890")
            val slackUserId = SlackUserId("U1234567890")
            val slackChannelId = SlackChannelId("C1234567890")
            val event1 =
                ReactionAddedEvent(
                    slackUserId,
                    "ticket",
                    slackUserId,
                    Item("reaction_added", slackChannelId, SlackTs("1360782804.083113")),
                    SlackTs("1360782804.083113"),
                )
            val event2 =
                ReactionAddedEvent(
                    slackUserId,
                    "ticket",
                    slackUserId,
                    Item("reaction_added", slackChannelId, SlackTs("1360782999.083999")),
                    SlackTs("1360782999.083999"),
                )
            val events = listOf(event1, event2)

            val message1 = SlackMessage(SlackTs("1360782804.083113"), "Hello, world!", "username")
            val message2 = SlackMessage(SlackTs("1360782999.083999"), "A thread!", "username", SlackTs("1360782804.083113"))

            val reply1 = SlackReplies(listOf(message1), slackChannelId, SlackTs("1360782804.083113"))
            val reply2 = SlackReplies(listOf(message2), slackChannelId, SlackTs("1360782999.083999"))

            every { userGroupService.getUserGroups(any()) } returns emptyList()
            every { slackToJiraConfigurationRepository.getSlackToJiraConfig(any(), any(), any(), any()) }.returnsMany(
                Pair(
                    JiraCloudId(UUID.randomUUID()),
                    JiraBoardConfiguration.UserConfiguration(
                        Project("TOMR", "Tomahawk"),
                        "ticket",
                        "Yey! A new ticket!",
                        IssueType("Task"),
                        slackUserId,
                        false,
                        false,
                    ),
                ),
                Pair(
                    JiraCloudId(UUID.randomUUID()),
                    JiraBoardConfiguration.UserConfiguration(
                        Project("TOMR", "Tomahawk"),
                        "fire",
                        "Yey! A new ticket!",
                        IssueType("Task"),
                        slackUserId,
                        false,
                        false,
                    ),
                ),
            )

            every { slackClient.getConversations(any()) }.returnsMany(Either.Right(reply1), Either.Right(reply2))
            every { jiraAutoRefreshTokenClient.createIssue(any(), any()) } returns
                Either.Right(
                    IssueResponse(
                        "1234",
                        "TOMR-1234",
                        "self",
                    ),
                )

            // When
            events.forEach { createJiraFromReaction.handle(slackTeamId, it) }

            // Then
            verify(exactly = 1) {
                jiraAutoRefreshTokenClient.createIssue(
                    any(),
                    any(),
                )
            }
        }

        test("Should create a JIRA ticket from a reaction") {
            // Given
            val slackToJiraConfigurationRepository = mockk<SlackToJiraConfigurationRepository>()
            val userGroupService = mockk<UserGroupService>()
            val slackClient = mockk<SlackClient>(relaxed = true)
            val jiraAutoRefreshTokenClient = mockk<JiraAutoRefreshTokenClient>()
            val applicationEventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
            val aiSummarizer = mockk<Summarizer>()
            val createJiraFromReaction =
                CreateJiraFromReaction(
                    slackToJiraConfigurationRepository,
                    userGroupService,
                    slackClient,
                    jiraAutoRefreshTokenClient,
                    applicationEventPublisher,
                    mutableSetOf(),
                    mutableSetOf(),
                    aiSummarizer,
                )

            val jiraCloudId = JiraCloudId(UUID.randomUUID())

            val slackTeamId = SlackTeamId("T1234567890")
            val slackUserId = SlackUserId("U1234567890")
            val slackChannelId = SlackChannelId("C1234567890")
            val event =
                ReactionAddedEvent(
                    slackUserId,
                    "ticket",
                    slackUserId,
                    Item("reaction_added", slackChannelId, SlackTs("1360782804.083113")),
                    SlackTs("1360782804.083113"),
                )
            val message = SlackMessage(SlackTs("1360782804.083113"), "Hello, world!", "username")
            val reply = SlackReplies(listOf(message), slackChannelId, SlackTs("1360782804.083113"))

            val jiraBoardConfig =
                JiraBoardConfiguration.UserConfiguration(
                    Project("TOMR", "Tomahawk"),
                    "ticket",
                    "Yey! A new ticket!",
                    IssueType("Task"),
                    slackUserId,
                    false,
                )
            val expectedIssueResponse = IssueResponse("1234", "TOMR-1234", "self")

            every { userGroupService.getUserGroups(any()) } returns emptyList()
            every { slackToJiraConfigurationRepository.getSlackToJiraConfig(any(), any(), any(), any()) } returns
                Pair(
                    jiraCloudId,
                    jiraBoardConfig,
                )
            every { slackClient.getConversations(any()) } returns Either.Right(reply)
            every { jiraAutoRefreshTokenClient.createIssue(any(), any()) } returns
                Either.Right(expectedIssueResponse)
            every { applicationEventPublisher.publishEvent(any()) } just Runs

            // When
            createJiraFromReaction.handle(slackTeamId, event)

            // Then
            verify(exactly = 1) {
                jiraAutoRefreshTokenClient.createIssue(
                    jiraCloudId,
                    Issue(
                        IssueFields(
                            jiraBoardConfig.project,
                            message.text,
                            message.text,
                            jiraBoardConfig.issueType,
                        ),
                    ),
                )
            }
            verify(exactly = 1) {
                applicationEventPublisher.publishEvent(
                    match<JiraIssueCreatedEvent> {
                        it.source == createJiraFromReaction &&
                            it.jiraCloudId == jiraCloudId &&
                            it.issue == expectedIssueResponse
                    },
                )
            }
        }

        test("Should report back feedback to the user") {
            // Given
            val slackToJiraConfigurationRepository = mockk<SlackToJiraConfigurationRepository>()
            val userGroupService = mockk<UserGroupService>()
            val slackClient = mockk<SlackClient>()
            val jiraAutoRefreshTokenClient = mockk<JiraAutoRefreshTokenClient>()
            val applicationEventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
            val aiSummarizer = mockk<Summarizer>()
            val createJiraFromReaction =
                CreateJiraFromReaction(
                    slackToJiraConfigurationRepository,
                    userGroupService,
                    slackClient,
                    jiraAutoRefreshTokenClient,
                    applicationEventPublisher,
                    mutableSetOf(),
                    mutableSetOf(),
                    aiSummarizer,
                )

            val jiraCloudId = JiraCloudId(UUID.randomUUID())

            val slackTeamId = SlackTeamId("T1234567890")
            val userWhoReacted = SlackUserId("U0123456789")
            val slackUserId = SlackUserId("U1234567890")
            val slackChannelId = SlackChannelId("C1234567890")
            val event =
                ReactionAddedEvent(
                    userWhoReacted,
                    "ticket",
                    slackUserId,
                    Item("reaction_added", slackChannelId, SlackTs("1360782804.083113")),
                    SlackTs("1360782804.083113"),
                )
            val message = SlackMessage(SlackTs("1360782804.083113"), "Hello, world!", "username")
            val reply = SlackReplies(listOf(message), slackChannelId, SlackTs("1360782804.083113"))

            val jiraBoardConfig =
                JiraBoardConfiguration.UserConfiguration(
                    Project("TOMR", "Tomahawk"),
                    "ticket",
                    "Yey! A new ticket!",
                    IssueType("Task"),
                    slackUserId,
                    false,
                )

            val expectedSlackPostMessage =
                SlackPostMessage(
                    SlackPostTo(slackChannelId, SlackTs("1360782804.083113")),
                    "Yey! A new ticket!",
                    slackTeamId,
                )

            every { userGroupService.getUserGroups(any()) } returns emptyList()
            every { slackToJiraConfigurationRepository.getSlackToJiraConfig(any(), any(), any(), any()) } returns
                Pair(
                    jiraCloudId,
                    jiraBoardConfig,
                )
            every { slackClient.getConversations(any()) } returns Either.Right(reply)
            every { jiraAutoRefreshTokenClient.createIssue(any(), any()) } returns
                Either.Right(
                    IssueResponse(
                        "1234",
                        "TOMR-1234",
                        "self",
                    ),
                )
            every { slackClient.postMessage(expectedSlackPostMessage) } returns Either.Right(mockk())

            // When
            createJiraFromReaction.handle(slackTeamId, event)

            // Then
            verify(exactly = 1) {
                slackClient.postMessage(
                    expectedSlackPostMessage,
                )
            }
        }
    })
