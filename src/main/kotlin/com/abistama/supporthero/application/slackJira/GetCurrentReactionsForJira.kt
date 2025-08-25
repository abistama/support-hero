package com.abistama.supporthero.application.slackJira

import com.abistama.supporthero.application.slack.SlackClient
import com.abistama.supporthero.application.slackJira.events.JiraOnboardingStartedEvent
import com.abistama.supporthero.domain.slack.SlackPostBlocksMessage
import com.abistama.supporthero.domain.slack.SlackUserTrigger
import com.abistama.supporthero.domain.slackToJira.JiraBoardsStatistics
import com.abistama.supporthero.infrastructure.repository.SlackToJiraConfigurationRepository
import com.abistama.supporthero.infrastructure.repository.SlackToJiraRepository
import com.slack.api.model.kotlin_extension.block.withBlocks
import mu.KLogging
import java.util.*

class GetCurrentReactionsForJira(
    private val slackToJiraRepository: SlackToJiraRepository,
    private val slackToJiraConfigurationRepository: SlackToJiraConfigurationRepository,
    private val slackAutoRefreshTokenClient: SlackClient,
    private val slackToJiraOnboardingMessageService: SlackToJiraOnboardingMessageService,
) : KLogging() {
    fun handle(trigger: SlackUserTrigger) {
        logger.info { "Getting current reactions for Jira action..." }
        slackToJiraConfigurationRepository.getByOwner(trigger.user.id)?.let {
            slackAutoRefreshTokenClient.postBlocks(
                SlackPostBlocksMessage(
                    trigger.user.id,
                    generateBlocksForJiraBoardsConfiguration(it.jiraBoardsConfiguration),
                    trigger.team.id,
                ),
            )
        } ?: slackToJiraRepository.get(trigger.team.id)?.let {
            handleNoConfigurations(trigger, it)
        } ?: handleNoTenant(trigger)
    }

    private fun handleNoTenant(trigger: SlackUserTrigger) {
        slackToJiraOnboardingMessageService.firstOnboarding(
            JiraOnboardingStartedEvent(
                this,
                trigger.user.id,
                trigger.team.id,
            ),
        )
    }

    private fun handleNoConfigurations(
        trigger: SlackUserTrigger,
        slackToJiraId: UUID,
    ) {
        slackAutoRefreshTokenClient.postBlocks(
            SlackPostBlocksMessage(
                trigger.user.id,
                withBlocks {
                    header {
                        text(":gear: Project configurations", emoji = true)
                    }
                    section {
                        markdownText("You don't have any configurations yet.")
                    }
                    actions {
                        button {
                            text("Create one :heavy_plus_sign:", emoji = true)
                            value("$slackToJiraId")
                            actionId("configure_jira_reaction||")
                        }
                    }
                },
                trigger.team.id,
            ),
        )
    }

    private fun csatToEmoji(csat: Float) =
        when {
            csat < 2 -> ":large_red_circle:"
            csat < 4 -> ":large_yellow_circle:"
            else -> ":large_green_circle:"
        }

    private fun generateBlocksForJiraBoardsConfiguration(jiraBoardConfiguration: List<JiraBoardsStatistics>) =
        withBlocks {
            header {
                text(":gear: Project configurations", emoji = true)
            }
            section {
                markdownText("Here you can see the configurations owned by you or create a new one.")
            }
            divider()
            jiraBoardConfiguration.map {
                section {
                    markdownText(
                        ":${it.configuration.reaction}: in ${it.configuration.channelId
                            ?.id
                            ?.let { "<#$it>" } ?: "all channels"} " +
                            "creates a ticket in ${it.configuration.project.key} Jira project",
                    )
                    accessory {
                        button {
                            text("Edit", emoji = true)
                            value(it.configurationId.toString())
                            actionId("edit_jira_reaction||")
                        }
                    }
                }
                section {
                    if (it.configuration.sendCsat) {
                        markdownText(
                            "${csatToEmoji(
                                it.avgCsat,
                            )} Average CSAT: ${it.avgCsat}",
                        )
                    } else {
                        markdownText("No CSAT configured.")
                    }
                    accessory {
                        button {
                            text("Delete", emoji = true)
                            style("danger")
                            confirm {
                                title("Are you sure?")
                                markdownText("This action will delete the configuration.")
                                confirm("Yes, delete it")
                                deny("No, keep it")
                            }
                            value(it.configurationId.toString())
                            actionId("delete_jira_reaction||")
                        }
                    }
                }
            }
        }
}
