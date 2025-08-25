package com.abistama.supporthero.infrastructure.email

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "smtp2go")
data class Smtp2GoProperties(
    val apiKey: String,
    val senderEmail: String,
)
