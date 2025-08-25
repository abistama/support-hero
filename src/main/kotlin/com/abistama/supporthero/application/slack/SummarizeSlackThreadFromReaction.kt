package com.abistama.supporthero.application.slack

import com.abistama.supporthero.application.summarizer.Summarizer
import com.abistama.supporthero.domain.slack.SlackChannelId
import com.abistama.supporthero.domain.slack.SlackGetConversations
import com.abistama.supporthero.domain.slack.SlackMessage
import com.abistama.supporthero.domain.slack.SlackPostBlocksMessage
import com.abistama.supporthero.domain.slack.SlackTeamId
import com.abistama.supporthero.domain.slack.events.ReactionAddedEvent
import com.abistama.supporthero.domain.slack.events.SlackTs
import com.slack.api.model.kotlin_extension.block.withBlocks
import mu.KLogging
import org.springframework.data.redis.core.RedisTemplate
import java.time.Duration

class SummarizeSlackThreadFromReaction(
    private val aiSummarizer: Summarizer,
    private val slackAutoRefreshTokenClient: SlackClient,
    private val redisTemplate: RedisTemplate<String, Any>,
) {
    companion object : KLogging()

    fun handle(
        slackTeamId: SlackTeamId,
        event: ReactionAddedEvent,
    ) {
        if (event.reaction == "dart") {
            val read = redisTemplate.opsForValue().getAndSet("slackThread:${event.item.channel}:${event.item.ts}", true)
            logger.info { "Will summarize a message... | read? $read" }
            if (read == null) {
                logger.info { "Getting messages from the Slack API ..." }
                val messages = getMessage(slackTeamId, event.item.channel, event.item.ts)
                val summary = aiSummarizer.summarizeText(messages.joinToString("\n") { "${it.username}: ${it.text}" })
                val blocks =
                    withBlocks {
                        header { text("Summary") }
                        section {
                            markdownText(summary)
                        }
                        divider()
                    }
                val postToThread =
                    if (messages.size > 1) {
                        event.item.ts
                    } else {
                        null
                    }
                redisTemplate
                    .opsForValue()
                    .getAndExpire("slackThread:${event.item.channel}:${event.item.ts}", Duration.ofSeconds(60))
                slackAutoRefreshTokenClient
                    .postEphemeralMessage(
                        SlackPostBlocksMessage(
                            event.user,
                            blocks,
                            slackTeamId,
                            event.item.channel,
                            postToThread,
                        ),
                    ).fold(
                        { error -> logger.error { "Error posting message: $error" } },
                        { logger.info { "Message posted: $it" } },
                    )
            }
        }
    }

    private fun getMessage(
        slackTeamId: SlackTeamId,
        slackChannelId: SlackChannelId,
        ts: SlackTs,
    ): List<SlackMessage> =
        slackAutoRefreshTokenClient.getConversations(SlackGetConversations(ts, slackChannelId, slackTeamId)).fold(
            { error ->
                logger.error { "Error getting message content: $error" }
                emptyList()
            },
            { response -> response.messages },
        )
}
