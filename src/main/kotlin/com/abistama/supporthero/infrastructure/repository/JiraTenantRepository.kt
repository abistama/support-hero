package com.abistama.supporthero.infrastructure.repository

import com.abistama.supporthero.domain.jira.JiraCredentials
import com.abistama.supporthero.infrastructure.generated.tables.references.JIRA_TENANT
import com.abistama.supporthero.infrastructure.generated.tables.references.SLACK_TO_JIRA_TENANT
import com.abistama.supporthero.infrastructure.security.TenantEncryptionService
import org.jooq.DSLContext
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

class JiraTenantRepository(
    private val dslContext: DSLContext,
    private val clock: Clock,
    private val encryptionService: TenantEncryptionService,
) {
    fun add(jiraCredentials: JiraCredentials): Int {
        val now = LocalDateTime.now(clock)
        val tenantId = jiraCredentials.jiraCloudId.value.toString()
        val encryptedAccessToken = encryptionService.encryptToken(jiraCredentials.accessToken, tenantId)
        val encryptedRefreshToken = encryptionService.encryptToken(jiraCredentials.refreshToken, tenantId)

        return dslContext
            .insertInto(JIRA_TENANT)
            .set(JIRA_TENANT.ACCESS_TOKEN, encryptedAccessToken)
            .set(JIRA_TENANT.REFRESH_TOKEN, encryptedRefreshToken)
            .set(JIRA_TENANT.CLOUD_ID, tenantId)
            .set(
                JIRA_TENANT.EXPIRES_IN,
                jiraCredentials.expires,
            ).set(JIRA_TENANT.SCOPES, jiraCredentials.scopes)
            .set(JIRA_TENANT.CREATED_AT, now)
            .set(JIRA_TENANT.UPDATED_AT, now)
            .onConflict(JIRA_TENANT.CLOUD_ID)
            .doUpdate()
            .set(JIRA_TENANT.ACCESS_TOKEN, encryptedAccessToken)
            .set(JIRA_TENANT.REFRESH_TOKEN, encryptedRefreshToken)
            .set(JIRA_TENANT.EXPIRES_IN, jiraCredentials.expires)
            .set(JIRA_TENANT.SCOPES, jiraCredentials.scopes)
            .set(JIRA_TENANT.UPDATED_AT, now)
            .execute()
    }

    fun get(jiraCloudId: JiraCloudId): JiraCredentials? =
        dslContext
            .selectFrom(JIRA_TENANT)
            .where(JIRA_TENANT.CLOUD_ID.eq(jiraCloudId.value.toString()))
            .fetchOne {
                val tenantId = jiraCloudId.value.toString()
                val encryptedAccessToken = it.get(JIRA_TENANT.ACCESS_TOKEN)!!
                val encryptedRefreshToken = it.get(JIRA_TENANT.REFRESH_TOKEN)!!

                JiraCredentials(
                    jiraCloudId,
                    encryptionService.decryptToken(encryptedAccessToken, tenantId),
                    it.get(JIRA_TENANT.EXPIRES_IN)!!,
                    it.get(JIRA_TENANT.SCOPES)!!,
                    encryptionService.decryptToken(encryptedRefreshToken, tenantId),
                )
            }

    fun getJiraCloudId(slackToJiraId: UUID): JiraCloudId? =
        dslContext
            .select(JIRA_TENANT.CLOUD_ID)
            .from(JIRA_TENANT)
            .join(SLACK_TO_JIRA_TENANT)
            .on(JIRA_TENANT.ID.eq(SLACK_TO_JIRA_TENANT.JIRA_TENANT_ID))
            .where(SLACK_TO_JIRA_TENANT.ID.eq(slackToJiraId))
            .fetchOne { JiraCloudId(UUID.fromString(it[JIRA_TENANT.CLOUD_ID])) }
}
