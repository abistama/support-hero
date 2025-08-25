package com.abistama.supporthero.application.slackJira

import arrow.core.toOption
import com.abistama.supporthero.application.slack.events.SlackDomain
import com.abistama.supporthero.application.slack.events.SlackTeamProcessedEvent
import com.abistama.supporthero.domain.slack.JiraBoardConfigurationIdTrigger
import com.abistama.supporthero.domain.slack.SlackToJiraTenantTrigger
import com.abistama.supporthero.infrastructure.slack.events.adapter.SlackBlockActionsEvent
import mu.KLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.redis.core.RedisTemplate

class ProcessSlackBlockActions(
    private val processCsatAction: ProcessCsatAction,
    private val processAskForSupportAction: ProcessAskForSupportAction,
    private val processConfigureReactionForJira: ProcessConfigureReactionForJira,
    private val processDeleteReactionForJira: ProcessDeleteReactionForJira,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val redisTemplate: RedisTemplate<String, Any>,
) {
    companion object {
        private val logger = KLogging().logger
    }

    fun handle(event: SlackBlockActionsEvent) {
        event.actions
            .firstOrNull()
            .toOption()
            .flatMap { blockAction -> blockAction.toAction() }
            .map { action ->
                when (action) {
                    is CSatAction -> {
                        logger.info { "Processing CSAT action..." }
                        when (event.container) {
                            is SlackBlockActionsEvent.MessageContainer -> {
                                processCsatAction.handle(
                                    event.team.id,
                                    event.container,
                                    action,
                                )
                            }

                            else -> {
                                logger.info { "Container not supported: ${event.container.type}" }
                            }
                        }
                    }

                    is ConfigureReactionForJiraAction -> {
                        processConfigureReactionForJira.handle(
                            SlackToJiraTenantTrigger(
                                event.triggerId,
                                event.team,
                                action.slackToJiraTenant,
                            ),
                        )
                    }

                    is DeleteReactionForJiraAction -> {
                        logger.info { "Processing DeleteReactionForJiraAction..." }
                        event.container as SlackBlockActionsEvent.MessageContainer
                        processDeleteReactionForJira.handle(
                            JiraBoardConfigurationIdTrigger(
                                event.triggerId,
                                event.team,
                                action.configurationId,
                                event.container,
                            ),
                        )
                    }

                    is AskForSupportAction -> {
                        logger.info { "Processing AskForSupportAction..." }
                        processAskForSupportAction.handle(
                            event,
                            action,
                        )
                    }

                    is LinkJiraCloudAction -> {
                        event.container as SlackBlockActionsEvent.MessageContainer
                        redisTemplate.opsForValue().set("onboarding-dm:${event.team.id.id}", event.container.channelId)
                    }
                }
            }
        applicationEventPublisher.publishEvent(
            SlackTeamProcessedEvent(
                this,
                event.team.id,
                SlackDomain(event.team.domain),
            ),
        )
    }
}
