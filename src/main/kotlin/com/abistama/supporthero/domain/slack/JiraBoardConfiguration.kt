package com.abistama.supporthero.domain.slack

import com.abistama.supporthero.infrastructure.jira.IssueType
import com.abistama.supporthero.infrastructure.jira.Project
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = JiraBoardConfiguration.UserConfiguration::class, name = "user"),
    JsonSubTypes.Type(value = JiraBoardConfiguration.UserGroupConfiguration::class, name = "userGroup"),
)
sealed class JiraBoardConfiguration(
    open val project: Project,
    open val reaction: String,
    open val feedbackMessage: String,
    open val issueType: IssueType,
    open val sendCsat: Boolean,
    open val useAiSummarizer: Boolean = false,
    open val channelId: SlackChannelId? = null,
    open val components: List<String>? = null,
    open val labels: List<String>? = null,
) {
    data class UserConfiguration(
        override val project: Project,
        override val reaction: String,
        override val feedbackMessage: String,
        override val issueType: IssueType,
        val user: SlackUserId,
        override val sendCsat: Boolean,
        override val useAiSummarizer: Boolean = false,
        override val channelId: SlackChannelId? = null,
        override val components: List<String>? = null,
        override val labels: List<String>? = null,
    ) : JiraBoardConfiguration(project, reaction, feedbackMessage, issueType, sendCsat, useAiSummarizer, channelId, components, labels)

    data class UserGroupConfiguration(
        override val project: Project,
        override val reaction: String,
        override val feedbackMessage: String,
        override val issueType: IssueType,
        val userGroup: SlackUserGroupId,
        override val sendCsat: Boolean,
        override val useAiSummarizer: Boolean = false,
        override val channelId: SlackChannelId? = null,
        override val components: List<String>? = null,
        override val labels: List<String>? = null,
    ) : JiraBoardConfiguration(project, reaction, feedbackMessage, issueType, sendCsat, useAiSummarizer, channelId, components, labels)
}
