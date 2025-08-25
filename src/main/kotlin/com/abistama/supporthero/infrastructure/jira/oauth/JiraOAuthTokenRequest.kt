package com.abistama.supporthero.infrastructure.jira.oauth

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

data class JiraOAuthInitialTokenRequest(
    @JsonProperty("code") val code: String,
    @JsonProperty("client_id") val clientId: String,
    @JsonProperty("client_secret") val clientSecret: String,
    @JsonProperty("redirect_uri") val redirectUri: String,
    @JsonProperty("grant_type") val grantType: String = "authorization_code",
)

data class JiraOAuthRefreshableTokenRequest(
    @JsonProperty("client_id") val clientId: String,
    @JsonProperty("client_secret") val clientSecret: String,
    @JsonProperty("refresh_token") val refreshToken: String,
    @JsonProperty("grant_type") val grantType: String = "refresh_token",
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class JiraOAuthTokenResponse(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("expires_in") val expiresIn: Int,
    @JsonProperty("scope") val scopes: String,
    @JsonProperty("refresh_token") val refreshToken: String,
)
