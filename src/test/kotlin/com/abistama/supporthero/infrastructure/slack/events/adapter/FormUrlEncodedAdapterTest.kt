package com.abistama.supporthero.infrastructure.slack.events.adapter

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class FormUrlEncodedAdapterTest :
    StringSpec({
        val formUrlEncodedAdapter = FormUrlEncodedAdapter()

        "should return payload when requestBody is valid" {
            val requestBody = "payload=%7B%22type%22%3A%22view_submission%22%7D"
            val expected = "{\"type\":\"view_submission\"}"

            val result = formUrlEncodedAdapter.from(requestBody)

            result shouldBe expected
        }

        "should return null when payload is not present in requestBody" {
            val requestBody = "notPayload=%7B%22type%22%3A%22view_submission%22%7D"

            val result = formUrlEncodedAdapter.from(requestBody)

            result shouldBe null
        }

        "should return empty string when payload is empty in requestBody" {
            val requestBody = "payload="

            val result = formUrlEncodedAdapter.from(requestBody)

            result shouldBe ""
        }
    })
