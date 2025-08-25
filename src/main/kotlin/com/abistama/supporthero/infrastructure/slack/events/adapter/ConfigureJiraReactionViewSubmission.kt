package com.abistama.supporthero.infrastructure.slack.events.adapter

import com.abistama.supporthero.domain.slack.SlackUserOrGroupId
import com.abistama.supporthero.infrastructure.jira.IssueType
import com.abistama.supporthero.infrastructure.jira.Project
import com.abistama.supporthero.infrastructure.slack.events.SlackViewSubmissionEvent

data class ConfigureJiraReactionViewSubmission internal constructor(
    val project: Project,
    val reaction: String,
    val userOrGroupId: SlackUserOrGroupId,
    val sendCsat: Boolean,
    val issueType: IssueType,
    val feedbackMessage: String,
    val useAiSummarizer: Boolean,
) {
    companion object {
        fun SlackViewSubmissionEvent.toConfigureJiraReactionViewSubmission(): ConfigureJiraReactionViewSubmission {
            val project =
                Project(
                    this.view.state
                        .get<SlackViewSubmissionEvent.StaticSelect>("project")
                        .selectedOption.value,
                )
            val reaction =
                this.view.state
                    .get<SlackViewSubmissionEvent.StaticSelect>("reaction")
                    .selectedOption.value
            val userOrGroupId =
                SlackUserOrGroupId(
                    this.view.state
                        .get<SlackViewSubmissionEvent.UsersSelect>("user_or_group_id")
                        .selectedUser,
                )
            val issueType =
                IssueType(
                    this.view.state
                        .get<SlackViewSubmissionEvent.StaticSelect>("jira_issue_type")
                        .selectedOption.value,
                )

            val sendCsat =
                this.view.state
                    .get<SlackViewSubmissionEvent.RadioButtons>("send_csat")
                    .selectedOption.value == "Yes"

            val feedbackMessage =
                this.view.state
                    .get<SlackViewSubmissionEvent.PlainTextInput>(
                        "feedback_message",
                    ).value
                    .orEmpty()

            val useAiSummarizer =
                this.view.state
                    .get<SlackViewSubmissionEvent.Checkboxes>("use_ai_summarizer")
                    .selectedOptions
                    .any { it.value == "enabled" }

            return ConfigureJiraReactionViewSubmission(
                project,
                reaction,
                userOrGroupId,
                sendCsat,
                issueType,
                feedbackMessage,
                useAiSummarizer,
            )
        }
    }
}
