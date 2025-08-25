package com.abistama.supporthero.infrastructure.repository

import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.infrastructure.generated.tables.references.JIRA_TENANT
import com.abistama.supporthero.infrastructure.generated.tables.references.SLACK_TENANT
import com.abistama.supporthero.infrastructure.generated.tables.references.SLACK_TO_JIRA_TENANT
import org.jooq.DSLContext
import java.time.Clock
import java.time.LocalDateTime
import java.util.*

class SlackToJiraRepository(
    private val dslContext: DSLContext,
    private val clock: Clock,
) {
    fun add(
        slackTeamId: SlackTeamId,
        jiraCloudId: JiraCloudId,
    ): UUID? {
        val now = LocalDateTime.now(clock)
        return dslContext
            .insertInto(SLACK_TO_JIRA_TENANT)
            .set(
                SLACK_TO_JIRA_TENANT.SLACK_TENANT_ID,
                dslContext
                    .select(SLACK_TENANT.ID)
                    .from(SLACK_TENANT)
                    .where(SLACK_TENANT.SLACK_TEAM_ID.eq(slackTeamId.id))
                    .fetchOne()
                    ?.get(SLACK_TENANT.ID),
            ).set(
                SLACK_TO_JIRA_TENANT.JIRA_TENANT_ID,
                dslContext
                    .select(JIRA_TENANT.ID)
                    .from(JIRA_TENANT)
                    .where(JIRA_TENANT.CLOUD_ID.eq(jiraCloudId.value.toString()))
                    .fetchOne()
                    ?.get(JIRA_TENANT.ID),
            ).set(SLACK_TO_JIRA_TENANT.CREATED_AT, now)
            .set(SLACK_TO_JIRA_TENANT.UPDATED_AT, now)
            .onConflict(SLACK_TO_JIRA_TENANT.SLACK_TENANT_ID, SLACK_TO_JIRA_TENANT.JIRA_TENANT_ID)
            .doNothing()
            .returning(SLACK_TO_JIRA_TENANT.ID)
            .fetchOne {
                it[SLACK_TO_JIRA_TENANT.ID]
            }
    }

    fun get(slackTeamId: SlackTeamId): UUID? =
        dslContext
            .select(SLACK_TO_JIRA_TENANT.ID)
            .from(SLACK_TO_JIRA_TENANT)
            .join(SLACK_TENANT)
            .on(SLACK_TENANT.ID.eq(SLACK_TO_JIRA_TENANT.SLACK_TENANT_ID))
            .join(JIRA_TENANT)
            .on(JIRA_TENANT.ID.eq(SLACK_TO_JIRA_TENANT.JIRA_TENANT_ID))
            .where(
                SLACK_TENANT.SLACK_TEAM_ID.eq(slackTeamId.id),
            ).fetchOne { it[SLACK_TO_JIRA_TENANT.ID] }
}
