package com.abistama.supporthero.infrastructure.slack.events.adapter

import arrow.core.toOption
import com.abistama.supporthero.application.slackJira.GetCurrentReactionsForJira
import com.abistama.supporthero.application.slackJira.ProcessConfigureReactionForJira
import com.abistama.supporthero.application.slackJira.UseCaseResult
import com.abistama.supporthero.domain.slack.SlackToJiraTenantTrigger
import com.abistama.supporthero.domain.slack.SlackUserTrigger
import com.abistama.supporthero.infrastructure.repository.SlackToJiraRepository
import com.abistama.supporthero.infrastructure.slack.events.adapter.CallbackId.CONFIGURE_JIRA_REACTION
import com.abistama.supporthero.infrastructure.slack.events.adapter.CallbackId.GET_CONFIGURED_JIRA_REACTIONS
import mu.KLogging

class ShortcutHandler(
    private val slackToJiraRepository: SlackToJiraRepository,
    private val processConfigureReactionForJira: ProcessConfigureReactionForJira,
    private val getCurrentReactionsForJira: GetCurrentReactionsForJira,
) {
    companion object : KLogging()

    fun handle(event: SlackShortcutEvent): UseCaseResult =
        when (event.callbackId) {
            CONFIGURE_JIRA_REACTION ->
                slackToJiraRepository.get(event.team.id).toOption().fold(
                    {
                        logger.error { "Could not get SlackToJiraTenant from team ${event.team.id}" }
                        UseCaseResult.RESULT_ERROR
                    },
                    {
                        processConfigureReactionForJira.handle(
                            SlackToJiraTenantTrigger(event.triggerId, event.team, it),
                        )
                        UseCaseResult.RESULT_SUCCESS
                    },
                )

            GET_CONFIGURED_JIRA_REACTIONS -> {
                getCurrentReactionsForJira.handle(SlackUserTrigger(event.triggerId, event.team, event.user))
                UseCaseResult.RESULT_SUCCESS
            }

            else -> UseCaseResult.RESULT_ERROR
        }
}
