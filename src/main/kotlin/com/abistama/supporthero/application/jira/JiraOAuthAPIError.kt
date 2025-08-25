package com.abistama.supporthero.application.jira

data class JiraOAuthAPIError(
    val message: String,
)

fun fromException(e: Throwable): JiraOAuthAPIError = JiraOAuthAPIError(e.message ?: "Unknown error")
