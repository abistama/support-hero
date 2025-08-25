package com.abistama.supporthero.infrastructure.jira

import com.abistama.supporthero.infrastructure.jira.events.JiraIssueResponse
import com.abistama.supporthero.infrastructure.repository.JiraCloudId
import com.abistama.supporthero.infrastructure.slack.events.SlackViewSubmissionEvent
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

class Fixtures {
    private val objectMapper = jacksonObjectMapper()

    fun getJiraIssueResponseFixture(
        key: String? = null,
        status: String? = null,
    ): JiraIssueResponse {
        val uri =
            this::class.java.classLoader
                .getResource("jira-issue-response-real.json")
                ?.toURI()
        val json = uri?.let { Paths.get(it) }?.let { Files.readAllLines(it) }?.joinToString("\n") ?: ""
        val issue = objectMapper.readValue(json, JiraIssueResponse::class.java)

        if (key != null && status != null) {
            return issue.copy(key = key, fields = issue.fields.copy(status = issue.fields.status.copy(name = status)))
        } else if (key != null) {
            return issue.copy(key = key)
        } else if (status != null) {
            return issue.copy(fields = issue.fields.copy(status = issue.fields.status.copy(name = status)))
        }
        return issue
    }

    fun getSlackViewSubmissionEvent(): SlackViewSubmissionEvent {
        val uri =
            this::class.java.classLoader
                .getResource("view-submission-event.json")
                ?.toURI()
        val json = uri?.let { Paths.get(it) }?.let { Files.readAllLines(it) }?.joinToString("\n") ?: ""
        return objectMapper.readValue(json, SlackViewSubmissionEvent::class.java)
    }

    fun getJiraCloudIdFixture(uuid: UUID? = null): JiraCloudId = JiraCloudId(uuid ?: UUID.randomUUID())
}
