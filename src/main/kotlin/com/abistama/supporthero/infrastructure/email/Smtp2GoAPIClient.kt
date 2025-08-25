package com.abistama.supporthero.infrastructure.email

import feign.Headers
import feign.Param
import feign.RequestLine

@Headers("Content-Type: application/json")
interface Smtp2GoAPIClient {
    @RequestLine("POST /v3/email/send")
    @Headers("X-Smtp2go-Api-Key: {access_token}")
    fun sendEmail(
        @Param("access_token") accessToken: String,
        email: Email,
    ): EmailResponse
}
