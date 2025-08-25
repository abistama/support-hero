package com.abistama.supporthero.infrastructure.email

import com.abistama.supporthero.application.slack.EmailService
import feign.Feign
import feign.codec.Decoder
import feign.codec.Encoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EmailConfiguration {
    @Bean
    fun smtp2GoAPIClient(
        smtp2GoProperties: Smtp2GoProperties,
        decoder: Decoder,
        encoder: Encoder,
    ) = Feign
        .builder()
        .encoder(encoder)
        .decoder(decoder)
        .target(Smtp2GoAPIClient::class.java, "https://eu-api.smtp2go.com")

    @Bean
    fun emailService(
        smtp2GoProperties: Smtp2GoProperties,
        smtp2GoAPIClient: Smtp2GoAPIClient,
    ) = EmailService(smtp2GoProperties, smtp2GoAPIClient)
}
