package com.abistama.supporthero.infrastructure.jira

sealed class JiraAPIException(
    override val message: String,
) : Exception(message)

class JiraBadRequestException(
    override val message: String,
) : JiraAPIException(message)

class JiraUnauthorizedException(
    override val message: String,
) : JiraAPIException(message)

class JiraInternalErrorException(
    override val message: String,
) : JiraAPIException(message)
