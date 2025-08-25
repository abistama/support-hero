package com.abistama.supporthero.application.slack

import com.abistama.supporthero.infrastructure.email.Email
import com.abistama.supporthero.infrastructure.email.EmailHeader
import com.abistama.supporthero.infrastructure.email.Smtp2GoAPIClient
import com.abistama.supporthero.infrastructure.email.Smtp2GoProperties

class EmailService(
    private val smtp2GoProperties: Smtp2GoProperties,
    private val smtp2GoAPIClient: Smtp2GoAPIClient,
) {
    fun sendEmail(
        subject: String,
        textBody: String,
        replyTo: String,
    ) {
        val email =
            Email(
                sender = smtp2GoProperties.senderEmail,
                to = listOf(smtp2GoProperties.senderEmail),
                subject = subject,
                textBody = textBody,
                customHeaders = listOf(EmailHeader("Reply-To", replyTo)),
            )

        smtp2GoAPIClient.sendEmail(smtp2GoProperties.apiKey, email)
    }
}
