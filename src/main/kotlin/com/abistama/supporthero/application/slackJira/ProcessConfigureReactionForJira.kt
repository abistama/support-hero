package com.abistama.supporthero.application.slackJira

import arrow.core.Either
import arrow.core.toOption
import com.abistama.supporthero.application.jira.JiraAPIError
import com.abistama.supporthero.application.slack.SlackClient
import com.abistama.supporthero.domain.slack.SlackToJiraTenantTrigger
import com.abistama.supporthero.domain.slack.SlackView
import com.abistama.supporthero.infrastructure.jira.JiraAutoRefreshTokenClient
import com.abistama.supporthero.infrastructure.jira.Project
import com.abistama.supporthero.infrastructure.repository.JiraTenantRepository
import mu.KLogging
import java.util.*

class ProcessConfigureReactionForJira(
    private val jiraTenantRepository: JiraTenantRepository,
    private val autoRefreshTokenClient: SlackClient,
    private val jiraAutoRefreshTokenClient: JiraAutoRefreshTokenClient,
) : KLogging() {
    fun handle(trigger: SlackToJiraTenantTrigger) {
        logger.info { "Processing configure reaction for Jira action..." }
        jiraTenantRepository.getJiraCloudId(trigger.slackToJiraTenant).toOption().fold(
            {
                logger.error { "Could not get JiraCloudId from tenant ${trigger.slackToJiraTenant}" }
            },
            { jiraCloudId ->
                jiraAutoRefreshTokenClient.getProjects(jiraCloudId).fold(
                    { error ->
                        logger.error { "Could not get projects due to ${error.message}" }
                        Either.Left(JiraAPIError("Could not get projects"))
                    },
                    { projects ->
                        autoRefreshTokenClient
                            .openView(
                                SlackView(
                                    generateConfigureEmojiModal("Abistama", projects, trigger.slackToJiraTenant),
                                    trigger.triggerId,
                                    trigger.team.id,
                                ),
                            ).fold(
                                { error ->
                                    logger.error { "Could not open view due to ${error.message}" }
                                },
                                {
                                    logger.info { "View opened" }
                                },
                            )
                    },
                )
            },
        )
    }
}

