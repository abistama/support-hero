package com.abistama.supporthero.infrastructure.repository

import com.abistama.supporthero.domain.slack.JiraBoardConfiguration
import com.abistama.supporthero.domain.slack.SlackChannelId
import com.abistama.supporthero.domain.slack.SlackUserGroupId
import com.abistama.supporthero.domain.slack.SlackUserId
import com.abistama.supporthero.domain.slackToJira.DisplayJiraBoards
import com.abistama.supporthero.domain.slackToJira.JiraBoardsStatistics
import com.abistama.supporthero.infrastructure.generated.tables.records.JiraTenantRecord
import com.abistama.supporthero.infrastructure.generated.tables.records.SlackToJiraConfigurationRecord
import com.abistama.supporthero.infrastructure.generated.tables.references.JIRA_TENANT
import com.abistama.supporthero.infrastructure.generated.tables.references.JIRA_TICKET_CSAT
import com.abistama.supporthero.infrastructure.generated.tables.references.SLACK_TO_JIRA_CONFIGURATION
import com.abistama.supporthero.infrastructure.generated.tables.references.SLACK_TO_JIRA_TENANT
import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.DSLContext
import org.jooq.JSONB
import org.jooq.impl.DSL.avg
import org.jooq.impl.DSL.row
import java.time.Clock
import java.time.LocalDateTime
import java.util.*

