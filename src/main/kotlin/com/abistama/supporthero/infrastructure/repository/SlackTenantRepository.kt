package com.abistama.supporthero.infrastructure.repository

import com.abistama.supporthero.application.slack.events.SlackTeamProcessedEvent
import com.abistama.supporthero.domain.slack.SlackAddTenant
import com.abistama.supporthero.domain.slack.SlackCredentials
import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.infrastructure.generated.tables.references.SLACK_TENANT
import com.abistama.supporthero.infrastructure.security.TenantEncryptionService
import org.jooq.DSLContext
import java.time.Clock
import java.time.LocalDateTime
import java.util.*

data class JiraCloudId(
    val value: UUID,
)

class SlackTenantRepository(
    private val dslContext: DSLContext,
    private val clock: Clock,
    private val encryptionService: TenantEncryptionService,
) {
    fun add(slackAddTenant: SlackAddTenant) {
        val now = LocalDateTime.now(clock)
        val tenantId = slackAddTenant.teamId.id
        val encryptedAccessToken = encryptionService.encryptToken(slackAddTenant.slackCredentials.accessToken, tenantId)
        val encryptedRefreshToken = encryptionService.encryptToken(slackAddTenant.slackCredentials.refreshToken, tenantId)

        dslContext
            .insertInto(SLACK_TENANT)
            .set(SLACK_TENANT.SLACK_TEAM_ID, tenantId)
            .set(SLACK_TENANT.SLACK_ENTERPRISE_ID, slackAddTenant.enterpriseId?.enterpriseId)
            .set(SLACK_TENANT.ACCESS_TOKEN, encryptedAccessToken)
            .set(SLACK_TENANT.TOKEN_TYPE, slackAddTenant.slackCredentials.tokenType)
            .set(SLACK_TENANT.REFRESH_TOKEN, encryptedRefreshToken)
            .set(
                SLACK_TENANT.EXPIRES_IN,
                slackAddTenant.slackCredentials.expires,
            ).set(SLACK_TENANT.SCOPES, slackAddTenant.slackCredentials.scopes)
            .set(SLACK_TENANT.CREATED_AT, now)
            .set(SLACK_TENANT.UPDATED_AT, now)
            .onConflict(SLACK_TENANT.SLACK_TEAM_ID)
            .doUpdate()
            .set(SLACK_TENANT.ACCESS_TOKEN, encryptedAccessToken)
            .set(SLACK_TENANT.TOKEN_TYPE, slackAddTenant.slackCredentials.tokenType)
            .set(SLACK_TENANT.REFRESH_TOKEN, encryptedRefreshToken)
            .set(
                SLACK_TENANT.EXPIRES_IN,
                slackAddTenant.slackCredentials.expires,
            ).set(SLACK_TENANT.SCOPES, slackAddTenant.slackCredentials.scopes)
            .set(SLACK_TENANT.UPDATED_AT, now)
            .execute()
    }

    fun updateToken(
        slackTeamId: SlackTeamId,
        refreshedCredentials: SlackCredentials,
    ) {
        val now = LocalDateTime.now(clock)
        val tenantId = slackTeamId.id
        val encryptedAccessToken = encryptionService.encryptToken(refreshedCredentials.accessToken, tenantId)
        val encryptedRefreshToken = encryptionService.encryptToken(refreshedCredentials.refreshToken, tenantId)

        dslContext
            .update(SLACK_TENANT)
            .set(SLACK_TENANT.ACCESS_TOKEN, encryptedAccessToken)
            .set(SLACK_TENANT.REFRESH_TOKEN, encryptedRefreshToken)
            .set(SLACK_TENANT.TOKEN_TYPE, refreshedCredentials.tokenType)
            .set(
                SLACK_TENANT.EXPIRES_IN,
                refreshedCredentials.expires,
            ).set(SLACK_TENANT.SCOPES, refreshedCredentials.scopes)
            .set(SLACK_TENANT.UPDATED_AT, now)
            .where(
                SLACK_TENANT.SLACK_TEAM_ID
                    .eq(
                        tenantId,
                    ),
            ).execute()
    }

    fun get(slackTeamId: SlackTeamId): SlackCredentials? =
        dslContext
            .select(
                SLACK_TENANT.ACCESS_TOKEN,
                SLACK_TENANT.REFRESH_TOKEN,
                SLACK_TENANT.TOKEN_TYPE,
                SLACK_TENANT.EXPIRES_IN,
                SLACK_TENANT.SCOPES,
            ).from(SLACK_TENANT)
            .where(SLACK_TENANT.SLACK_TEAM_ID.eq(slackTeamId.id))
            .fetchOne()
            ?.let {
                val tenantId = slackTeamId.id
                val encryptedAccessToken = it[SLACK_TENANT.ACCESS_TOKEN]!!
                val encryptedRefreshToken = it[SLACK_TENANT.REFRESH_TOKEN]!!

                SlackCredentials(
                    encryptionService.decryptToken(encryptedAccessToken, tenantId),
                    it[SLACK_TENANT.TOKEN_TYPE]!!,
                    it[SLACK_TENANT.EXPIRES_IN]!!,
                    it[SLACK_TENANT.SCOPES]!!,
                    encryptionService.decryptToken(encryptedRefreshToken, tenantId),
                )
            }

    fun getAllSlackTeamIds(): Collection<SlackTeamId> =
        dslContext
            .select(SLACK_TENANT.SLACK_TEAM_ID)
            .from(SLACK_TENANT)
            .fetch()
            .map { SlackTeamId((it[SLACK_TENANT.SLACK_TEAM_ID]!!)) }

    fun updateDomain(event: SlackTeamProcessedEvent) {
        dslContext
            .update(SLACK_TENANT)
            .set(SLACK_TENANT.DOMAIN, event.domain.value)
            .set(SLACK_TENANT.UPDATED_AT, LocalDateTime.now(clock))
            .where(SLACK_TENANT.SLACK_TEAM_ID.eq(event.teamId.id))
            .execute()
    }
}
