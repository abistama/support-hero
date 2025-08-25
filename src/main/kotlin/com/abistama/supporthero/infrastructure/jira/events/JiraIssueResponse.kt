package com.abistama.supporthero.infrastructure.jira.events

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class JiraIssueResponse(
    val fields: Fields,
    val id: String,
    val key: String,
    val self: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Fields(
    val attachment: List<Attachment>,
    val description: String,
    val project: Project,
    val comment: CommentWrapper,
    val reporter: Author,
    @JsonProperty("issuelinks")
    val issueLinks: List<IssueLink>,
    val status: IssueStatus,
    val worklog: WorklogWrapper,
    val updated: String,
    val timetracking: TimeTracking,
    val watcher: Watcher? = null,
    @JsonProperty("sub-tasks")
    val subTasks: List<SubTask>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class IssueStatus(
    val self: String,
    val description: String,
    val iconUrl: String,
    val name: String,
    val id: String,
    val statusCategory: StatusCategory,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class StatusCategory(
    val self: String,
    val id: Int,
    val key: String,
    val colorName: String,
    val name: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Watcher(
    @JsonProperty("isWatching")
    val isWatching: Boolean,
    val self: String,
    @JsonProperty("watchCount")
    val watchCount: Int,
    val watchers: List<WatcherUser>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WatcherUser(
    val accountId: String,
    val active: Boolean,
    val displayName: String,
    val self: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Attachment(
    val author: Author,
    val content: String,
    val created: String,
    val filename: String,
    val id: Int,
    val mimeType: String,
    val self: String,
    val size: Int,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Author(
    val accountId: String,
    val active: Boolean,
    val displayName: String,
    val self: String,
    @JsonProperty("emailAddress")
    val email: String? = null,
    val accountType: String? = null,
    val avatarUrls: AvatarUrls? = null,
    val key: String? = null,
    val name: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AvatarUrls(
    @JsonProperty("16x16")
    val x16: String,
    @JsonProperty("24x24")
    val x24: String,
    @JsonProperty("32x32")
    val x32: String,
    @JsonProperty("48x48")
    val x48: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SubTask(
    val id: String,
    val outwardIssue: OutwardIssue,
    val type: Type,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OutwardIssue(
    val fields: OutwardIssueFields,
    val id: String,
    val key: String,
    val self: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OutwardIssueFields(
    val status: Status,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Status(
    val iconUrl: String,
    val name: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Type(
    val id: String,
    val inward: String,
    val name: String,
    val outward: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Project(
    val id: String,
    val key: String,
    val name: String,
    val simplified: Boolean,
    val self: String,
    val projectCategory: ProjectCategory? = null,
    val avatarUrls: AvatarUrls? = null,
    val style: String? = null,
    val insight: Insight? = null,
)

data class Insight(
    @JsonProperty("lastIssueUpdateTime")
    val lastIssueUpdateTime: String,
    @JsonProperty("totalIssueCount")
    val totalIssueCount: Int,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProjectCategory(
    val description: String,
    val id: String,
    val name: String,
    val self: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CommentWrapper(
    val comments: List<Comment>,
    val maxResults: Int,
    val startAt: Int,
    val total: Int,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Comment(
    val author: Author,
    val body: String,
    val created: String,
    val id: String,
    val self: String,
    val updateAuthor: Author,
    val updated: String,
    val visibility: Visibility,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class IssueLink(
    val id: String,
    val outwardIssue: OutwardIssue?,
    val inwardIssue: InwardIssue?,
    val type: Type,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InwardIssue(
    val fields: InwardIssueFields,
    val id: String,
    val key: String,
    val self: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InwardIssueFields(
    val status: Status,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WorklogWrapper(
    val worklogs: List<Worklog>,
    val maxResults: Int,
    val startAt: Int,
    val total: Int,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Worklog(
    val author: Author,
    val comment: String,
    val id: String,
    val issueId: String,
    val self: String,
    val started: String,
    val timeSpent: String,
    val timeSpentSeconds: Int,
    val updateAuthor: Author,
    val updated: String,
    val visibility: Visibility,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Visibility(
    val identifier: String,
    val type: String,
    val value: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TimeTracking(
    val originalEstimate: String? = null,
    val originalEstimateSeconds: Int? = null,
    val remainingEstimate: String? = null,
    val remainingEstimateSeconds: Int? = null,
    val timeSpent: String? = null,
    val timeSpentSeconds: Int? = null,
)
