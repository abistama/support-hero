package com.abistama.supporthero.infrastructure.jira.repository

import com.abistama.supporthero.application.slackJira.SlackToJiraPendingTickedId
import com.abistama.supporthero.application.slackJira.SlackToJiraPendingTicket
import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.SlackUserId
import com.abistama.supporthero.infrastructure.generated.tables.references.JIRA_TENANT
import com.abistama.supporthero.infrastructure.generated.tables.references.JIRA_TICKET_TRACKING
import com.abistama.supporthero.infrastructure.generated.tables.references.SLACK_TENANT
import com.abistama.supporthero.infrastructure.generated.tables.references.SLACK_TO_JIRA_TENANT
import com.abistama.supporthero.infrastructure.jira.events.JiraIssueCreatedEvent
import com.abistama.supporthero.infrastructure.jira.events.JiraIssueResponse
import com.abistama.supporthero.infrastructure.repository.JiraCloudId
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import org.jooq.DSLContext
import org.jooq.JSONB
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.jsonbGetAttributeAsText
import org.jooq.impl.DSL.jsonbValue
import java.time.Clock
import java.time.LocalDateTime
import java.util.*

class JiraTicketTrackingRepository(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
) {
    companion object : KLogging()

    fun add(
        event: JiraIssueCreatedEvent,
        jiraIssueResponse: JiraIssueResponse,
    ) {
        val now = LocalDateTime.now(clock)
        val jiraIssueResponseJson = objectMapper.writeValueAsString(jiraIssueResponse)
        dslContext
            .insertInto(JIRA_TICKET_TRACKING)
            .set(
                JIRA_TICKET_TRACKING.SLACK_TO_JIRA_TENANT_ID,
                dslContext
                    .select(SLACK_TO_JIRA_TENANT.ID)
                    .from(SLACK_TO_JIRA_TENANT)
                    .join(JIRA_TENANT)
                    .on(JIRA_TENANT.ID.eq(SLACK_TO_JIRA_TENANT.JIRA_TENANT_ID))
                    .where(JIRA_TENANT.CLOUD_ID.eq(event.jiraCloudId.value.toString())),
            ).set(JIRA_TICKET_TRACKING.JIRA_ISSUE, jiraIssueResponse.key)
            .set(JIRA_TICKET_TRACKING.SEND_CSAT, event.sendCsat)
            .set(JIRA_TICKET_TRACKING.FULL_ISSUE, JSONB.valueOf(jiraIssueResponseJson))
            .set(JIRA_TICKET_TRACKING.REPORTED_BY, event.reportedBy.id)
            .set(JIRA_TICKET_TRACKING.CREATED_AT, now)
            .set(JIRA_TICKET_TRACKING.UPDATED_AT, now)
            .execute()
    }

    fun getPending(): List<SlackToJiraPendingTicket> =
        dslContext
            .select(
                JIRA_TICKET_TRACKING.FULL_ISSUE,
                JIRA_TENANT.CLOUD_ID,
                SLACK_TENANT.SLACK_TEAM_ID,
                JIRA_TICKET_TRACKING.ID,
                JIRA_TICKET_TRACKING.REPORTED_BY,
            ).from(JIRA_TICKET_TRACKING)
            .join(SLACK_TO_JIRA_TENANT)
            .on(SLACK_TO_JIRA_TENANT.ID.eq(JIRA_TICKET_TRACKING.SLACK_TO_JIRA_TENANT_ID))
            .join(JIRA_TENANT)
            .on(JIRA_TENANT.ID.eq(SLACK_TO_JIRA_TENANT.JIRA_TENANT_ID))
            .join(SLACK_TENANT)
            .on(SLACK_TENANT.ID.eq(SLACK_TO_JIRA_TENANT.SLACK_TENANT_ID))
            .where(JIRA_TICKET_TRACKING.SEND_CSAT.isTrue)
            .and(jsonbGetAttributeAsText(jsonbValue(JIRA_TICKET_TRACKING.FULL_ISSUE, "$.fields.status"), "name").notEqual("Done"))
            .fetch {
                val jiraIssue = objectMapper.readValue(it.value1()!!.data(), JiraIssueResponse::class.java)
                val cloudId = JiraCloudId(UUID.fromString(it.value2()!!))
                val slackTeamId = SlackTeamId(it.value3()!!)
                SlackToJiraPendingTicket(
                    cloudId,
                    slackTeamId,
                    SlackToJiraPendingTickedId(it.value4()!!),
                    jiraIssue,
                    SlackUserId(it.value5()!!),
                )
            }

    fun updateStatus(
        slackToJiraPendingTickedId: SlackToJiraPendingTickedId,
        status: String,
    ) {
        dslContext
            .update(JIRA_TICKET_TRACKING)
            .set(
                JIRA_TICKET_TRACKING.FULL_ISSUE,
                field("jsonb_set(full_issue::JSONB, '{fields,status,name}', '\"$status\"'::JSONB)", JSONB::class.java),
            ).where(JIRA_TICKET_TRACKING.ID.eq(slackToJiraPendingTickedId.value))
            .execute()
    }
}
