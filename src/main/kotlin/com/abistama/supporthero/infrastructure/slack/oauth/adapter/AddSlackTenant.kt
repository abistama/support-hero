package com.abistama.supporthero.infrastructure.slack.oauth.adapter

import com.abistama.supporthero.application.slackJira.events.JiraOnboardingStartedEvent
import com.abistama.supporthero.domain.slack.SlackAddTenant
import com.abistama.supporthero.domain.slack.SlackEnterpriseId
import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.SlackUserId
import com.abistama.supporthero.domain.slack.toSlackCredentials
import com.abistama.supporthero.infrastructure.repository.SlackTenantRepository
import com.slack.api.methods.response.oauth.OAuthV2AccessResponse
import org.springframework.context.ApplicationEventPublisher
import java.time.Clock

class AddSlackTenant(
    private val slackTenantRepository: SlackTenantRepository,
    private val clock: Clock,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    fun handle(response: OAuthV2AccessResponse) {
        val teamId = SlackTeamId(response.team.id)
        val enterpriseId = SlackEnterpriseId(response.enterprise?.id)
        slackTenantRepository.add(
            SlackAddTenant(
                teamId,
                enterpriseId,
                response.toSlackCredentials(clock),
            ),
        )

        applicationEventPublisher.publishEvent(
            JiraOnboardingStartedEvent(
                this,
                SlackUserId(response.authedUser.id),
                teamId,
            ),
        )
    }
}
