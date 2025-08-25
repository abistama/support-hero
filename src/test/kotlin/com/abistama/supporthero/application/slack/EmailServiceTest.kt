package com.abistama.supporthero.application.slack

import com.abistama.supporthero.infrastructure.email.Email
import com.abistama.supporthero.infrastructure.email.EmailHeader
import com.abistama.supporthero.infrastructure.email.Smtp2GoAPIClient
import com.abistama.supporthero.infrastructure.email.Smtp2GoProperties
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class EmailServiceTest :
    FunSpec({
        val smtp2GoProperties = mockk<Smtp2GoProperties>()
        val smtp2GoAPIClient = mockk<Smtp2GoAPIClient>(relaxed = true)
        val emailService = EmailService(smtp2GoProperties, smtp2GoAPIClient)

        context("sendEmail") {
            test("should send email with correct parameters") {
                val subject = "Test Subject"
                val textBody = "Test Body"
                val replyTo = "test@example.com"

                every { smtp2GoProperties.senderEmail } returns "sender@example.com"
                every { smtp2GoProperties.apiKey } returns "apiKey"

                emailService.sendEmail(subject, textBody, replyTo)

                val expectedEmail =
                    Email(
                        sender = "sender@example.com",
                        to = listOf("sender@example.com"),
                        subject = subject,
                        textBody = textBody,
                        customHeaders = listOf(EmailHeader("Reply-To", replyTo)),
                    )

                verify { smtp2GoAPIClient.sendEmail("apiKey", expectedEmail) }
            }
        }
    })
