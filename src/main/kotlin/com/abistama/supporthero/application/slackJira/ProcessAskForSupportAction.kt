package com.abistama.supporthero.application.slackJira

import com.abistama.supporthero.application.slack.SlackClient
import com.abistama.supporthero.domain.slack.SlackView
import com.abistama.supporthero.infrastructure.slack.events.adapter.SlackBlockActionsEvent
import mu.KLogging

class ProcessAskForSupportAction(
    private val slackAutoRefreshTokenClient: SlackClient,
) : KLogging() {
    fun handle(
        event: SlackBlockActionsEvent,
        action: AskForSupportAction,
    ) {
        slackAutoRefreshTokenClient
            .openView(
                SlackView(
                    generateModal(),
                    event.triggerId,
                    event.team.id,
                ),
            ).fold(
                { error ->
                    logger.error { "Could not open view due to ${error.message}" }
                },
                {
                    logger.info { "View opened" }
                },
            )
    }
}

private fun generateModal() =
    """
    {
    	"type": "modal",
        "external_id": "ask_for_support",
    	"submit": {
    		"type": "plain_text",
    		"text": "Submit",
    		"emoji": true
    	},
    	"close": {
    		"type": "plain_text",
    		"text": "Cancel",
    		"emoji": true
    	},
    	"title": {
    		"type": "plain_text",
    		"text": "Ask for Support",
    		"emoji": true
    	},
    	"blocks": [
    		{
    			"type": "section",
    			"text": {
    				"type": "plain_text",
    				"text": ":wave: Hi there!\n\nPlease describe your issue or question in the field below.\nYou'll receive a response as soon as possible, in the email address linked to your Slack account.\n\nThank you! :blush:",
    				"emoji": true
    			}
    		},
    		{
    			"type": "divider"
    		},
    		{
    			"type": "input",
    			"block_id": "support_request_input_block",
    			"element": {
    				"type": "plain_text_input",
    				"multiline": true,
    				"action_id": "support_request_input",
    				"placeholder": {
    					"type": "plain_text",
    					"text": "Describe your issue or question..."
    				}
    			},
    			"label": {
    				"type": "plain_text",
    				"text": "Support Request",
    				"emoji": true
    			}
    		}
    	]
    }
    """.trimIndent()