class SlackToJiraConfigurationRepository(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
) {
    fun add(
        slackToJiraTenantId: UUID,
        owner: SlackUserId,
        config: JiraBoardConfiguration,
    ) {
        val now = LocalDateTime.now(clock)
        val configJson = objectMapper.writeValueAsString(config)
        val userOrGroupId =
            when (config) {
                is JiraBoardConfiguration.UserConfiguration -> config.user.id
                is JiraBoardConfiguration.UserGroupConfiguration -> config.userGroup.id
            }
        dslContext
            .insertInto(SLACK_TO_JIRA_CONFIGURATION)
            .set(SLACK_TO_JIRA_CONFIGURATION.SLACK_TO_JIRA_TENANT_ID, slackToJiraTenantId)
            .set(SLACK_TO_JIRA_CONFIGURATION.SLACK_USER_OR_GROUP_ID, userOrGroupId)
            .set(SLACK_TO_JIRA_CONFIGURATION.CHANNEL_ID, config.channelId?.id)
            .set(SLACK_TO_JIRA_CONFIGURATION.REACTION, config.reaction)
            .set(SLACK_TO_JIRA_CONFIGURATION.CONFIGURATION, JSONB.valueOf(configJson))
            .set(SLACK_TO_JIRA_CONFIGURATION.CREATED_AT, now)
            .set(SLACK_TO_JIRA_CONFIGURATION.UPDATED_AT, now)
            .set(SLACK_TO_JIRA_CONFIGURATION.OWNER, owner.id)
            .execute()
    }

    fun getSlackToJiraConfig(
        userId: SlackUserId,
        slackUserGroupIds: Collection<SlackUserGroupId>,
        reaction: String,
        slackChannelId: SlackChannelId,
    ): Pair<JiraCloudId, JiraBoardConfiguration>? {
        val withChannelId =
            dslContext
                .select(
                    row(
                        SLACK_TO_JIRA_CONFIGURATION.SLACK_TO_JIRA_TENANT_ID,
                        SLACK_TO_JIRA_CONFIGURATION.SLACK_USER_OR_GROUP_ID,
                        SLACK_TO_JIRA_CONFIGURATION.REACTION,
                        SLACK_TO_JIRA_CONFIGURATION.CHANNEL_ID,
                        SLACK_TO_JIRA_CONFIGURATION.CONFIGURATION,
                    ).mapping { tenantId, userOrGroupId, r, channelId, configuration ->
                        SlackToJiraConfigurationRecord().apply {
                            set(SLACK_TO_JIRA_CONFIGURATION.SLACK_TO_JIRA_TENANT_ID, tenantId)
                            set(SLACK_TO_JIRA_CONFIGURATION.SLACK_USER_OR_GROUP_ID, userOrGroupId)
                            set(SLACK_TO_JIRA_CONFIGURATION.REACTION, r)
                            set(SLACK_TO_JIRA_CONFIGURATION.CHANNEL_ID, channelId)
                            set(SLACK_TO_JIRA_CONFIGURATION.CONFIGURATION, configuration)
                        }
                    },
                    row(JIRA_TENANT.CLOUD_ID).mapping { cloudId ->
                        JiraTenantRecord().apply {
                            set(JIRA_TENANT.CLOUD_ID, cloudId)
                        }
                    },
                ).from(SLACK_TO_JIRA_CONFIGURATION)
                .join(SLACK_TO_JIRA_TENANT)
                .on(SLACK_TO_JIRA_CONFIGURATION.SLACK_TO_JIRA_TENANT_ID.eq(SLACK_TO_JIRA_TENANT.ID))
                .join(JIRA_TENANT)
                .on(SLACK_TO_JIRA_TENANT.JIRA_TENANT_ID.eq(JIRA_TENANT.ID))
                .where(
                    SLACK_TO_JIRA_CONFIGURATION.SLACK_USER_OR_GROUP_ID.eq(userId.id).or(
                        SLACK_TO_JIRA_CONFIGURATION.SLACK_USER_OR_GROUP_ID.`in`(slackUserGroupIds.map { it.id }),
                    ),
                ).and(SLACK_TO_JIRA_CONFIGURATION.REACTION.eq(reaction))
                .and(SLACK_TO_JIRA_CONFIGURATION.CHANNEL_ID.eq(slackChannelId.id))
                .fetchOne { record ->
                    val slackConfigRecord = record.component1() as SlackToJiraConfigurationRecord
                    val jiraTenantRecord = record.component2() as JiraTenantRecord

                    val baseConfig =
                        objectMapper.readValue(
                            slackConfigRecord.get(SLACK_TO_JIRA_CONFIGURATION.CONFIGURATION)!!.data(),
                            JiraBoardConfiguration::class.java,
                        )
                    val jiraBoardConfiguration =
                        when (slackConfigRecord.get(SLACK_TO_JIRA_CONFIGURATION.SLACK_USER_OR_GROUP_ID)!!.first()) {
                            'U' -> baseConfig as JiraBoardConfiguration.UserConfiguration
                            'W' -> baseConfig as JiraBoardConfiguration.UserConfiguration
                            'S' -> baseConfig as JiraBoardConfiguration.UserGroupConfiguration
                            else -> throw IllegalArgumentException("Invalid SlackUserOrGroupId format")
                        }
                    val jiraCloudId = JiraCloudId(UUID.fromString(jiraTenantRecord.get(JIRA_TENANT.CLOUD_ID)!!))
                    Pair(jiraCloudId, jiraBoardConfiguration)
                }

        if (withChannelId != null) {
            return withChannelId
        }

        return dslContext
            .select(
                row(
                    SLACK_TO_JIRA_CONFIGURATION.SLACK_TO_JIRA_TENANT_ID,
                    SLACK_TO_JIRA_CONFIGURATION.SLACK_USER_OR_GROUP_ID,
                    SLACK_TO_JIRA_CONFIGURATION.REACTION,
                    SLACK_TO_JIRA_CONFIGURATION.CHANNEL_ID,
                    SLACK_TO_JIRA_CONFIGURATION.CONFIGURATION,
                ).mapping { tenantId, userOrGroupId, r, channelId, configuration ->
                    SlackToJiraConfigurationRecord().apply {
                        set(SLACK_TO_JIRA_CONFIGURATION.SLACK_TO_JIRA_TENANT_ID, tenantId)
                        set(SLACK_TO_JIRA_CONFIGURATION.SLACK_USER_OR_GROUP_ID, userOrGroupId)
                        set(SLACK_TO_JIRA_CONFIGURATION.REACTION, r)
                        set(SLACK_TO_JIRA_CONFIGURATION.CHANNEL_ID, channelId)
                        set(SLACK_TO_JIRA_CONFIGURATION.CONFIGURATION, configuration)
                    }
                },
                row(JIRA_TENANT.CLOUD_ID).mapping { cloudId ->
                    JiraTenantRecord().apply {
                        set(JIRA_TENANT.CLOUD_ID, cloudId)
                    }
                },
            ).from(SLACK_TO_JIRA_CONFIGURATION)
            .join(SLACK_TO_JIRA_TENANT)
            .on(SLACK_TO_JIRA_CONFIGURATION.SLACK_TO_JIRA_TENANT_ID.eq(SLACK_TO_JIRA_TENANT.ID))
            .join(JIRA_TENANT)
            .on(SLACK_TO_JIRA_TENANT.JIRA_TENANT_ID.eq(JIRA_TENANT.ID))
            .where(
                SLACK_TO_JIRA_CONFIGURATION.SLACK_USER_OR_GROUP_ID.eq(userId.id).or(
                    SLACK_TO_JIRA_CONFIGURATION.SLACK_USER_OR_GROUP_ID.`in`(slackUserGroupIds.map { it.id }),
                ),
            ).and(SLACK_TO_JIRA_CONFIGURATION.REACTION.eq(reaction))
            .fetchOne { record ->
                val slackConfigRecord = record.component1() as SlackToJiraConfigurationRecord
                val jiraTenantRecord = record.component2() as JiraTenantRecord

                val baseConfig =
                    objectMapper.readValue(
                        slackConfigRecord.get(SLACK_TO_JIRA_CONFIGURATION.CONFIGURATION)!!.data(),
                        JiraBoardConfiguration::class.java,
                    )
                val jiraBoardConfiguration =
                    when (slackConfigRecord.get(SLACK_TO_JIRA_CONFIGURATION.SLACK_USER_OR_GROUP_ID)!!.first()) {
                        'U' -> baseConfig as JiraBoardConfiguration.UserConfiguration
                        'W' -> baseConfig as JiraBoardConfiguration.UserConfiguration
                        'S' -> baseConfig as JiraBoardConfiguration.UserGroupConfiguration
                        else -> throw IllegalArgumentException("Invalid SlackUserOrGroupId format")
                    }
                val jiraCloudId = JiraCloudId(UUID.fromString(jiraTenantRecord.get(JIRA_TENANT.CLOUD_ID)!!))
                Pair(jiraCloudId, jiraBoardConfiguration)
            }
    }

    fun getByOwner(userId: SlackUserId): DisplayJiraBoards? {
        val records =
            dslContext
                .select(
                    SLACK_TO_JIRA_CONFIGURATION.ID,
                    SLACK_TO_JIRA_CONFIGURATION.SLACK_TO_JIRA_TENANT_ID,
                    SLACK_TO_JIRA_CONFIGURATION.SLACK_USER_OR_GROUP_ID,
                    SLACK_TO_JIRA_CONFIGURATION.REACTION,
                    SLACK_TO_JIRA_CONFIGURATION.CHANNEL_ID,
                    SLACK_TO_JIRA_CONFIGURATION.CONFIGURATION,
                    avg(JIRA_TICKET_CSAT.CSAT_VALUE).cast(Float::class.java),
                ).from(SLACK_TO_JIRA_CONFIGURATION)
                .leftJoin(JIRA_TICKET_CSAT)
                .on(JIRA_TICKET_CSAT.SLACK_TO_JIRA_TENANT_ID.eq(SLACK_TO_JIRA_CONFIGURATION.SLACK_TO_JIRA_TENANT_ID))
                .where(SLACK_TO_JIRA_CONFIGURATION.OWNER.eq(userId.id))
                .groupBy(SLACK_TO_JIRA_CONFIGURATION.ID)
                .fetch { record ->
                    val baseConfig =
                        objectMapper.readValue(
                            record.get(SLACK_TO_JIRA_CONFIGURATION.CONFIGURATION)!!.data(),
                            JiraBoardConfiguration::class.java,
                        )
                    val jiraBoardConfiguration =
                        when (record.get(SLACK_TO_JIRA_CONFIGURATION.SLACK_USER_OR_GROUP_ID)!!.first()) {
                            'U' -> baseConfig as JiraBoardConfiguration.UserConfiguration
                            'W' -> baseConfig as JiraBoardConfiguration.UserConfiguration
                            'S' -> baseConfig as JiraBoardConfiguration.UserGroupConfiguration
                            else -> throw IllegalArgumentException("Invalid SlackUserOrGroupId format")
                        }
                    val slackToJiraTenantId = record.get(SLACK_TO_JIRA_CONFIGURATION.SLACK_TO_JIRA_TENANT_ID)!!
                    DisplayJiraBoards(
                        slackToJiraTenantId,
                        listOf(
                            JiraBoardsStatistics(
                                record.get(SLACK_TO_JIRA_CONFIGURATION.ID)!!,
                                jiraBoardConfiguration,
                                record.value7(),
                            ),
                        ),
                    )
                }

        val slackToJiraTenantId = records.firstOrNull()?.slackToJiraTenantId ?: return null
        return DisplayJiraBoards(slackToJiraTenantId, records.flatMap { it.jiraBoardsConfiguration })
    }

    fun delete(jiraBoardConfigurationId: UUID) {
        dslContext
            .deleteFrom(SLACK_TO_JIRA_CONFIGURATION)
            .where(SLACK_TO_JIRA_CONFIGURATION.ID.eq(jiraBoardConfigurationId))
            .execute()
    }
}
