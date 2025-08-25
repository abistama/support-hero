package com.abistama.supporthero.application.slackJira

import com.abistama.supporthero.application.slack.SlackClient
import com.abistama.supporthero.domain.jira.SlackToJiraCsat
import com.abistama.supporthero.domain.slack.SlackPostBlocksMessage
import com.abistama.supporthero.infrastructure.jira.repository.JiraCsatRepository
import com.slack.api.model.kotlin_extension.block.withBlocks
import mu.KLogging
import java.time.Duration

class SendCsatReminderUseCase(
    private val jiraCsatRepository: JiraCsatRepository,
    private val slackAutoRefreshTokenClient: SlackClient,
) {
    companion object : KLogging()

    fun execute() {
        logger.info { "Sending CSAT reminders..." }
        jiraCsatRepository.getReminders().forEach { reminder ->
            slackAutoRefreshTokenClient
                .postBlocks(
                    SlackPostBlocksMessage(
                        reminder.sendTo,
                        createCsatSurveyBlock(reminder),
                        reminder.slackTeamId,
                    ),
                ).fold(
                    { error ->
                        logger.error { "Could not send CSAT reminder to user ${reminder.sendTo} due to ${error.message}" }
                    },
                    {
                        logger.info { "CSAT reminder sent to user ${reminder.sendTo}" }
                        jiraCsatRepository.decreaseReminderAttempts(reminder.id, Duration.ofHours(24))
                    },
                )
        }
    }
}

fun createCsatSurveyBlock(slackToJiraCsat: SlackToJiraCsat) =
    withBlocks {
        section {
            markdownText(
                "Hello! We would like to know how your experience was with the ticket ${slackToJiraCsat.ticketKey}. " +
                    "Please take a moment to fill out the CSAT survey.", // i18n
            )
        }
        actions {
            elements {
                button {
                    text("1", emoji = true)
                    style("danger")
                    value("1")
                    actionId("csat|${slackToJiraCsat.id}|1")
                }
                button {
                    text("2", emoji = true)
                    style("danger")
                    value("2")
                    actionId("csat|${slackToJiraCsat.id}|2")
                }
                button {
                    text("3", emoji = true)
                    value("3")
                    actionId("csat|${slackToJiraCsat.id}|3")
                }
                button {
                    text("4", emoji = true)
                    style("primary")
                    value("4")
                    actionId("csat|${slackToJiraCsat.id}|4")
                }
                button {
                    text("5", emoji = true)
                    style("primary")
                    value("5")
                    actionId("csat|${slackToJiraCsat.id}|5")
                }
            }
        }
    }
