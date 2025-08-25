package com.abistama.supporthero.infrastructure.ai

import com.abistama.supporthero.application.summarizer.Summarizer
import dev.langchain4j.model.anthropic.AnthropicChatModel
import dev.langchain4j.service.AiServices
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AiConfiguration {
    @Bean
    fun aiSummarizer(anthropicChatModel: AnthropicChatModel): Summarizer =
        AiServices
            .builder(Summarizer::class.java)
            .chatLanguageModel(anthropicChatModel)
            .build()
}
