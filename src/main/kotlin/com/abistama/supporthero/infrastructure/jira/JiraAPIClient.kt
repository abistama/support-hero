package com.abistama.supporthero.infrastructure.jira

import com.abistama.supporthero.infrastructure.jira.events.JiraIssueResponse
import com.abistama.supporthero.infrastructure.jira.oauth.JiraAccessibleResourcesResponse
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import feign.Headers
import feign.Param
import feign.RequestLine
import kotlin.jvm.Throws

@Headers("Content-Type: application/json")
interface JiraAPIClient {
    @RequestLine("GET /oauth/token/accessible-resources")
    @Headers("Authorization: Bearer {access_token}")
    @Throws(JiraBadRequestException::class, JiraInternalErrorException::class)
    fun getAccessibleResources(
        @Param("access_token") accessToken: String,
    ): List<JiraAccessibleResourcesResponse>

    @RequestLine("POST /ex/jira/{cloudid}/rest/api/2/issue")
    @Headers("Authorization: Bearer {access_token}")
    @Throws(JiraBadRequestException::class, JiraInternalErrorException::class)
    fun createIssue(
        @Param("access_token") accessToken: String,
        @Param("cloudid") cloudId: String,
        issue: Issue,
    ): IssueResponse

    @RequestLine("GET /ex/jira/{cloudid}/rest/api/2/issue/{issueKey}")
    @Headers("Authorization: Bearer {access_token}")
    @Throws(JiraBadRequestException::class, JiraInternalErrorException::class)
    fun getIssue(
        @Param("access_token") accessToken: String,
        @Param("cloudid") cloudId: String,
        @Param("issueKey") issueKey: String,
    ): JiraIssueResponse

    @RequestLine("GET /ex/jira/{cloudid}/rest/api/2/project/{projectKey}/component")
    @Headers("Authorization: Bearer {access_token}")
    @Throws(JiraBadRequestException::class, JiraInternalErrorException::class)
    fun getComponents(
        @Param("access_token") accessToken: String,
        @Param("cloudid") cloudId: String,
        @Param("projectKey") projectKey: String,
    ): List<JiraComponent>

    @RequestLine("GET /ex/jira/{cloudid}/rest/api/2/label")
    @Headers("Authorization: Bearer {access_token}")
    @Throws(JiraBadRequestException::class, JiraInternalErrorException::class)
    fun getLabels(
        @Param("access_token") accessToken: String,
        @Param("cloudid") cloudId: String,
    ): JiraLabelResponse

    @RequestLine("GET /ex/jira/{cloudid}/rest/api/2/project")
    @Headers("Authorization: Bearer {access_token}")
    @Throws(JiraBadRequestException::class, JiraInternalErrorException::class)
    fun getProjects(
        @Param("access_token") accessToken: String,
        @Param("cloudid") cloudId: String,
    ): List<Project>
}

data class Issue(
    val fields: IssueFields,
)

data class IssueFields(
    val project: Project,
    val summary: String,
    val description: String,
    val issuetype: IssueType,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Project(
    val key: String,
    val name: String? = null,
)

data class IssueType(
    val name: String,
)

data class IssueResponse(
    val id: String,
    val key: String,
    val self: String,
)

data class JiraComponent(
    val id: String,
    val name: String,
)

data class JiraLabelResponse(
    val values: Set<String>,
)
