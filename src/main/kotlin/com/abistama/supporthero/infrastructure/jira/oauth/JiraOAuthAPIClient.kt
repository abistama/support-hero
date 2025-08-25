package com.abistama.supporthero.infrastructure.jira.oauth

import com.abistama.supporthero.infrastructure.jira.JiraBadRequestException
import com.abistama.supporthero.infrastructure.jira.JiraInternalErrorException
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import kotlin.jvm.Throws

@FeignClient(name = "jiraOAuthClient", url = "https://auth.atlassian.com/oauth/token")
interface JiraOAuthAPIClient {
    @PostMapping(consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    @Throws(JiraBadRequestException::class, JiraInternalErrorException::class)
    fun getInitialAccessToken(
        @RequestBody request: JiraOAuthInitialTokenRequest,
    ): JiraOAuthTokenResponse

    @PostMapping(consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    @Throws(JiraBadRequestException::class, JiraInternalErrorException::class)
    fun getRefreshableAccessToken(
        @RequestBody request: JiraOAuthRefreshableTokenRequest,
    ): JiraOAuthTokenResponse
}