fun generateConfigureEmojiModal(
    appName: String,
    projects: List<Project>,
    slackToJiraTenant: UUID,
): String =
    """
    {
    "private_metadata": "$slackToJiraTenant",
    "external_id": "configure_jira_reaction",
	"title": {
		"type": "plain_text",
		"text": "$appName",
		"emoji": true
	},
	"submit": {
		"type": "plain_text",
		"text": "Submit",
		"emoji": true
	},
	"type": "modal",
	"close": {
		"type": "plain_text",
		"text": "Cancel",
		"emoji": true
	},
	"blocks": [
		{
			"type": "section",
			"text": {
				"type": "mrkdwn",
				"text": ":wave: Hi! Let's configure $appName"
			}
		},
		{
			"type": "divider"
		},
		{
			"type": "section",
			"block_id": "project_input_block",
			"text": {
				"type": "mrkdwn",
				"text": ":mailbox: *Jira Project*\nWhere $appName will create tickets"
			},
			"accessory": {
				"type": "static_select",
				"action_id": "project_input",
				"placeholder": {
					"type": "plain_text",
					"text": "Select Jira Project"
				},
				"options": [${
        projects.joinToString(",") { project ->
            """
                    {
                        "text": {
                            "type": "plain_text",
                            "text": "${project.name}",
                            "emoji": true
                        },
                        "value": "${project.key}"
                    }
                """
        }
    }]
			}
		},
		{
			"type": "section",
			"block_id": "reaction_input_block",
			"text": {
				"type": "mrkdwn",
				"text": ":thumbsup: *Reaction Emoji*\nOnly messages with this reaction create a ticket"
			},
			"accessory": {
				"type": "static_select",
				"action_id": "reaction_input",
				"placeholder": {
					"type": "plain_text",
					"text": "Select the reaction emoji"
				},
				"options": [
					{
						"text": {
							"type": "plain_text",
							"text": ":ticket:",
							"emoji": true
						},
						"value": "ticket"
					},
					{
						"text": {
							"type": "plain_text",
							"text": ":bug:",
							"emoji": true
						},
						"value": "bug"
					},
					{
						"text": {
							"type": "plain_text",
							"text": ":eyes:",
							"emoji": true
						},
						"value": "eyes"
					},
					{
						"text": {
							"type": "plain_text",
							"text": ":fire:",
							"emoji": true
						},
						"value": "fire"
					}
				]
			}
		},
		{
			"type": "section",
			"block_id": "user_or_group_id_input_block",
			"text": {
				"type": "mrkdwn",
				"text": ":dart: *User or User Group*\n Only them will be able to create a ticket"
			},
			"accessory": {
				"type": "users_select",
				"action_id": "user_or_group_id_input",
				"placeholder": {
					"type": "plain_text",
					"text": "Select the Slack User or User Group"
				}
			}
		},
        {
			"type": "section",
			"block_id": "jira_issue_type_input_block",
			"text": {
				"type": "mrkdwn",
				"text": ":game_die: *Jira issue type*\n It will create the ticket with this type (e.g. Task, Bug)"
			},
			"accessory": {
				"type": "static_select",
				"action_id": "jira_issue_type_input",
                "placeholder": {
                    "type": "plain_text",
                    "text": "Select the issue type"
			    },
                "options": [
                    {
                        "text": {
                            "type": "plain_text",
                            "text": "Task",
                            "emoji": true
                        },
                        "value": "Task"
                    },
                    {
                        "text": {
                            "type": "plain_text",
                            "text": "Bug",
                            "emoji": true
                        },
                        "value": "Bug"
                    },
                    {
                        "text": {
                            "type": "plain_text",
                            "text": "Story",
                            "emoji": true
                        },
                        "value": "Story"
                    }
                ]
			}
		},
		{
			"type": "input",
			"block_id": "feedback_message_input_block",
			"element": {
				"type": "plain_text_input",
                "multiline": true,
				"action_id": "feedback_message_input",
				"placeholder": {
					"type": "plain_text",
					"text": "Hi ${"#reporter".replace(
        "#",
        "$",
    )}! Thanks for reporting this. We've automatically created a Jira issue for you: ${"#issue".replace("#", "$")}."
				}
			},
			"label": {
				"type": "plain_text",
				"text": "Feedback Message",
				"emoji": true
			},
			"hint": {
				"type": "plain_text",
				"text": "You can use variables: ${"issue,reporter".split(",").joinToString(", ") { "$$it" }}"
			}
		},
        {
			"type": "section",
			"block_id": "send_csat_input_block",
			"text": {
				"type": "mrkdwn",
				"text": ":chart_with_upwards_trend: *Send Customer Satisfaction survey*\n It will send a CSat survey to the reporter of the issue"
			},
			"accessory": {
				"type": "radio_buttons",
				"action_id": "send_csat_input",
				"initial_option": {
					"text": {
						"type": "plain_text",
						"text": "Yes"
					},
					"value": "Yes"
				},
				"options": [
					{
						"text": {
							"type": "plain_text",
							"text": "Yes"
						},
						"value": "Yes"
					},
					{
						"text": {
							"type": "plain_text",
							"text": "No"
						},
						"value": "No"
					}
				]
			}
		},
		{
			"type": "section",
			"block_id": "use_ai_summarizer_input_block",
			"text": {
				"type": "mrkdwn",
				"text": ":robot_face: *AI Summarizer for Title*\n Use AI to generate smart titles for Jira tickets based on message content"
			},
			"accessory": {
				"type": "checkboxes",
				"action_id": "use_ai_summarizer_input",
				"options": [
					{
						"text": {
							"type": "plain_text",
							"text": "Enable AI-generated titles"
						},
						"value": "enabled"
					}
				]
			}
		},
		{
			"type": "input",
			"block_id": "labels_input_block",
			"optional": true,
			"element": {
				"type": "plain_text_input",
				"action_id": "labels_input",
				"placeholder": {
					"type": "plain_text",
					"text": "Enter labels separated by commas (Optional)"
				}
			},
			"label": {
				"type": "plain_text",
				"text": "Labels",
				"emoji": true
			}
		}
	]
}
    """
