package com.abistama.supporthero.application.slack

import com.abistama.supporthero.domain.slack.SlackGetGroups
import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.SlackUserGroupId
import com.abistama.supporthero.domain.slack.SlackUserId
import com.abistama.supporthero.infrastructure.repository.SlackTenantRepository
import mu.KLogging
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled

open class UserGroupService(
    private val slackTenantRepository: SlackTenantRepository,
    private val slackAutoRefreshTokenClient: SlackClient,
) {
    companion object : KLogging()

    private var cache: MutableMap<SlackUserId, Collection<SlackUserGroupId>> = mutableMapOf()

    @Scheduled(fixedRate = 60000)
    @SchedulerLock(name = "refreshUserGroupCache")
    open fun refreshCache() {
        LockAssert.assertLocked()
        slackTenantRepository.getAllSlackTeamIds().forEach { slackTeamId ->
            slackAutoRefreshTokenClient.getUserGroups(SlackGetGroups(SlackTeamId(slackTeamId.id))).fold(
                { error ->
                    logger.info {
                        "Could not get the user groups for team ${slackTeamId.id} due to ${error.message}. Will use outdated cache"
                    }
                },
                {
                    logger.info { "Got the user groups..." }
                    cache.plus(it)
                },
            )
        }
    }

    open fun getUserGroups(userId: SlackUserId): Collection<SlackUserGroupId> = cache[userId] ?: emptyList()
}
