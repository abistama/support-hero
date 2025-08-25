package com.abistama.supporthero.infrastructure.jira.oauth

data class JiraAccessibleResourcesResponse(
    val id: String,
    val name: String,
    val url: String,
    val scopes: List<String>,
    val avatarUrl: String,
)
