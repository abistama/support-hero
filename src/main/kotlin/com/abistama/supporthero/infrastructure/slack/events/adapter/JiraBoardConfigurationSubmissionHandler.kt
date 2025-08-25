package com.abistama.supporthero.infrastructure.slack.events.adapter

import com.abistama.supporthero.infrastructure.repository.SlackToJiraConfigurationRepository

class JiraBoardConfigurationSubmissionHandler(
    private val slackToJiraConfigurationRepository: SlackToJiraConfigurationRepository,
) {
    fun handle(event: ConfigureJiraReactionViewSubmission) {
        /*
            open val project: Project,
    open val reaction: String,
    open val feedbackMessage: String,
    open val issueType: IssueType,
        slackToJiraConfigurationRepository.add(
            JiraBoardConfiguration(
                Project(event.projectKey),
                event.projectKey,
                event.reaction,
                event.userOrGroupId,
                event.feedbackMessage,
            ),
        )
         */
    }
}
