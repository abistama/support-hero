package com.abistama.supporthero

import com.abistama.supporthero.infrastructure.email.Smtp2GoProperties
import com.abistama.supporthero.infrastructure.jira.oauth.JiraOAuthProperties
import com.abistama.supporthero.infrastructure.slack.oauth.SlackOAuthProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import dev.langchain4j.anthropic.spring.Properties as AnthropicProperties

@SpringBootApplication
@EnableFeignClients
@EnableConfigurationProperties(
    JiraOAuthProperties::class,
    SlackOAuthProperties::class,
    Smtp2GoProperties::class,
    AnthropicProperties::class,
)
class MainApplication

fun main(args: Array<String>) {
    runApplication<MainApplication>(*args)
}
