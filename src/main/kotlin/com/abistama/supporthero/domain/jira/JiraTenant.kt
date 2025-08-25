package com.abistama.supporthero.domain.jira

import com.abistama.supporthero.infrastructure.repository.JiraCloudId

data class JiraTenant(
    val cloudId: JiraCloudId,
    val accessToken: String,
)
