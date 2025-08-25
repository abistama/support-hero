package com.abistama.supporthero.infrastructure.email

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

data class Email(
    val sender: String,
    val to: List<String>,
    val subject: String,
    @JsonProperty("text_body")
    val textBody: String,
    @JsonProperty("custom_headers")
    val customHeaders: List<EmailHeader>,
)

data class EmailHeader(
    val header: String,
    val value: String,
)

data class EmailResponse(
    @JsonProperty("request_id")
    val requestId: String,
    val data: EmailData,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EmailData(
    val succeeded: Int,
    val failed: Int,
    @JsonProperty("email_id")
    val emailId: String,
)
