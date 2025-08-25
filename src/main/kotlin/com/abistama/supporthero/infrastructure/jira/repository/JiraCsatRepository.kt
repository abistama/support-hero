package com.abistama.supporthero.infrastructure.jira.repository

import com.abistama.supporthero.domain.jira.SlackToJiraCsat
import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.SlackUserId
import com.abistama.supporthero.infrastructure.generated.tables.references.JIRA_TENANT
import com.abistama.supporthero.infrastructure.generated.tables.references.JIRA_TICKET_CSAT
import com.abistama.supporthero.infrastructure.generated.tables.references.SLACK_TENANT
import com.abistama.supporthero.infrastructure.generated.tables.references.SLACK_TO_JIRA_TENANT
import com.abistama.supporthero.infrastructure.repository.JiraCloudId
import org.jooq.DSLContext
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

class JiraCsatRepository(
    private val dslContext: DSLContext,
    private val clock: Clock,
) {
    fun getReminders(): List<SlackToJiraCsat> {
        val now = LocalDateTime.now(clock)
        return dslContext
            .select(
                JIRA_TICKET_CSAT.ID,
                JIRA_TICKET_CSAT.SEND_TO,
                SLACK_TENANT.SLACK_TEAM_ID,
                JIRA_TICKET_CSAT.TICKET_KEY,
            ).from(JIRA_TICKET_CSAT)
            .join(SLACK_TO_JIRA_TENANT)
            .on(SLACK_TO_JIRA_TENANT.ID.eq(JIRA_TICKET_CSAT.SLACK_TO_JIRA_TENANT_ID))
            .join(JIRA_TENANT)
            .on(JIRA_TENANT.ID.eq(SLACK_TO_JIRA_TENANT.JIRA_TENANT_ID))
            .join(SLACK_TENANT)
            .on(SLACK_TENANT.ID.eq(SLACK_TO_JIRA_TENANT.SLACK_TENANT_ID))
            .where(
                JIRA_TICKET_CSAT.NUMBER_REMINDERS
                    .greaterThan(0)
                    .and(JIRA_TICKET_CSAT.CSAT_VALUE.isNull)
                    .and(JIRA_TICKET_CSAT.NEXT_REMINDER_AT.lessOrEqual(now)),
            ).fetch {
                SlackToJiraCsat(
                    it.value1()!!,
                    SlackUserId(it.value2()!!),
                    SlackTeamId(
                        it.value3()!!,
                    ),
                    it.value4()!!,
                )
            }
    }

    fun decreaseReminderAttempts(
        jiraCsatId: UUID,
        nextReminderIn: Duration,
    ) {
        val now = LocalDateTime.now(clock)
        dslContext
            .update(JIRA_TICKET_CSAT)
            .set(JIRA_TICKET_CSAT.NUMBER_REMINDERS, JIRA_TICKET_CSAT.NUMBER_REMINDERS.minus(1))
            .set(JIRA_TICKET_CSAT.NEXT_REMINDER_AT, now.plus(nextReminderIn))
            .set(JIRA_TICKET_CSAT.UPDATED_AT, now)
            .where(JIRA_TICKET_CSAT.ID.eq(jiraCsatId))
            .execute()
    }

    fun createCsat(
        jiraCloudId: JiraCloudId,
        sendTo: SlackUserId,
        ticketKey: String,
        projectKey: String,
        numberOfReminders: Int,
        nextReminderIn: Duration,
    ): Int {
        val now = LocalDateTime.now(clock)
        return dslContext
            .insertInto(JIRA_TICKET_CSAT)
            .columns(
                JIRA_TICKET_CSAT.SLACK_TO_JIRA_TENANT_ID,
                JIRA_TICKET_CSAT.SEND_TO,
                JIRA_TICKET_CSAT.TICKET_KEY,
                JIRA_TICKET_CSAT.PROJECT_KEY,
                JIRA_TICKET_CSAT.NUMBER_REMINDERS,
                JIRA_TICKET_CSAT.NEXT_REMINDER_AT,
                JIRA_TICKET_CSAT.CREATED_AT,
                JIRA_TICKET_CSAT.UPDATED_AT,
            ).values(
                dslContext
                    .select(SLACK_TO_JIRA_TENANT.ID)
                    .from(SLACK_TO_JIRA_TENANT)
                    .join(JIRA_TENANT)
                    .on(JIRA_TENANT.ID.eq(SLACK_TO_JIRA_TENANT.JIRA_TENANT_ID))
                    .where(JIRA_TENANT.CLOUD_ID.eq(jiraCloudId.value.toString()))
                    .fetchOne()
                    ?.get(SLACK_TO_JIRA_TENANT.ID)!!,
                sendTo.id,
                ticketKey,
                projectKey,
                numberOfReminders,
                now.plus(nextReminderIn),
                now,
                now,
            ).execute()
    }

    fun updateCsatValue(
        jiraCsatId: UUID,
        csatValue: Int,
    ) {
        dslContext
            .update(JIRA_TICKET_CSAT)
            .set(JIRA_TICKET_CSAT.CSAT_VALUE, csatValue)
            .set(JIRA_TICKET_CSAT.UPDATED_AT, LocalDateTime.now(clock))
            .where(JIRA_TICKET_CSAT.ID.eq(jiraCsatId))
            .execute()
    }
}
