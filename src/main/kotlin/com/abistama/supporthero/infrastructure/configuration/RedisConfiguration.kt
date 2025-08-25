package com.abistama.supporthero.infrastructure.configuration

import com.abistama.supporthero.domain.slack.events.SlackTs
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.data.redis.support.collections.DefaultRedisSet
import org.springframework.data.redis.support.collections.RedisSet

@Configuration
class RedisConfiguration {
    @Bean
    fun redisTemplate(connectionFactory: LettuceConnectionFactory): RedisTemplate<String, Any> {
        val objectMapper = ObjectMapper()
        objectMapper.registerModule(JavaTimeModule())
        objectMapper.registerKotlinModule()
        objectMapper.activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder().allowIfBaseType(Any::class.java).build(),
            ObjectMapper.DefaultTyping.EVERYTHING,
        )
        val jsonRedisSerializer = GenericJackson2JsonRedisSerializer(objectMapper)
        val stringRedisSerializer: RedisSerializer<String> = StringRedisSerializer()
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = connectionFactory
        template.keySerializer = stringRedisSerializer
        template.valueSerializer = jsonRedisSerializer
        template.hashKeySerializer = stringRedisSerializer
        template.hashValueSerializer = jsonRedisSerializer

        return template
    }

    @Suppress("UNCHECKED_CAST")
    @Bean
    fun viewedEvents(redisTemplate: RedisTemplate<String, Any>): RedisSet<SlackTs> =
        DefaultRedisSet("viewed-events", redisTemplate as RedisTemplate<String, SlackTs>)

    @Suppress("UNCHECKED_CAST")
    @Bean
    fun ticketCreatedMessage(redisTemplate: RedisTemplate<String, Any>): RedisSet<SlackTs> =
        DefaultRedisSet("ticket-created-message", redisTemplate as RedisTemplate<String, SlackTs>)
}
