package com.abistama.supporthero.domain.slack

data class SlackAddTenant(
    val teamId: SlackTeamId,
    val enterpriseId: SlackEnterpriseId,
    val slackCredentials: SlackCredentials,
)
